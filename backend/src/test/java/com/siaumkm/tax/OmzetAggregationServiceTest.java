package com.siaumkm.tax;

import com.siaumkm.auth.AppUser;
import com.siaumkm.auth.AppUserRepository;
import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import com.siaumkm.identity.BusinessEntityRepository;
import com.siaumkm.identity.RelatedEntity;
import com.siaumkm.identity.RelatedEntityRepository;
import com.siaumkm.masterdata.ChartOfAccountRepository;
import com.siaumkm.tax.OmzetAggregationService.HasilAgregasi;
import com.siaumkm.tax.OmzetAggregationService.StatusAmbang;
import com.siaumkm.transaction.JournalEntry;
import com.siaumkm.transaction.JournalEntryRepository;
import com.siaumkm.transaction.JournalLine;
import com.siaumkm.transaction.JournalPostingService;
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

/**
 * SRS-B5-02 / Pasal 58 PP 20/2026 — WAJIB PostgreSQL asli (Testcontainers):
 * memverifikasi juga upsert ON CONFLICT dan trigger audit hash-chain.
 * Isolasi antar test memakai TAHUN berbeda (jurnal bersifat tenant-global).
 */
@Testcontainers
@SpringBootTest
class OmzetAggregationServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private OmzetAggregationService service;
    @Autowired private JournalPostingService postingService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private BusinessEntityRepository businessEntityRepository;
    @Autowired private RelatedEntityRepository relatedEntityRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final BigDecimal AMBANG = new BigDecimal("4800000000");

    // ---- Sumber omzet utama ----

    @Test
    void jurnalPembalikDalamTahunSama_omzetNettoBenar() {
        UUID be = entitas(BentukBadanUsaha.OP);
        // pembalik selalu bertanggal HARI INI — pakai tahun berjalan agar satu tahun
        int tahun = LocalDate.now().getYear();
        JournalEntry sumber = jurnalPendapatanPosted("500000", LocalDate.of(tahun, 3, 10));
        postingService.balikkan(sumber.getId(), user());

        HasilAgregasi hasil = service.hitungUlang(be, tahun);

        assertThat(hasil.omzetEntitasUtama()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void jurnalDraft_tidakIkutTerhitung() {
        UUID be = entitas(BentukBadanUsaha.OP);
        jurnalPendapatanPosted("300000", LocalDate.of(2030, 5, 1));
        jurnalPendapatanDraft("200000", LocalDate.of(2030, 6, 1));

        HasilAgregasi hasil = service.hitungUlang(be, 2030);

        assertThat(hasil.omzetEntitasUtama()).isEqualByComparingTo(new BigDecimal("300000.00"));
    }

    @Test
    void tahunTanpaTransaksi_barisTersimpanDenganNol() {
        UUID be = entitas(BentukBadanUsaha.OP);

        HasilAgregasi hasil = service.hitungUlang(be, 2036);

        assertThat(hasil.omzetGabungan()).isEqualByComparingTo(BigDecimal.ZERO);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM aggregated_omzet WHERE business_entity_id = ? AND periode_tahun = 2036",
                Integer.class, be);
        assertThat(rows).isEqualTo(1);
    }

    // ---- Ambang batas (dari tax_rule, bukan hardcode) ----

    @Test
    void omzetTepatPadaAmbang_belumMelebihi() {
        UUID be = entitas(BentukBadanUsaha.OP);
        entitasTerkait(be, AMBANG); // gabungan tepat 4,8 M

        HasilAgregasi hasil = service.hitungUlang(be, 2031);

        // "tidak melebihi" — tepat sama masih memenuhi kriteria
        assertThat(hasil.status()).isEqualTo(StatusAmbang.MENDEKATI_BATAS);
    }

    @Test
    void omzetLewatAmbangSatuSen_melebihi() {
        UUID be = entitas(BentukBadanUsaha.OP);
        entitasTerkait(be, new BigDecimal("4800000000.01"));

        HasilAgregasi hasil = service.hitungUlang(be, 2032);

        assertThat(hasil.status()).isEqualTo(StatusAmbang.MELEBIHI_BATAS);
        assertThat(hasil.keterangan()).contains("tahun pajak").contains("berikutnya");
    }

    @Test
    void batasPeringatan80Persen_tepatDanDiBawahnya() {
        UUID be = entitas(BentukBadanUsaha.OP);
        entitasTerkait(be, new BigDecimal("3840000000.00")); // tepat 80%
        assertThat(service.hitungUlang(be, 2033).status())
                .isEqualTo(StatusAmbang.MENDEKATI_BATAS);

        UUID be2 = entitas(BentukBadanUsaha.OP);
        entitasTerkait(be2, new BigDecimal("3839999999.99"));
        assertThat(service.hitungUlang(be2, 2038).status())
                .isEqualTo(StatusAmbang.DI_BAWAH_BATAS);
    }

    // ---- Cakupan Pasal 58 per bentuk badan ----

    @Test
    void cv_tidakBerhakPphFinal_tanpaException_omzetTetapTersimpan() {
        UUID be = entitas(BentukBadanUsaha.CV);
        entitasTerkait(be, new BigDecimal("1000000"));

        HasilAgregasi hasil = service.hitungUlang(be, 2035);

        assertThat(hasil.status()).isEqualTo(StatusAmbang.TIDAK_BERHAK_PPH_FINAL);
        assertThat(hasil.ambangAtas()).isNull();
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM aggregated_omzet WHERE business_entity_id = ?", Integer.class, be);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void koperasi_omzetKeluargaTidakDigabung() {
        UUID be = entitas(BentukBadanUsaha.KOPERASI);
        entitasTerkait(be, new BigDecimal("9000000000")); // > ambang, tapi bukan milik koperasi

        HasilAgregasi hasil = service.hitungUlang(be, 2039);

        assertThat(hasil.omzetEntitasTerkait()).isEqualByComparingTo(new BigDecimal("9000000000"));
        assertThat(hasil.omzetGabungan()).isEqualByComparingTo(BigDecimal.ZERO); // dinilai omzet sendiri
        assertThat(hasil.status()).isEqualTo(StatusAmbang.DI_BAWAH_BATAS);
    }

    // ---- Kelengkapan data & idempotensi ----

    @Test
    void omzetTerkaitNull_dihitungNol_denganFlagTidakLengkap() {
        UUID be = entitas(BentukBadanUsaha.OP);
        entitasTerkait(be, null);

        HasilAgregasi hasil = service.hitungUlang(be, 2034);

        assertThat(hasil.omzetEntitasTerkait()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(hasil.dataEntitasTerkaitLengkap()).isFalse();
        assertThat(hasil.keterangan()).contains("belum utuh");
    }

    @Test
    void rekalkulasiBerulang_upsertSatuBaris() {
        UUID be = entitas(BentukBadanUsaha.OP);
        service.hitungUlang(be, 2037);
        entitasTerkait(be, new BigDecimal("50000"));
        HasilAgregasi kedua = service.hitungUlang(be, 2037);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM aggregated_omzet WHERE business_entity_id = ? AND periode_tahun = 2037",
                Integer.class, be);
        assertThat(rows).isEqualTo(1);
        assertThat(kedua.omzetEntitasTerkait()).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void perubahanOmzetTerkait_terekamAuditHashChain() {
        UUID be = entitas(BentukBadanUsaha.OP);
        RelatedEntity re = entitasTerkait(be, new BigDecimal("100"));
        re.setOmzetTahunanDiketahui(new BigDecimal("200"));
        relatedEntityRepository.saveAndFlush(re);

        Integer auditRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE table_name = 'related_entity' AND row_id = ?",
                Integer.class, re.getId());
        assertThat(auditRows).isGreaterThanOrEqualTo(2); // INSERT + UPDATE
    }

    // ---- helpers ----

    private UUID entitas(BentukBadanUsaha bentuk) {
        BusinessEntity be = new BusinessEntity();
        be.setNamaUsaha("Usaha Agregasi " + UUID.randomUUID());
        be.setNamaPemilik("Pemilik Uji");
        be.setNikPemilik("3175011122233344");
        be.setBentukBadan(bentuk);
        return businessEntityRepository.saveAndFlush(be).getId();
    }

    private RelatedEntity entitasTerkait(UUID businessEntityId, BigDecimal omzet) {
        RelatedEntity re = new RelatedEntity();
        re.setBusinessEntityId(businessEntityId);
        re.setNamaEntitasLain("Entitas Terkait " + UUID.randomUUID());
        re.setOmzetTahunanDiketahui(omzet);
        return relatedEntityRepository.saveAndFlush(re);
    }

    private JournalEntry jurnalPendapatanPosted(String jumlah, LocalDate tanggal) {
        JournalEntry je = jurnalPendapatan(jumlah, tanggal);
        je.setStatus(JournalEntry.Status.POSTED); // INSERT ber-status POSTED valid
        je.setPostedAt(Instant.now());
        return journalEntryRepository.saveAndFlush(je);
    }

    private void jurnalPendapatanDraft(String jumlah, LocalDate tanggal) {
        journalEntryRepository.saveAndFlush(jurnalPendapatan(jumlah, tanggal));
    }

    private JournalEntry jurnalPendapatan(String jumlah, LocalDate tanggal) {
        BigDecimal nilai = new BigDecimal(jumlah);
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal("JUT-" + System.nanoTime());
        je.setTanggalTransaksi(tanggal);
        je.setKeterangan("Penjualan uji agregasi");
        je.setCreatedBy(user());
        je.addLine(new JournalLine(akun("1000"), nilai, BigDecimal.ZERO));
        je.addLine(new JournalLine(akun("4000"), BigDecimal.ZERO, nilai));
        return je;
    }

    private UUID akun(String kode) {
        return chartOfAccountRepository.findByKodeAkun(kode).orElseThrow().getId();
    }

    private UUID user() {
        return appUserRepository.findByUsername("owner-agregasi-uji").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("owner-agregasi-uji");
            u.setPasswordHash("bukan-untuk-login");
            u.setNama("Owner Agregasi Uji");
            u.setPeran(AppUser.PeranPengguna.OWNER);
            return appUserRepository.save(u);
        }).getId();
    }
}
