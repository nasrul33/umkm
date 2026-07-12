package com.siaumkm.report;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.transaction.JournalEntry;
import com.siaumkm.transaction.JournalEntryRepository;
import com.siaumkm.transaction.JournalPostingService;
import com.siaumkm.transaction.TransactionRequest;
import com.siaumkm.transaction.TransactionWizardService;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SRS-B4-08: regresi bug timezone "Laba Bulan Ini" — periode view harus DATE
 * murni (V4) sehingga hasil sama apa pun timezone sesi penulis/pembaca.
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
    @Autowired private TransactionWizardService wizardService;
    @Autowired private JournalPostingService postingService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardSummary_menghitungKasDanLabaBulanBerjalan() {
        JournalEntry je = journalEntryRepository.saveAndFlush(wizardService.buatJurnal(
                new TransactionRequest("JUAL_BARANG_JASA", new BigDecimal("150000"),
                        "CASH", LocalDate.now()), user()));
        postingService.post(je.getId());

        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_balance_sheet");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW vw_income_statement");

        Map<String, Object> summary = reportService.getDashboardSummary();

        assertThat((BigDecimal) summary.get("saldoKas"))
                .isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat((BigDecimal) summary.get("labaBulanIni"))
                .isEqualByComparingTo(new BigDecimal("150000.00"));
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
