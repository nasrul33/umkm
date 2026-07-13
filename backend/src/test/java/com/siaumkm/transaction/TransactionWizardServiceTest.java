package com.siaumkm.transaction;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SRS-B3-01/04: test wizard transaksi WAJIB memakai PostgreSQL asli
 * (Testcontainers) karena memverifikasi juga trigger database
 * (prevent_update_posted_journal, audit hash-chain) — lihat CLAUDE.md.
 */
@Testcontainers
@SpringBootTest
class TransactionWizardServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionWizardService wizardService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    // ---- Template: JUAL_BARANG_JASA ----

    @Test
    void jualTunai_debitKas_kreditPendapatan_danTersimpanKeDatabase() {
        JournalEntry je = wizardService.buatJurnal(
                req("JUAL_BARANG_JASA", "150000", "CASH"), user());

        assertLine(je, akun(AccountResolver.KAS), "150000.00", "0");
        assertLine(je, akun(AccountResolver.PENDAPATAN_USAHA), "0", "150000.00");

        JournalEntry saved = journalEntryRepository.saveAndFlush(je);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(JournalEntry.Status.DRAFT);
        assertThat(saved.getMetodePembayaran()).isEqualTo(MetodePembayaran.CASH);
    }

    @Test
    void jualKredit_receivable_debitPiutangUsaha() {
        JournalEntry je = wizardService.buatJurnal(
                req("JUAL_BARANG_JASA", "250000", "RECEIVABLE"), user());

        assertLine(je, akun(AccountResolver.PIUTANG_USAHA), "250000.00", "0");
        assertLine(je, akun(AccountResolver.PENDAPATAN_USAHA), "0", "250000.00");
    }

    // ---- Template: TERIMA_PEMBAYARAN ----

    @Test
    void terimaPembayaranTransfer_debitBank_kreditPiutang() {
        JournalEntry je = wizardService.buatJurnal(
                req("TERIMA_PEMBAYARAN", "250000", "TRANSFER"), user());

        assertLine(je, akun(AccountResolver.BANK), "250000.00", "0");
        assertLine(je, akun(AccountResolver.PIUTANG_USAHA), "0", "250000.00");
    }

    // ---- Template: BELI_BAHAN ----

    @Test
    void beliBahanKredit_payable_debitPersediaan_kreditHutang() {
        JournalEntry je = wizardService.buatJurnal(
                req("BELI_BAHAN", "500000", "PAYABLE"), user());

        assertLine(je, akun(AccountResolver.PERSEDIAAN), "500000.00", "0");
        assertLine(je, akun(AccountResolver.HUTANG_USAHA), "0", "500000.00");
    }

    @Test
    void beliBahanTunai_kreditKas() {
        JournalEntry je = wizardService.buatJurnal(
                req("BELI_BAHAN", "75000", "CASH"), user());

        assertLine(je, akun(AccountResolver.PERSEDIAAN), "75000.00", "0");
        assertLine(je, akun(AccountResolver.KAS), "0", "75000.00");
    }

    // ---- Template: BAYAR_BIAYA ----

    @Test
    void bayarBiayaQris_debitBiayaOperasional_kreditBank() {
        JournalEntry je = wizardService.buatJurnal(
                req("BAYAR_BIAYA", "120000.505", "QRIS"), user());

        // pembulatan HALF_UP eksplisit (Aturan Emas #1): 120000.505 -> 120000.51
        assertLine(je, akun(AccountResolver.BIAYA_OPERASIONAL), "120000.51", "0");
        assertLine(je, akun(AccountResolver.BANK), "0", "120000.51");
    }

    // ---- Template: SETOR_KAS_PEMILIK / TARIK_KAS_PEMILIK ----

    @Test
    void setorKasPemilik_debitKas_kreditModal() {
        JournalEntry je = wizardService.buatJurnal(
                req("SETOR_KAS_PEMILIK", "1000000", "CASH"), user());

        assertLine(je, akun(AccountResolver.KAS), "1000000.00", "0");
        assertLine(je, akun(AccountResolver.MODAL_PEMILIK), "0", "1000000.00");
    }

    @Test
    void tarikKasPemilik_debitPrive_kreditBank() {
        JournalEntry je = wizardService.buatJurnal(
                req("TARIK_KAS_PEMILIK", "300000", "TRANSFER"), user());

        // Prive (3100) terpisah dari Modal (3000) — lihat BR-B4-04
        assertLine(je, akun(AccountResolver.PRIVE), "300000.00", "0");
        assertLine(je, akun(AccountResolver.BANK), "0", "300000.00");
    }

    // ---- Kasus tepi ----

    @Test
    void templateTidakDikenal_ditolak() {
        assertThatThrownBy(() -> wizardService.buatJurnal(
                req("TEMPLATE_NGACO", "100000", "CASH"), user()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("TEMPLATE_NGACO");
    }

    @Test
    void jumlahNolAtauNegatif_ditolak() {
        assertThatThrownBy(() -> wizardService.buatJurnal(
                req("JUAL_BARANG_JASA", "0", "CASH"), user()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> wizardService.buatJurnal(
                req("BAYAR_BIAYA", "-5000", "CASH"), user()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- SRS-B3-04: jurnal POSTED immutable, ditegakkan trigger database ----

    @Test
    void jurnalPosted_tidakBisaDiubah_ditolakTriggerDatabase() {
        JournalEntry je = journalEntryRepository.saveAndFlush(
                wizardService.buatJurnal(req("JUAL_BARANG_JASA", "100000", "CASH"), user()));

        // DRAFT -> POSTED masih diizinkan trigger (OLD.status belum POSTED)
        je.setStatus(JournalEntry.Status.POSTED);
        JournalEntry posted = journalEntryRepository.saveAndFlush(je);

        posted.setKeterangan("coba ubah setelah POSTED");
        assertThatThrownBy(() -> journalEntryRepository.saveAndFlush(posted))
                .hasStackTraceContaining("POSTED tidak dapat diubah");
    }

    // ---- helpers ----

    private TransactionRequest req(String template, String jumlah, String metode) {
        return new TransactionRequest(template, new BigDecimal(jumlah), metode, LocalDate.of(2026, 7, 1));
    }

    /**
     * journal_entry.created_by ber-FK ke app_user (V2) — createdBy harus
     * user sungguhan, bukan UUID acak.
     */
    private UUID user() {
        return appUserRepository.findByUsername("owner-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }

    private UUID akun(String kodeAkun) {
        return chartOfAccountRepository.findByKodeAkun(kodeAkun).orElseThrow().getId();
    }

    private void assertLine(JournalEntry je, UUID akunId, String debit, String kredit) {
        JournalLine line = je.getLines().stream()
                .filter(l -> l.getChartOfAccountId().equals(akunId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tidak ada baris jurnal untuk akun " + akunId));
        assertThat(line.getDebit()).isEqualByComparingTo(new BigDecimal(debit));
        assertThat(line.getKredit()).isEqualByComparingTo(new BigDecimal(kredit));
    }
}
