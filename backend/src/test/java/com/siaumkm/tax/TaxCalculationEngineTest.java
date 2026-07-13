package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SRS Bagian 2.1: test kalkulasi pajak WAJIB memakai PostgreSQL asli (Testcontainers),
 * jangan mock repository untuk logic uang/pajak — lihat CLAUDE.md.
 *
 * Seluruh parameter (tarif, ambang, batas waktu) berasal dari seed Flyway
 * tax_rule (V1 + V7) — tidak ada yang di-hardcode di test selain nilai harapan.
 * Rezim yang tercakup: PP 55/2022 jo. UU HPP (2022-12-20 s.d. 2026-04-21) dan
 * PP 20/2026 (sejak 2026-04-22, koperasi ber-batas 4 tahun pajak).
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

    // ---- Rezim PP 20/2026 (sejak 2026-04-22) ----

    @Test
    void wpOpDenganOmzetDiBawahAmbangRp500jt_tidakKenaPajak() {
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.OP,
                new BigDecimal("30000000"),   // omzet bulan ini
                new BigDecimal("400000000"),  // omzet kumulatif tahun ini, masih di bawah Rp500jt
                LocalDate.of(2026, 7, 1));

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void wpOpDenganOmzetDiAtasAmbang_kenaPajak0_5Persen() {
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.OP,
                new BigDecimal("50000000"),
                new BigDecimal("600000000"), // sudah lewat Rp500jt kumulatif
                LocalDate.of(2026, 7, 1));

        // 0.5% dari 50.000.000 = 250.000
        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(hasil.regulasiAcuan()).isEqualTo("PP 20/2026");
    }

    // ---- Rezim PP 55/2022 (2022-12-20 s.d. 2026-04-21) — backfill V7 ----

    @Test
    void transaksiEraPP55_memakaiAturanLamaBukanAturanBaru() {
        // Sebelum V7, tanggal ini melempar "tidak ada tax_rule aktif".
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.OP,
                new BigDecimal("50000000"),
                new BigDecimal("600000000"),
                LocalDate.of(2026, 1, 15)); // sebelum 2026-04-22

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(hasil.regulasiAcuan()).contains("PP 55/2022");
    }

    @Test
    void sebelumRezimPP55_tidakAdaAturan_ditolak() {
        // Rezim PP 23/2018 sengaja tidak dimodelkan (lihat changelog V7-a).
        assertThatThrownBy(() -> engine.hitungPphFinal(
                BentukBadanUsaha.OP, new BigDecimal("50000000"),
                new BigDecimal("600000000"), LocalDate.of(2022, 6, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tidak ada tax_rule aktif");
    }

    // ---- Batas waktu koperasi (PP 20/2026, data-driven via tax_rule V7) ----

    @Test
    void koperasiBaru_dalamEmpatTahunPajak_kenaPajak() {
        // Terdaftar 2026 -> tahun pajak terakhir 2029 (inklusif tahun terdaftar)
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2029, 6, 1), LocalDate.of(2026, 5, 1));

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(hasil.regulasiAcuan()).isEqualTo("PP 20/2026");
    }

    @Test
    void koperasiBaru_tahunKelima_ditolak() {
        assertThatThrownBy(() -> engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2030, 1, 15), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Batas waktu");
    }

    @Test
    void koperasiLama_masaTransisiFlatSampai2029_bukanTerdaftarPlus3() {
        // Terdaftar 2023: rumus keliru "terdaftar+3" akan menolak sejak 2027 —
        // ketentuan peralihan PP 20/2026 memberi hak s.d. Tahun Pajak 2029.
        var hasil2027 = engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2027, 6, 1), LocalDate.of(2023, 3, 1));
        assertThat(hasil2027.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("500000.00"));

        var hasil2029 = engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2029, 12, 1), LocalDate.of(2023, 3, 1));
        assertThat(hasil2029.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("500000.00"));

        assertThatThrownBy(() -> engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2030, 2, 1), LocalDate.of(2023, 3, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Batas waktu");
    }

    @Test
    void koperasiEraPP55_resolveTanpaCekBatas() {
        // Rezim lama: baris PPH-FINAL-UMKM-KOPERASI 2022-12-20..2026-04-21,
        // kolom batas NULL (limitation terdokumentasi) — tidak ada cek batas.
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2025, 6, 1), LocalDate.of(2019, 1, 1));

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(hasil.regulasiAcuan()).contains("PP 55/2022");
    }

    @Test
    void koperasiTanpaTanggalTerdaftar_ditolakDenganPesanJelas() {
        assertThatThrownBy(() -> engine.hitungPphFinal(
                BentukBadanUsaha.KOPERASI,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2026, 7, 1), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tanggal terdaftar pajak wajib");
    }

    @Test
    void ptPerorangan_tanpaBatasWaktu_tanggalTerdaftarTidakDiwajibkan() {
        var hasil = engine.hitungPphFinal(
                BentukBadanUsaha.PT_PERORANGAN,
                new BigDecimal("100000000"), BigDecimal.ZERO,
                LocalDate.of(2030, 7, 1)); // jauh di masa depan, tetap berhak

        assertThat(hasil.pajakTerhitung()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }
}
