package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SRS-B5-01..03: mesin kalkulasi PPh Final. Contoh implementasi untuk WP OP
 * pasca PP 20/2026 (tanpa batas waktu, dengan pengecualian omzet s.d. Rp500jt).
 *
 * PENTING (lihat tax-compliance-specialist agent):
 * - Sistem TIDAK PERNAH menampilkan opini/rekomendasi pajak — hanya angka hasil
 *   kalkulasi + rujukan pasal (getRegulasiAcuan()). Jangan tambah field/method
 *   yang menghasilkan teks rekomendasi personal di layer ini.
 * - Agregasi omzet lintas entitas/keluarga (Pasal 58 PP 20/2026) TIDAK termasuk
 *   di kelas ini — lihat OmzetAggregationJob (SRS-B5-02) yang menghasilkan
 *   nilai omzetBruto yang sudah teragregasi SEBELUM dipanggil ke sini.
 */
@Service
public class TaxCalculationEngine {

    private final TaxRuleRepository taxRuleRepository;

    public TaxCalculationEngine(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    public record HasilKalkulasi(BigDecimal omzetKenaPajak, BigDecimal pajakTerhitung, String regulasiAcuan) {}

    /**
     * @param omzetBrutoBulanan omzet BULAN INI (bukan kumulatif tahunan — kumulatif
     *                          dihitung oleh caller/OmzetAggregationJob untuk cek ambang)
     * @param omzetKumulatifTahunan omzet kumulatif tahun berjalan, untuk cek pengecualian Rp500jt (khusus OP)
     */
    public HasilKalkulasi hitungPphFinal(BentukBadanUsaha bentukBadan,
                                          BigDecimal omzetBrutoBulanan,
                                          BigDecimal omzetKumulatifTahunan,
                                          LocalDate tanggalTransaksi) {

        String kodeAturan = switch (bentukBadan) {
            case OP -> "PPH-FINAL-UMKM-OP";
            case PT_PERORANGAN, KOPERASI -> "PPH-FINAL-UMKM-PT-PERORANGAN";
            default -> throw new IllegalStateException(
                "Bentuk badan " + bentukBadan + " tidak lagi berhak PPh Final sejak PP 20/2026 " +
                "(kecuali masa transisi ketentuan lama) — cek status_transisi_pajak, jangan hitung di sini.");
        };

        TaxRule rule = taxRuleRepository.findAturanAktif(kodeAturan, bentukBadan, tanggalTransaksi)
                .orElseThrow(() -> new IllegalStateException(
                    "Tidak ada tax_rule aktif untuk " + kodeAturan + " pada tanggal " + tanggalTransaksi +
                    " — kemungkinan data tax_rule belum di-seed atau regulasi baru belum ditambahkan via /add-tax-rule."));

        BigDecimal omzetKenaPajak = omzetBrutoBulanan;

        // Pengecualian omzet s.d. Rp500jt/tahun khusus WP OP (SRS-B5-02)
        if (bentukBadan == BentukBadanUsaha.OP && rule.getAmbangBawah() != null) {
            if (omzetKumulatifTahunan.compareTo(rule.getAmbangBawah()) <= 0) {
                return new HasilKalkulasi(BigDecimal.ZERO, BigDecimal.ZERO, rule.getRegulasiAcuan());
            }
        }

        BigDecimal pajak = omzetKenaPajak
                .multiply(rule.getTarifPersen())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new HasilKalkulasi(omzetKenaPajak, pajak, rule.getRegulasiAcuan());
    }
}
