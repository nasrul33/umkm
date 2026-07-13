package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

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

    public record HasilKalkulasi(BigDecimal omzetKenaPajak, BigDecimal pajakTerhitung,
                                  String regulasiAcuan, UUID taxRuleId) {}

    /** Kompatibilitas untuk bentuk badan tanpa batas waktu (OP/PT Perorangan). */
    public HasilKalkulasi hitungPphFinal(BentukBadanUsaha bentukBadan,
                                          BigDecimal omzetBrutoBulanan,
                                          BigDecimal omzetKumulatifTahunan,
                                          LocalDate tanggalTransaksi) {
        return hitungPphFinal(bentukBadan, omzetBrutoBulanan, omzetKumulatifTahunan,
                tanggalTransaksi, null);
    }

    /**
     * @param omzetBrutoBulanan omzet BULAN INI (bukan kumulatif tahunan)
     * @param omzetKumulatifTahunan kumulatif tahun berjalan SESUDAH menambahkan
     *        bulan berjalan — dasar rumus pengecualian Rp500jt (khusus OP)
     * @param tanggalTerdaftarPajak wajib bila aturan bentuk badan punya batas_tahun_pajak (koperasi)
     */
    public HasilKalkulasi hitungPphFinal(BentukBadanUsaha bentukBadan,
                                          BigDecimal omzetBrutoBulanan,
                                          BigDecimal omzetKumulatifTahunan,
                                          LocalDate tanggalTransaksi,
                                          LocalDate tanggalTerdaftarPajak) {

        String kodeAturan = switch (bentukBadan) {
            case OP -> "PPH-FINAL-UMKM-OP";
            case PT_PERORANGAN -> "PPH-FINAL-UMKM-PT-PERORANGAN";
            case KOPERASI -> "PPH-FINAL-UMKM-KOPERASI";
            default -> throw new IllegalStateException(
                "Bentuk badan " + bentukBadan + " tidak lagi berhak PPh Final sejak PP 20/2026 " +
                "(kecuali masa transisi ketentuan lama) — cek status_transisi_pajak, jangan hitung di sini.");
        };

        TaxRule rule = taxRuleRepository.findAturanAktif(kodeAturan, bentukBadan, tanggalTransaksi)
                .orElseThrow(() -> new IllegalStateException(
                    "Tidak ada tax_rule aktif untuk " + kodeAturan + " pada tanggal " + tanggalTransaksi +
                    " — kemungkinan data tax_rule belum di-seed atau regulasi baru belum ditambahkan via /add-tax-rule."));

        validasiBatasWaktu(rule, tanggalTransaksi, tanggalTerdaftarPajak);

        // PP 55/2022 Pasal 60 jo. PMK 164/2023 (dipertahankan PP 20/2026):
        // tarif hanya dikenakan atas BAGIAN peredaran bruto di atas pengecualian
        // (Rp500jt utk OP). Di bulan penembusan, DPP = kelebihan kumulatif di
        // atas ambang, dibatasi omzet bulan berjalan. DPP tidak pernah negatif
        // (pembalik lintas bulan tidak menghasilkan pajak negatif — koreksi masa
        // asal adalah proses pembetulan terpisah, lihat changelog limitation).
        BigDecimal omzetKenaPajak;
        if (rule.getAmbangBawah() != null) {
            BigDecimal kelebihan = omzetKumulatifTahunan.subtract(rule.getAmbangBawah());
            omzetKenaPajak = omzetBrutoBulanan.min(kelebihan).max(BigDecimal.ZERO);
        } else {
            omzetKenaPajak = omzetBrutoBulanan.max(BigDecimal.ZERO);
        }

        // PER-11/PJ/2025 (era Coretax): pajak terutang dibulatkan ARITMETIS ke
        // rupiah penuh (< 0,50 ke bawah, >= 0,50 ke atas = HALF_UP scale 0) —
        // di SINI, bukan di lapisan billing, agar angka di log, kode billing,
        // dan setoran identik. DPP tidak dipangkas (pembulatan ribuan hanya
        // utk PKP tarif umum Pasal 17(4), bukan PPh Final).
        BigDecimal pajak = omzetKenaPajak
                .multiply(rule.getTarifPersen())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);

        return new HasilKalkulasi(omzetKenaPajak, pajak, rule.getRegulasiAcuan(), rule.getId());
    }

    /**
     * Batas waktu pemanfaatan sebagai DATA di tax_rule (Aturan Emas #3) — saat
     * ini terisi hanya utk KOPERASI (PP 20/2026: maks 4 tahun pajak sejak
     * terdaftar, inklusif tahun terdaftar; yang terdaftar sebelum PP berlaku
     * memakai masa transisi flat s.d. tahun_pajak_akhir_transisi, mis. 2029).
     */
    private void validasiBatasWaktu(TaxRule rule, LocalDate tanggalTransaksi,
                                    LocalDate tanggalTerdaftarPajak) {
        if (rule.getBatasTahunPajak() == null) return; // tanpa batas waktu

        if (tanggalTerdaftarPajak == null) {
            throw new IllegalStateException(
                "Tanggal terdaftar pajak wajib diisi untuk bentuk badan dengan batas waktu "
                + "PPh Final (" + rule.getKodeAturan() + ") — lengkapi identitas usaha (Modul B1).");
        }

        boolean terdaftarSebelumAturan = tanggalTerdaftarPajak.isBefore(rule.getBerlakuDari());
        int tahunTerakhir = (terdaftarSebelumAturan && rule.getTahunPajakAkhirTransisi() != null)
                ? rule.getTahunPajakAkhirTransisi()
                : tanggalTerdaftarPajak.getYear() + rule.getBatasTahunPajak() - 1;

        if (tanggalTransaksi.getYear() > tahunTerakhir) {
            throw new IllegalStateException(
                "Batas waktu PPh Final terlampaui: terdaftar " + tanggalTerdaftarPajak
                + ", tahun pajak terakhir " + tahunTerakhir + " (" + rule.getRegulasiAcuan()
                + ") — penghasilan dihitung dengan ketentuan umum UU PPh, cek status_transisi_pajak.");
        }
    }
}
