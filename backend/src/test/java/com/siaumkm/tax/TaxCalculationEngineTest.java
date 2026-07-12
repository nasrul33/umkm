package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SRS Bagian 2.1: test kalkulasi pajak WAJIB memakai PostgreSQL asli (Testcontainers),
 * jangan mock repository untuk logic uang/pajak — lihat CLAUDE.md.
 *
 * Test ini memverifikasi skenario nyata pasca PP 20/2026:
 * WP OP tanpa batas waktu, dengan pengecualian omzet s.d. Rp500jt/tahun.
 */
@Testcontainers
@SpringBootTest
class TaxCalculationEngineTest {

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
    private TaxCalculationEngine engine;

    @Autowired
    private TaxRuleRepository taxRuleRepository;

    @Test
    void wpOpDenganOmzetDiBawahAmbangRp500jt_tidakKenaPajak() {
        seedTaxRuleOpTanpaBatasWaktu();

        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.OP,
                new BigDecimal("30000000"),   // omzet bulan ini
                new BigDecimal("400000000"),  // omzet kumulatif tahun ini, masih di bawah Rp500jt
                LocalDate.of(2026, 7, 1));

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void wpOpDenganOmzetDiAtasAmbang_kenaPajak0_5Persen() {
        seedTaxRuleOpTanpaBatasWaktu();

        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.OP,
                new BigDecimal("50000000"),
                new BigDecimal("600000000"), // sudah lewat Rp500jt kumulatif
                LocalDate.of(2026, 7, 1));

        // 0.5% dari 50.000.000 = 250.000
        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(hasil.regulasiAcuan()).isEqualTo("PP 20/2026");
    }

    @Test
    void tanggalTransaksiSebelumPP20_2026_tidakBolehMemakaiAturanBaru() {
        seedTaxRuleOpTanpaBatasWaktu(); // berlaku_dari = 2026-04-22

        assertThatCalculationFailsForDateBefore(LocalDate.of(2026, 1, 1));
    }

    private void assertThatCalculationFailsForDateBefore(LocalDate tanggal) {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
            engine.hitungPphFinal(BentukBadanUsaha.OP, new BigDecimal("50000000"),
                    new BigDecimal("600000000"), tanggal));
    }

    private void seedTaxRuleOpTanpaBatasWaktu() {
        // Dalam proyek nyata: dilakukan via Flyway seed (lihat schema.sql),
        // di sini disederhanakan untuk ilustrasi test — ganti dengan
        // taxRuleRepository.save(...) atau @Sql script sesuai kebutuhan implementasi.
    }
}
