package com.siaumkm.report;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import com.siaumkm.report.FinancialReportService.CashFlowCategoryRow;
import com.siaumkm.report.FinancialReportService.CashFlowReport;
import com.siaumkm.report.FinancialReportService.EquityChangeReport;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SRS-B4: laporan dari materialized view atas jurnal POSTED nyata (PG asli).
 * Isolasi antar test memakai TAHUN berbeda (jurnal tenant-global).
 */
@Testcontainers
@SpringBootTest
class FinancialReportServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private FinancialReportService reportService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardSummary_labaAdalahPendapatanDikurangiBeban() {
        // Regresi bug tanda: rumus lama justru MENAMBAHKAN beban ke laba.
        LocalDate hariIni = LocalDate.now();
        jurnalPosted(hariIni, "1000", "150000", "4000", "150000"); // jual tunai
        jurnalPosted(hariIni, "5100", "40000", "1000", "40000");   // bayar biaya

        refreshViews();
        Map<String, Object> summary = reportService.getDashboardSummary();

        // Hanya bulan berjalan yang terisolasi antar test (test lain memakai
        // tahun 2060+); saldo kas bersifat all-time sehingga tidak di-assert absolut.
        assertThat((BigDecimal) summary.get("labaBulanIni"))
                .isEqualByComparingTo(new BigDecimal("110000.00")); // 150k - 40k
    }

    @Test
    void arusKasMetodeLangsung_klasifikasiPerAkunLawan() {
        // Tahun terisolasi 2060
        jurnalPosted(LocalDate.of(2060, 1, 10), "1000", "100000", "4000", "100000"); // jual tunai -> OPERASI +100k
        jurnalPosted(LocalDate.of(2060, 2, 5), "5100", "30000", "1000", "30000");    // biaya -> OPERASI -30k
        jurnalPosted(LocalDate.of(2060, 3, 1), "1000", "50000", "3000", "50000");    // setor modal -> PENDANAAN +50k
        jurnalPosted(LocalDate.of(2060, 4, 1), "1200", "999000", "4000", "999000");  // jual KREDIT -> tidak ada kas, TIDAK muncul
        jurnalPosted(LocalDate.of(2060, 5, 1), "1100", "20000", "1000", "20000");    // transfer kas->bank -> ter-eksklusi

        refreshViews();
        CashFlowReport laporan = reportService.getCashFlow(2060);

        assertThat(kategori(laporan, "OPERASI").arusBersih())
                .isEqualByComparingTo(new BigDecimal("70000")); // 100k - 30k, penjualan kredit TIDAK ikut
        assertThat(kategori(laporan, "PENDANAAN").arusBersih())
                .isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(laporan.kategori()).hasSize(2); // tidak ada INVESTASI; transfer kas tidak menciptakan baris
        assertThat(laporan.saldoKasAkhir().subtract(laporan.saldoKasAwal()))
                .isEqualByComparingTo(new BigDecimal("120000"));
    }

    @Test
    void reversalSetoranModal_tidakSalahTampilSebagaiPrive() {
        // Justifikasi sub-akun 3100: pembalik setoran mendebit 3000 (net thd
        // setoran), BUKAN menaikkan prive.
        jurnalPosted(LocalDate.of(2061, 2, 1), "1000", "50000", "3000", "50000");  // setor
        jurnalPosted(LocalDate.of(2061, 2, 15), "3000", "50000", "1000", "50000"); // pembalik setoran
        jurnalPosted(LocalDate.of(2061, 3, 1), "3100", "25000", "1000", "25000");  // prive sungguhan

        refreshViews();
        EquityChangeReport laporan = reportService.getEquityChange(2061);

        assertThat(laporan.setoran()).isEqualByComparingTo(BigDecimal.ZERO);   // 50k - 50k
        assertThat(laporan.prive()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(laporan.modalAkhir())
                .isEqualByComparingTo(laporan.modalAwal().subtract(new BigDecimal("25000")));
    }

    @Test
    void neraca_balanceDenganBarisSaldoLaba() {
        jurnalPosted(LocalDate.of(2062, 6, 1), "1000", "80000", "4000", "80000");

        refreshViews();
        List<AccountBalanceRow> neraca = reportService.getBalanceSheet();

        assertThat(neraca).anyMatch(r -> r.kodeAkun().equals("3900"));
        // Konvensi seragam debit-kredit: neraca balance <=> SUM(semua baris) == 0
        BigDecimal total = neraca.stream().map(AccountBalanceRow::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void tahunTanpaTransaksi_laporanNolBukanError() {
        refreshViews();
        CashFlowReport arusKas = reportService.getCashFlow(2099);
        assertThat(arusKas.kategori()).isEmpty();
        assertThat(arusKas.saldoKasAkhir()).isEqualByComparingTo(arusKas.saldoKasAwal());
    }

    @Test
    void akunNonKasTanpaKategoriArusKas_ditolakCheckConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO chart_of_account (kode_akun, nama_akun, tipe) VALUES ('5999', 'Beban Uji', 'BEBAN')"))
                .hasStackTraceContaining("chk_cash_flow_category");
    }

    // ---- helpers ----

    private CashFlowCategoryRow kategori(CashFlowReport laporan, String nama) {
        return laporan.kategori().stream().filter(k -> k.kategori().equals(nama))
                .findFirst().orElseThrow(() -> new AssertionError("kategori " + nama + " tidak ada"));
    }

    private void refreshViews() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_balance_sheet");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_income_statement");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_cash_flow");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_equity_change");
    }

    private void jurnalPosted(LocalDate tanggal, String kodeDebit, String debit,
                              String kodeKredit, String kredit) {
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal("JUR-" + System.nanoTime());
        je.setTanggalTransaksi(tanggal);
        je.setKeterangan("Uji laporan B4");
        je.setCreatedBy(user());
        je.setStatus(JournalEntry.Status.POSTED);
        je.setPostedAt(Instant.now());
        je.addLine(new JournalLine(akun(kodeDebit), new BigDecimal(debit), BigDecimal.ZERO));
        je.addLine(new JournalLine(akun(kodeKredit), BigDecimal.ZERO, new BigDecimal(kredit)));
        journalEntryRepository.saveAndFlush(je);
    }

    private UUID akun(String kode) {
        return chartOfAccountRepository.findByKodeAkun(kode).orElseThrow().getId();
    }

    private UUID user() {
        return appUserRepository.findByUsername("owner-report-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-report-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner Report Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }
}
