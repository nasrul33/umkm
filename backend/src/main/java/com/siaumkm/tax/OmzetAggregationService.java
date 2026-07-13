package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import com.siaumkm.identity.BusinessEntityRepository;
import com.siaumkm.identity.RelatedEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * SRS-B5-02 / BR-B5-03/04: agregasi omzet Pasal 58 PP 20/2026 (anti pemecahan
 * usaha) + monitoring ambang peredaran bruto. Desain hasil konsultasi
 * tax-compliance-specialist:
 * - Omzet utama = SUM(kredit-debit) akun PENDAPATAN ber-is_omzet_usaha dari
 *   jurnal POSTED per tanggal transaksi (accrual); jurnal pembalik otomatis
 *   tertangani karena mendebit akun pendapatan.
 * - Agregasi keluarga berlaku utk OP & PT_PERORANGAN (satuan = entitas usaha,
 *   via related_entity); KOPERASI dinilai omzetnya sendiri; CV/FIRMA/PT_BIASA
 *   di luar skema PPh Final — tetap dihitung utk monitoring, tanpa exception.
 * - Ambang dari tax_rule aktif per min(31 Des tahun agregasi, hari ini) —
 *   BUKAN hardcode (Aturan Emas #3). Rasio peringatan 80% adalah parameter
 *   monitoring produk (bukan regulasi), configurable via properti.
 * - TERLAMPAUI hanya bila omzet gabungan > ambang (tepat sama masih memenuhi);
 *   konsekuensinya berlaku untuk TAHUN PAJAK BERIKUTNYA (Pasal 58).
 */
@Service
public class OmzetAggregationService {

    public enum StatusAmbang { DI_BAWAH_BATAS, MENDEKATI_BATAS, MELEBIHI_BATAS, TIDAK_BERHAK_PPH_FINAL }

    public record HasilAgregasi(int tahun,
                                 BigDecimal omzetEntitasUtama,
                                 BigDecimal omzetEntitasTerkait,
                                 BigDecimal omzetGabungan,
                                 StatusAmbang status,
                                 BigDecimal ambangAtas,
                                 String regulasiAcuan,
                                 boolean dataEntitasTerkaitLengkap,
                                 String keterangan) {}

    private final JdbcTemplate jdbcTemplate;
    private final OmzetUsahaQuery omzetUsahaQuery;
    private final BusinessEntityRepository businessEntityRepository;
    private final RelatedEntityRepository relatedEntityRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final AggregatedOmzetRepository aggregatedOmzetRepository;
    private final BigDecimal warningRatio;

    public OmzetAggregationService(JdbcTemplate jdbcTemplate,
                                   OmzetUsahaQuery omzetUsahaQuery,
                                   BusinessEntityRepository businessEntityRepository,
                                   RelatedEntityRepository relatedEntityRepository,
                                   TaxRuleRepository taxRuleRepository,
                                   AggregatedOmzetRepository aggregatedOmzetRepository,
                                   @Value("${siaumkm.tax.omzet-warning-ratio:0.80}") BigDecimal warningRatio) {
        this.jdbcTemplate = jdbcTemplate;
        this.omzetUsahaQuery = omzetUsahaQuery;
        this.businessEntityRepository = businessEntityRepository;
        this.relatedEntityRepository = relatedEntityRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.aggregatedOmzetRepository = aggregatedOmzetRepository;
        this.warningRatio = warningRatio;
    }

    /**
     * Hasil tersimpan TERAKHIR tanpa menghitung ulang — utk dashboard (read
     * murni, tanpa side effect); status ambang dinilai ulang dari tax_rule
     * atas angka tersimpan.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<HasilAgregasi> hasilTerakhir() {
        return businessEntityRepository.findAll().stream().findFirst().flatMap(utama ->
            aggregatedOmzetRepository.findFirstByBusinessEntityIdOrderByCalculatedAtDesc(utama.getId())
                .map(baris -> {
                    boolean agregasiKeluargaBerlaku = utama.getBentukBadan() == BentukBadanUsaha.OP
                            || utama.getBentukBadan() == BentukBadanUsaha.PT_PERORANGAN;
                    boolean lengkap = relatedEntityRepository
                            .countByBusinessEntityIdAndOmzetTahunanDiketahuiIsNull(utama.getId()) == 0;
                    return nilaiAmbang(utama, baris.getPeriodeTahun(),
                            baris.getOmzetEntitasUtama(), baris.getOmzetEntitasTerkait(),
                            baris.getOmzetGabungan(), agregasiKeluargaBerlaku, lengkap);
                }));
    }

    /** Varian single-tenant: agregasi untuk satu-satunya business_entity terdaftar. */
    @Transactional
    public HasilAgregasi hitungUlang(int tahun) {
        BusinessEntity utama = businessEntityRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                    "Identitas usaha belum diisi — lengkapi Modul B1 terlebih dahulu."));
        return hitungUlang(utama.getId(), tahun);
    }

    @Transactional
    public HasilAgregasi hitungUlang(UUID businessEntityId, int tahun) {
        BusinessEntity utama = businessEntityRepository.findById(businessEntityId)
                .orElseThrow(() -> new NoSuchElementException("business_entity " + businessEntityId + " tidak ditemukan."));

        BigDecimal omzetUtama = omzetUsahaTahun(tahun);
        BigDecimal omzetTerkait = omzetEntitasTerkait(businessEntityId);
        boolean lengkap = relatedEntityRepository
                .countByBusinessEntityIdAndOmzetTahunanDiketahuiIsNull(businessEntityId) == 0;

        // Pasal 58: penggabungan lingkaran keluarga berlaku bagi OP dan
        // perseroan perorangan; koperasi dinilai atas omzetnya sendiri.
        boolean agregasiKeluargaBerlaku = utama.getBentukBadan() == BentukBadanUsaha.OP
                || utama.getBentukBadan() == BentukBadanUsaha.PT_PERORANGAN;
        BigDecimal omzetGabungan = agregasiKeluargaBerlaku ? omzetUtama.add(omzetTerkait) : omzetUtama;

        upsert(businessEntityId, tahun, omzetUtama, omzetTerkait, omzetGabungan);

        return nilaiAmbang(utama, tahun, omzetUtama, omzetTerkait, omzetGabungan,
                agregasiKeluargaBerlaku, lengkap);
    }

    private BigDecimal omzetUsahaTahun(int tahun) {
        return omzetUsahaQuery.hitung(LocalDate.of(tahun, 1, 1), LocalDate.of(tahun + 1, 1, 1));
    }

    private BigDecimal omzetEntitasTerkait(UUID businessEntityId) {
        // NULL diperlakukan 0, tetapi hasil diberi flag "tidak lengkap" oleh caller.
        BigDecimal hasil = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(COALESCE(omzet_tahunan_diketahui, 0)), 0)
            FROM related_entity WHERE business_entity_id = ?
            """, BigDecimal.class, businessEntityId);
        return hasil.setScale(2, RoundingMode.HALF_UP);
    }

    private void upsert(UUID businessEntityId, int tahun,
                        BigDecimal utama, BigDecimal terkait, BigDecimal gabungan) {
        // ON CONFLICT: race-safe saat job terjadwal & rekalkulasi on-demand
        // berjalan bersamaan (UNIQUE business_entity_id + periode_tahun).
        jdbcTemplate.update("""
            INSERT INTO aggregated_omzet
                (business_entity_id, periode_tahun, omzet_entitas_utama,
                 omzet_entitas_terkait, omzet_gabungan, calculated_at)
            VALUES (?, ?, ?, ?, ?, now())
            ON CONFLICT (business_entity_id, periode_tahun) DO UPDATE SET
                omzet_entitas_utama   = EXCLUDED.omzet_entitas_utama,
                omzet_entitas_terkait = EXCLUDED.omzet_entitas_terkait,
                omzet_gabungan        = EXCLUDED.omzet_gabungan,
                calculated_at         = now()
            """, businessEntityId, tahun, utama, terkait, gabungan);
    }

    private HasilAgregasi nilaiAmbang(BusinessEntity utama, int tahun,
                                      BigDecimal omzetUtama, BigDecimal omzetTerkait,
                                      BigDecimal omzetGabungan,
                                      boolean agregasiKeluargaBerlaku, boolean lengkap) {
        String kodeAturan = switch (utama.getBentukBadan()) {
            case OP -> "PPH-FINAL-UMKM-OP";
            case PT_PERORANGAN -> "PPH-FINAL-UMKM-PT-PERORANGAN";
            case KOPERASI -> "PPH-FINAL-UMKM-KOPERASI"; // baris sendiri sejak V7 (batas 4 tahun)
            default -> null; // CV/FIRMA/PT_BIASA: di luar skema PPh Final — monitoring saja
        };

        if (kodeAturan == null) {
            return new HasilAgregasi(tahun, omzetUtama, omzetTerkait, omzetGabungan,
                    StatusAmbang.TIDAK_BERHAK_PPH_FINAL, null, null, lengkap,
                    "Bentuk badan " + utama.getBentukBadan() + " tidak termasuk skema PPh Final "
                    + "peredaran bruto tertentu — lihat status transisi pajak ("
                    + utama.getStatusTransisiPajak() + ").");
        }

        // Aturan aktif per akhir periode yang dinilai (bukan tanggal sistem) —
        // konsisten dengan prinsip effective-dated TaxCalculationEngine.
        LocalDate akhirTahun = LocalDate.of(tahun, 12, 31);
        LocalDate referensi = akhirTahun.isAfter(LocalDate.now()) ? LocalDate.now() : akhirTahun;

        TaxRule rule = taxRuleRepository.findAturanAktif(kodeAturan, utama.getBentukBadan(), referensi)
                .orElseThrow(() -> new IllegalStateException(
                    "Tidak ada tax_rule aktif untuk " + kodeAturan + " pada " + referensi +
                    " — kemungkinan aturan periode tsb belum di-seed via /add-tax-rule."));

        BigDecimal ambang = rule.getAmbangAtas();
        StatusAmbang status;
        if (omzetGabungan.compareTo(ambang) > 0) {
            status = StatusAmbang.MELEBIHI_BATAS; // tepat sama dgn ambang masih memenuhi
        } else if (omzetGabungan.compareTo(ambang.multiply(warningRatio)) >= 0) {
            status = StatusAmbang.MENDEKATI_BATAS;
        } else {
            status = StatusAmbang.DI_BAWAH_BATAS;
        }

        return new HasilAgregasi(tahun, omzetUtama, omzetTerkait, omzetGabungan, status,
                ambang, rule.getRegulasiAcuan(), lengkap,
                keterangan(status, tahun, omzetGabungan, ambang, rule.getRegulasiAcuan(),
                        agregasiKeluargaBerlaku, lengkap));
    }

    /**
     * BR-B5-11 / Aturan Emas #5: fakta angka + rujukan pasal, TANPA verba
     * anjuran (sebaiknya/disarankan/strategi/manfaatkan dilarang) — sistem
     * bukan konsultan pajak.
     */
    private String keterangan(StatusAmbang status, int tahun, BigDecimal gabungan,
                              BigDecimal ambang, String regulasi,
                              boolean agregasiKeluargaBerlaku, boolean lengkap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Omzet gabungan tahun ").append(tahun).append(" tercatat Rp")
          .append(format(gabungan)).append(" dari batas peredaran bruto Rp")
          .append(format(ambang)).append(" (").append(regulasi).append(").");
        if (agregasiKeluargaBerlaku) {
            sb.append(" Sesuai Pasal 58, penghitungan menggabungkan peredaran bruto usaha ini, ")
              .append("pasangan, anak yang belum dewasa, serta perseroan perorangan yang didirikan oleh mereka.");
        }
        if (status == StatusAmbang.MELEBIHI_BATAS) {
            sb.append(" Berdasarkan ketentuan yang berlaku, penghasilan usaha pada tahun pajak ")
              .append("berikutnya tidak lagi dikenai PPh Final 0,5% dan dihitung dengan ketentuan umum UU PPh.");
        }
        if (!lengkap) {
            sb.append(" Catatan: sebagian entitas terkait belum memiliki data omzet — angka gabungan belum utuh.");
        }
        return sb.toString();
    }

    private String format(BigDecimal nilai) {
        return String.format("%,.0f", nilai).replace(',', '.');
    }
}
