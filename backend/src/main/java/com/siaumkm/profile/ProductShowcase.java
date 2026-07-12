package com.siaumkm.profile;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** SRS-A-03: katalog produk showcase (BUKAN transaksional — bukan e-commerce). */
@Entity
@Table(name = "product_showcase")
public class ProductShowcase {
    @Id @GeneratedValue private UUID id;
    @Column(name = "business_profile_id", nullable = false) private UUID businessProfileId;
    private String nama;
    private String deskripsi;
    @Column(name = "harga_ditampilkan", precision = 19, scale = 2) private BigDecimal hargaDitampilkan;
    @Column(name = "tampilkan_harga") private boolean tampilkanHarga = true;
    @Column(name = "foto_url") private String fotoUrl;
    @Column(name = "urutan_tampil") private Integer urutanTampil = 0;

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public BigDecimal getHargaDitampilkan() { return hargaDitampilkan; }
    public void setHargaDitampilkan(BigDecimal v) { this.hargaDitampilkan = v; }
    public boolean isTampilkanHarga() { return tampilkanHarga; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String v) { this.fotoUrl = v; }
}
