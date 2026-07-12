package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** SRS-B2-04: aset tetap, dasar penyusutan otomatis (jurnal berulang B3). */
@Entity
@Table(name = "fixed_asset")
public class FixedAsset {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false) private String nama;

    @Column(name = "tanggal_perolehan", nullable = false)
    private LocalDate tanggalPerolehan;

    @Column(name = "nilai_perolehan", nullable = false, precision = 19, scale = 2)
    private BigDecimal nilaiPerolehan;

    @Column(name = "metode_penyusutan", nullable = false, length = 30)
    private String metodePenyusutan = "STRAIGHT_LINE";

    @Column(name = "umur_manfaat_bulan", nullable = false)
    private Integer umurManfaatBulan;

    @Column(name = "nilai_residu", nullable = false, precision = 19, scale = 2)
    private BigDecimal nilaiResidu = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public LocalDate getTanggalPerolehan() { return tanggalPerolehan; }
    public void setTanggalPerolehan(LocalDate v) { this.tanggalPerolehan = v; }
    public BigDecimal getNilaiPerolehan() { return nilaiPerolehan; }
    public void setNilaiPerolehan(BigDecimal v) { this.nilaiPerolehan = v; }
    public String getMetodePenyusutan() { return metodePenyusutan; }
    public void setMetodePenyusutan(String v) { this.metodePenyusutan = v; }
    public Integer getUmurManfaatBulan() { return umurManfaatBulan; }
    public void setUmurManfaatBulan(Integer v) { this.umurManfaatBulan = v; }
    public BigDecimal getNilaiResidu() { return nilaiResidu; }
    public void setNilaiResidu(BigDecimal v) { this.nilaiResidu = v; }
}
