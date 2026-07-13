package com.siaumkm.report;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import com.siaumkm.transaction.JournalEntry;
import com.siaumkm.transaction.JournalEntryRepository;
import com.siaumkm.transaction.JournalLine;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** BR-B4-06: hasil ekspor harus berkas valid berisi angka laporan nyata. */
@Testcontainers
@SpringBootTest
class ReportExportServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ReportExportService exportService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void exportExcel_workbookValidDenganEmpatSheet_danAngkaBenar() throws Exception {
        jualTunaiPosted("125000", LocalDate.of(2070, 3, 10));
        refreshViews();

        byte[] hasil = exportService.exportExcel(2070);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(hasil))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(4);
            assertThat(wb.getSheet("Neraca")).isNotNull();
            assertThat(wb.getSheet("Laba Rugi")).isNotNull();
            assertThat(wb.getSheet("Arus Kas 2070")).isNotNull();
            assertThat(wb.getSheet("Perubahan Modal 2070")).isNotNull();

            // Baris OPERASI di sheet arus kas memuat 125000 (kolom arus bersih)
            var arusKas = wb.getSheet("Arus Kas 2070");
            boolean adaOperasi125k = false;
            for (var row : arusKas) {
                var c0 = row.getCell(0);
                if (c0 != null && "OPERASI".equals(c0.getStringCellValue())) {
                    adaOperasi125k = row.getCell(3).getNumericCellValue() == 125000.0;
                }
            }
            assertThat(adaOperasi125k).isTrue();
        }
    }

    @Test
    void exportPdf_berkasPdfValid() {
        refreshViews();
        byte[] hasil = exportService.exportPdf(2070);

        assertThat(hasil.length).isGreaterThan(1000);
        assertThat(new String(hasil, 0, 5)).isEqualTo("%PDF-");
    }

    // ---- helpers ----

    private void jualTunaiPosted(String jumlah, LocalDate tanggal) {
        BigDecimal nilai = new BigDecimal(jumlah);
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal("JUE-" + System.nanoTime());
        je.setTanggalTransaksi(tanggal);
        je.setKeterangan("Uji ekspor");
        je.setCreatedBy(user());
        je.setStatus(JournalEntry.Status.POSTED);
        je.setPostedAt(Instant.now());
        je.addLine(new JournalLine(akun("1000"), nilai, BigDecimal.ZERO));
        je.addLine(new JournalLine(akun("4000"), BigDecimal.ZERO, nilai));
        journalEntryRepository.saveAndFlush(je);
    }

    private void refreshViews() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_balance_sheet");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_income_statement");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_cash_flow");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_equity_change");
    }

    private UUID akun(String kode) {
        return chartOfAccountRepository.findByKodeAkun(kode).orElseThrow().getId();
    }

    private UUID user() {
        return appUserRepository.findByUsername("owner-export-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-export-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner Export Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }
}
