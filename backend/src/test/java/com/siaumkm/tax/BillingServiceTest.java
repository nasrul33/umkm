package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import com.siaumkm.identity.BusinessEntityRepository;
import com.siaumkm.tax.PjapClient.BillingOrder;
import com.siaumkm.tax.PjapClient.BillingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BR-B5-07 / SRS-B5-05: alur status billing + idempotensi database.
 * PjapClient dipalsukan (perilaku diprogram per test); klasifikasi HTTP
 * diuji terpisah di HttpPjapClientTest.
 */
@Testcontainers
@SpringBootTest
class BillingServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    static final AtomicReference<Function<BillingOrder, BillingResult>> PERILAKU = new AtomicReference<>();
    static final AtomicInteger PANGGILAN = new AtomicInteger();

    @TestConfiguration
    static class FakePjapConfig {
        @Bean
        PjapClient pjapClient() {
            return order -> {
                PANGGILAN.incrementAndGet();
                return PERILAKU.get().apply(order);
            };
        }
    }

    @Autowired private BillingService billingService;
    @Autowired private TaxCalculationLogRepository logRepository;
    @Autowired private BusinessEntityRepository businessEntityRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String NPWP = "1234567890123456";

    @BeforeEach
    void reset() {
        PANGGILAN.set(0);
        PERILAKU.set(order -> new BillingResult("012345678901234", null, "prov-1",
                "{\"kodeBilling\":\"012345678901234\",\"npwp\":\"" + order.npwp() + "\"}"));
    }

    @Test
    void sukses_menjadiIssued_payloadTersimpanMasked() {
        UUID logId = logKalkulasi(1, "250000.00");

        BillingRequest hasil = billingService.buatKodeBilling(logId);

        assertThat(hasil.getStatus()).isEqualTo(BillingRequest.Status.ISSUED);
        assertThat(hasil.getKodeBilling()).isEqualTo("012345678901234");
        // NFR-05: NPWP tidak pernah utuh di payload tersimpan — termasuk
        // response_payload mentah dari provider.
        assertThat(hasil.getRequestPayload()).doesNotContain(NPWP).contains("1234********3456");
        assertThat(hasil.getResponsePayload()).doesNotContain(NPWP);
    }

    @Test
    void transientHabisRetry_pendingManual_barisTetapAda() {
        PERILAKU.set(order -> { throw new PjapClient.PjapTransientException("PJAP down"); });
        UUID logId = logKalkulasi(2, "250000.00");

        BillingRequest hasil = billingService.buatKodeBilling(logId);

        assertThat(hasil.getStatus()).isEqualTo(BillingRequest.Status.PENDING_MANUAL);
        assertThat(hasil.getResponsePayload()).contains("PJAP down");
    }

    @Test
    void kegagalanPermanen_failed() {
        PERILAKU.set(order -> { throw new PjapClient.PjapPermanentException("data ditolak"); });
        UUID logId = logKalkulasi(3, "250000.00");

        assertThat(billingService.buatKodeBilling(logId).getStatus())
                .isEqualTo(BillingRequest.Status.FAILED);
    }

    @Test
    void responsAmbigu_pendingManual() {
        PERILAKU.set(order -> { throw new PjapClient.PjapAmbiguousResponseException("body tidak valid"); });
        UUID logId = logKalkulasi(4, "250000.00");

        assertThat(billingService.buatKodeBilling(logId).getStatus())
                .isEqualTo(BillingRequest.Status.PENDING_MANUAL);
    }

    @Test
    void panggilanKedua_untukLogSama_mengembalikanRequestYangAda() {
        UUID logId = logKalkulasi(5, "250000.00");
        BillingRequest pertama = billingService.buatKodeBilling(logId);
        BillingRequest kedua = billingService.buatKodeBilling(logId);

        assertThat(kedua.getId()).isEqualTo(pertama.getId());
        assertThat(PANGGILAN.get()).isEqualTo(1); // HTTP tidak dipanggil dua kali
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_request WHERE tax_calculation_log_id = ?",
                Integer.class, logId);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void setelahFailed_bolehDicobaUlangSebagaiBarisBaru() {
        PERILAKU.set(order -> { throw new PjapClient.PjapPermanentException("data salah"); });
        UUID logId = logKalkulasi(6, "250000.00");
        BillingRequest gagal = billingService.buatKodeBilling(logId);

        PERILAKU.set(order -> new BillingResult("543210987654321", null, null, "{}"));
        BillingRequest sukses = billingService.buatKodeBilling(logId);

        assertThat(gagal.getStatus()).isEqualTo(BillingRequest.Status.FAILED);
        assertThat(sukses.getStatus()).isEqualTo(BillingRequest.Status.ISSUED);
        assertThat(sukses.getId()).isNotEqualTo(gagal.getId());
    }

    @Test
    void pajakBerdesimal_failFast_tanpaPanggilanHttp() {
        UUID logId = logKalkulasi(7, "250000.50");

        assertThatThrownBy(() -> billingService.buatKodeBilling(logId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bukan rupiah utuh");
        assertThat(PANGGILAN.get()).isZero();
    }

    @Test
    void pajakNol_ditolak_kodeBillingTidakDiperlukan() {
        UUID logId = logKalkulasi(8, "0.00");

        assertThatThrownBy(() -> billingService.buatKodeBilling(logId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tidak diperlukan");
    }

    @Test
    void catatManual_dariPendingManual_keIssued_danIssuedTidakBolehDitimpa() {
        PERILAKU.set(order -> { throw new PjapClient.PjapTransientException("down"); });
        UUID logId = logKalkulasi(9, "250000.00");
        BillingRequest pending = billingService.buatKodeBilling(logId);

        BillingRequest manual = billingService.catatManual(pending.getId(), "111122223333444");
        assertThat(manual.getStatus()).isEqualTo(BillingRequest.Status.ISSUED);
        assertThat(manual.getKodeBilling()).isEqualTo("111122223333444");

        assertThatThrownBy(() -> billingService.catatManual(pending.getId(), "999988887777666"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> billingService.catatManual(pending.getId(), "bukan-angka"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transisiStatus_terekamAuditHashChain() {
        UUID logId = logKalkulasi(10, "250000.00");
        BillingRequest hasil = billingService.buatKodeBilling(logId);

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE table_name = 'billing_request' AND row_id = ?",
                Integer.class, hasil.getId());
        assertThat(auditRows).isGreaterThanOrEqualTo(2); // INSERT REQUESTED + UPDATE ISSUED
    }

    // ---- helpers ----

    private UUID logKalkulasi(int bulan, String pajak) {
        UUID ruleId = jdbcTemplate.queryForObject(
                "SELECT id FROM tax_rule WHERE kode_aturan = 'PPH-FINAL-UMKM-OP' AND berlaku_sampai IS NULL",
                UUID.class);
        return logRepository.saveAndFlush(new TaxCalculationLog(
                entitas(), bulan, 2050, new BigDecimal("50000000"),
                new BigDecimal("50000000"), ruleId, new BigDecimal(pajak))).getId();
    }

    private UUID entitas() {
        BusinessEntity be = new BusinessEntity();
        be.setNamaUsaha("Usaha Billing " + UUID.randomUUID());
        be.setNamaPemilik("Pemilik Uji");
        be.setNpwp(NPWP);
        be.setNikPemilik("3175011122233366");
        be.setBentukBadan(BentukBadanUsaha.OP);
        return businessEntityRepository.saveAndFlush(be).getId();
    }
}
