package com.siaumkm.transaction;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import com.siaumkm.masterdata.Product;
import com.siaumkm.masterdata.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BR-B3-07: posting & jurnal pembalik. Wajib PostgreSQL asli (Testcontainers)
 * karena memverifikasi trigger immutability (entry + line) dan partial unique
 * index anti pembalikan ganda — semuanya penegakan level database.
 */
@Testcontainers
@SpringBootTest
class JournalPostingServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JournalPostingService postingService;
    @Autowired private TransactionWizardService wizardService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ---- Posting ----

    @Test
    void postJurnalDraft_menjadiPostedDenganPostedAt() {
        JournalEntry draft = simpanJurnalBaru();
        assertThat(draft.getPostedAt()).isNull();

        JournalEntry posted = postingService.post(draft.getId());

        assertThat(posted.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
        assertThat(posted.getPostedAt()).isNotNull();
    }

    @Test
    void postJurnalYangSudahPosted_ditolak() {
        JournalEntry posted = postingService.post(simpanJurnalBaru().getId());

        assertThatThrownBy(() -> postingService.post(posted.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah POSTED");
    }

    @Test
    void postJurnalTidakDitemukan_melemparNoSuchElement() {
        assertThatThrownBy(() -> postingService.post(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- Jurnal pembalik ----

    @Test
    void reverseJurnalPosted_membuatPembalikPosted_barisTertukar_produkIkut() {
        UUID productId = produkUji();
        JournalEntry sumber = simpanJurnalDenganProduk(productId, "125000.00");
        postingService.post(sumber.getId());

        JournalEntry pembalik = postingService.balikkan(sumber.getId(), user());

        assertThat(pembalik.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
        assertThat(pembalik.getReversalOfId()).isEqualTo(sumber.getId());
        assertThat(pembalik.getNomorJurnal()).startsWith("JR-");
        assertThat(pembalik.getKeterangan()).contains(sumber.getNomorJurnal());
        // tanggal pembalik = hari ini (koreksi), bukan tanggal sumber
        assertThat(pembalik.getTanggalTransaksi()).isEqualTo(LocalDate.now());

        // baris kas: debit 125000 di sumber -> kredit 125000 di pembalik
        JournalLine kasPembalik = barisUntukAkun(pembalik, akun(AccountResolver.KAS));
        assertThat(kasPembalik.getDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(kasPembalik.getKredit()).isEqualByComparingTo(new BigDecimal("125000.00"));
        // product_id WAJIB ikut agar margin per produk (B6) terkoreksi
        JournalLine pendapatanPembalik = barisUntukAkun(pembalik, akun(AccountResolver.PENDAPATAN_USAHA));
        assertThat(pendapatanPembalik.getProductId()).isEqualTo(productId);
    }

    @Test
    void reverseJurnalDraft_ditolak() {
        JournalEntry draft = simpanJurnalBaru();

        assertThatThrownBy(() -> postingService.balikkan(draft.getId(), user()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");
    }

    @Test
    void reverseDuaKali_ditolakService() {
        JournalEntry sumber = postingService.post(simpanJurnalBaru().getId());
        postingService.balikkan(sumber.getId(), user());

        assertThatThrownBy(() -> postingService.balikkan(sumber.getId(), user()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah pernah dibalik");
    }

    @Test
    void reverseAtasJurnalPembalik_ditolak() {
        JournalEntry sumber = postingService.post(simpanJurnalBaru().getId());
        JournalEntry pembalik = postingService.balikkan(sumber.getId(), user());

        assertThatThrownBy(() -> postingService.balikkan(pembalik.getId(), user()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catat ulang");
    }

    @Test
    void pembalikGanda_lewatJalurApapun_ditolakUniqueIndexDatabase() {
        JournalEntry sumber = postingService.post(simpanJurnalBaru().getId());
        postingService.balikkan(sumber.getId(), user());

        // Bypass cek service: insert pembalik kedua langsung via repository —
        // partial unique index uq_journal_entry_reversal_of harus menolak (race-safe).
        JournalEntry bypass = new JournalEntry();
        bypass.setNomorJurnal(NomorJurnal.generate("JR"));
        bypass.setTanggalTransaksi(LocalDate.now());
        bypass.setKeterangan("Pembalik liar");
        bypass.setCreatedBy(user());
        bypass.setReversalOfId(sumber.getId());
        bypass.setStatus(JournalEntry.Status.POSTED);

        assertThatThrownBy(() -> journalEntryRepository.saveAndFlush(bypass))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- Celah BR-B3-07 yang ditutup V3: baris jurnal POSTED ikut immutable ----

    @Test
    void updateBarisJurnalPosted_ditolakTriggerDatabase() {
        JournalEntry posted = postingService.post(simpanJurnalBaru().getId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE journal_line SET debit = debit + 1 WHERE journal_entry_id = ?",
                posted.getId()))
                .hasStackTraceContaining("POSTED tidak dapat diubah");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM journal_line WHERE journal_entry_id = ?",
                posted.getId()))
                .hasStackTraceContaining("POSTED tidak dapat diubah");
    }

    // ---- helpers ----

    private JournalEntry simpanJurnalBaru() {
        return journalEntryRepository.saveAndFlush(wizardService.buatJurnal(
                new TransactionRequest("JUAL_BARANG_JASA", new BigDecimal("100000"),
                        "CASH", LocalDate.of(2026, 7, 1)), user()));
    }

    private JournalEntry simpanJurnalDenganProduk(UUID productId, String jumlah) {
        BigDecimal nilai = new BigDecimal(jumlah);
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal(NomorJurnal.generate("JU"));
        je.setTanggalTransaksi(LocalDate.of(2026, 7, 1));
        je.setKeterangan("Penjualan produk uji");
        je.setCreatedBy(user());
        je.addLine(new JournalLine(akun(AccountResolver.KAS), nilai, BigDecimal.ZERO));
        JournalLine pendapatan = new JournalLine(akun(AccountResolver.PENDAPATAN_USAHA), BigDecimal.ZERO, nilai);
        pendapatan.setProductId(productId);
        je.addLine(pendapatan);
        return journalEntryRepository.saveAndFlush(je);
    }

    private UUID produkUji() {
        Product p = new Product();
        p.setNama("Produk Uji Reversal " + UUID.randomUUID());
        p.setHargaJual(new BigDecimal("125000.00"));
        p.setHppDasar(new BigDecimal("80000.00"));
        return productRepository.save(p).getId();
    }

    private UUID akun(String kodeAkun) {
        return chartOfAccountRepository.findByKodeAkun(kodeAkun).orElseThrow().getId();
    }

    private JournalLine barisUntukAkun(JournalEntry je, UUID akunId) {
        return je.getLines().stream()
                .filter(l -> l.getChartOfAccountId().equals(akunId))
                .findFirst().orElseThrow();
    }

    private UUID user() {
        return appUserRepository.findByUsername("owner-posting-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-posting-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner Posting Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }
}
