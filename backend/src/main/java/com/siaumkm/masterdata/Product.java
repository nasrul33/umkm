package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** SRS-B2-02: master produk/jasa, dirujuk oleh journal_line (B3) dan vw_product_margin (B6). */
@Entity
@Table(name = "product")
public class Product {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false) private String nama;
    private String kategori;
    private String satuan = "pcs";

    @Column(name = "harga_jual", nullable = false, precision = 19, scale = 2)
    private BigDecimal hargaJual = BigDecimal.ZERO;

    @Column(name = "hpp_dasar", nullable = false, precision = 19, scale = 2)
    private BigDecimal hppDasar = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false) private boolean isActive = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public BigDecimal getHargaJual() { return hargaJual; }
    public void setHargaJual(BigDecimal v) { this.hargaJual = v; }
    public BigDecimal getHppDasar() { return hppDasar; }
    public void setHppDasar(BigDecimal v) { this.hppDasar = v; }
}
