package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B5-01: parameter pajak effective-dated. TIDAK PERNAH hardcode tarif di Java —
 * lihat CLAUDE.md Aturan Emas #3. Regulasi pajak UMKM sudah berubah 2x dalam 12
 * bulan terakhir (PP 55/2022 → PP 20/2026); desain ini mengantisipasi perubahan
 * berikutnya tanpa perlu redeploy kode.
 */
@Entity
@Table(name = "tax_rule")
public class TaxRule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "kode_aturan", nullable = false, length = 50)
    private String kodeAturan;

    private String deskripsi;

    @Column(name = "tarif_persen", nullable = false, precision = 6, scale = 4)
    private BigDecimal tarifPersen;

    @Column(name = "ambang_bawah", precision = 19, scale = 2)
    private BigDecimal ambangBawah;

    @Column(name = "ambang_atas", precision = 19, scale = 2)
    private BigDecimal ambangAtas;

    // Kolom array enum native PostgreSQL (bentuk_badan_usaha[]) sesuai schema.sql —
    // bukan @ElementCollection/tabel terpisah.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "bentuk_badan_berlaku", columnDefinition = "bentuk_badan_usaha[]")
    private List<BentukBadanUsaha> bentukBadanBerlaku;

    // Batas waktu pemanfaatan sebagai DATA (Aturan Emas #3): jumlah tahun pajak
    // maksimal sejak terdaftar (inklusif tahun terdaftar); NULL = tanpa batas.
    @Column(name = "batas_tahun_pajak")
    private Integer batasTahunPajak;

    // Tahun pajak terakhir bagi WP yang terdaftar SEBELUM berlaku_dari aturan
    // (masa transisi flat, mis. 2029 utk koperasi lama per PP 20/2026).
    @Column(name = "tahun_pajak_akhir_transisi")
    private Integer tahunPajakAkhirTransisi;

    @Column(name = "berlaku_dari", nullable = false)
    private LocalDate berlakuDari;

    @Column(name = "berlaku_sampai")
    private LocalDate berlakuSampai; // NULL = masih berlaku

    @Column(name = "regulasi_acuan", nullable = false, length = 200)
    private String regulasiAcuan;

    // getters
    public String getKodeAturan() { return kodeAturan; }
    public BigDecimal getTarifPersen() { return tarifPersen; }
    public BigDecimal getAmbangBawah() { return ambangBawah; }
    public BigDecimal getAmbangAtas() { return ambangAtas; }
    public List<BentukBadanUsaha> getBentukBadanBerlaku() { return bentukBadanBerlaku; }
    public Integer getBatasTahunPajak() { return batasTahunPajak; }
    public Integer getTahunPajakAkhirTransisi() { return tahunPajakAkhirTransisi; }
    public LocalDate getBerlakuDari() { return berlakuDari; }
    public LocalDate getBerlakuSampai() { return berlakuSampai; }
    public String getRegulasiAcuan() { return regulasiAcuan; }
}
