package com.siaumkm.tax;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import com.siaumkm.identity.BusinessEntityRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import com.siaumkm.tax.PphMasaService.HasilMasa;
import com.siaumkm.transaction.JournalEntry;
import com.siaumkm.transaction.JournalEntryRepository;
import com.siaumkm.transaction.JournalLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BR-B5-01: kalkulasi PPh Final per masa dari pembukuan nyata — WAJIB
 * PostgreSQL asli (rumus DPP PMK 164/2023, log append-only + trigger V8).
 * Isolasi antar test memakai TAHUN berbeda (jurnal tenant-global).
 */
@Testcontainers
@SpringBootTest
class PphMasaServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private PphMasaService service;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private BusinessEntityRepository businessEntityRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void bulanPenembusan500jt_dppHanyaKelebihan() {
        UUID be = entitas(BentukBadanUsaha.OP, null);
        jurnalPendapatanPosted("450000000", LocalDate.of(2040, 1, 15));
        jurnalPendapatanPosted("100000000", LocalDate.of(2040, 2, 10));

        HasilMasa hasil = service.hitungMasa(be, 2040, 2);

        assertThat(hasil.omzetBruto()).isEqualByComparingTo(new BigDecimal("100000000"));
        assertThat(hasil.omzetKumulatifTahunan()).isEqualByComparingTo(new BigDecimal("550000000"));
        assertThat(hasil.omzetKenaPajak()).isEqualByComparingTo(new BigDecimal("50000000"));
        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("250000.00"));

        // DPP tersimpan eksplisit di log (kolom V8)
        BigDecimal dppLog = jdbcTemplate.queryForObject(
                "SELECT omzet_kena_pajak FROM tax_calculation_log WHERE business_entity_id = ? AND periode_bulan = 2",
                BigDecimal.class, be);
        assertThat(dppLog).isEqualByComparingTo(new BigDecimal("50000000"));
    }

    @Test
    void masaTanpaOmzet_logDanKalenderTetapDitulis() {
        UUID be = entitas(BentukBadanUsaha.OP, null);

        HasilMasa hasil = service.hitungMasa(be, 2041, 3);

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(BigDecimal.ZERO);
        Integer logRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tax_calculation_log WHERE business_entity_id = ?", Integer.class, be);
        assertThat(logRows).isEqualTo(1);

        // BR-B5-09: jatuh tempo setor-sendiri = tanggal 15 bulan berikutnya
        assertThat(hasil.jatuhTempoSetor()).isEqualTo(LocalDate.of(2041, 4, 15));
        Integer kalenderRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tax_calendar WHERE business_entity_id = ? AND jatuh_tempo = '2041-04-15'",
                Integer.class, be);
        assertThat(kalenderRows).isEqualTo(1);
    }

    @Test
    void omzetMasaNegatif_karenaKoreksiLintasMasa_dppNol() {
        UUID be = entitas(BentukBadanUsaha.OP, null);
        jurnalPendapatanPosted("80000000", LocalDate.of(2042, 1, 20));
        jurnalKoreksiPendapatanPosted("80000000", LocalDate.of(2042, 2, 5)); // pembalik lintas masa

        HasilMasa hasil = service.hitungMasa(be, 2042, 2);

        assertThat(hasil.omzetBruto()).isEqualByComparingTo(new BigDecimal("-80000000"));
        assertThat(hasil.omzetKenaPajak()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void masaEraPP55_memakaiAturanLama() {
        UUID be = entitas(BentukBadanUsaha.OP, null);
        jurnalPendapatanPosted("30000000", LocalDate.of(2025, 11, 10));

        HasilMasa hasil = service.hitungMasa(be, 2025, 11);

        assertThat(hasil.regulasiAcuan()).contains("PP 55/2022");
    }

    @Test
    void kalkulasiUlang_menambahBarisBaru_barisLamaUtuh() {
        UUID be = entitas(BentukBadanUsaha.OP, null);
        service.hitungMasa(be, 2043, 5);
        service.hitungMasa(be, 2043, 5);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tax_calculation_log WHERE business_entity_id = ? AND periode_tahun = 2043 AND periode_bulan = 5",
                Integer.class, be);
        assertThat(rows).isEqualTo(2); // append-only: riwayat utuh
    }

    @Test
    void logPajak_appendOnly_ditegakkanTriggerDatabase_danTeraudit() {
        UUID be = entitas(BentukBadanUsaha.OP, null);
        service.hitungMasa(be, 2044, 6);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE tax_calculation_log SET pajak_terhitung = 0 WHERE business_entity_id = ?", be))
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM tax_calculation_log WHERE business_entity_id = ?", be))
                .hasStackTraceContaining("append-only");

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE table_name = 'tax_calculation_log'", Integer.class);
        assertThat(auditRows).isGreaterThanOrEqualTo(1);
    }

    @Test
    void koperasiTanpaTanggalTerdaftar_gagalTanpaBarisLog() {
        UUID be = entitas(BentukBadanUsaha.KOPERASI, null);

        assertThatThrownBy(() -> service.hitungMasa(be, 2045, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tanggal terdaftar pajak wajib");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tax_calculation_log WHERE business_entity_id = ?", Integer.class, be);
        assertThat(rows).isZero();
    }

    // ---- helpers ----

    private UUID entitas(BentukBadanUsaha bentuk, LocalDate tanggalTerdaftar) {
        BusinessEntity be = new BusinessEntity();
        be.setNamaUsaha("Usaha PPh Masa " + UUID.randomUUID());
        be.setNamaPemilik("Pemilik Uji");
        be.setNikPemilik("3175011122233355");
        be.setBentukBadan(bentuk);
        be.setTanggalTerdaftarPajak(tanggalTerdaftar);
        return businessEntityRepository.saveAndFlush(be).getId();
    }

    private void jurnalPendapatanPosted(String jumlah, LocalDate tanggal) {
        BigDecimal nilai = new BigDecimal(jumlah);
        JournalEntry je = jurnalBaru(tanggal);
        je.addLine(new JournalLine(akun("1000"), nilai, BigDecimal.ZERO));
        je.addLine(new JournalLine(akun("4000"), BigDecimal.ZERO, nilai));
        journalEntryRepository.saveAndFlush(je);
    }

    private void jurnalKoreksiPendapatanPosted(String jumlah, LocalDate tanggal) {
        BigDecimal nilai = new BigDecimal(jumlah);
        JournalEntry je = jurnalBaru(tanggal);
        je.addLine(new JournalLine(akun("4000"), nilai, BigDecimal.ZERO));
        je.addLine(new JournalLine(akun("1000"), BigDecimal.ZERO, nilai));
        journalEntryRepository.saveAndFlush(je);
    }

    private JournalEntry jurnalBaru(LocalDate tanggal) {
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal("JUM-" + System.nanoTime());
        je.setTanggalTransaksi(tanggal);
        je.setKeterangan("Uji PPh masa");
        je.setCreatedBy(user());
        je.setStatus(JournalEntry.Status.POSTED);
        je.setPostedAt(Instant.now());
        return je;
    }

    private UUID akun(String kode) {
        return chartOfAccountRepository.findByKodeAkun(kode).orElseThrow().getId();
    }

    private UUID user() {
        return appUserRepository.findByUsername("owner-pph-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-pph-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner PPh Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }
}
