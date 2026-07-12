package com.siaumkm.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** SRS-B3-05: baris invoice. Uang selalu BigDecimal/NUMERIC (Aturan Emas #1). */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id @GeneratedValue private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "product_id") private UUID productId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal qty = BigDecimal.ONE;

    @Column(name = "harga_satuan", nullable = false, precision = 19, scale = 2)
    private BigDecimal hargaSatuan;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    public UUID getId() { return id; }
    public void setInvoice(Invoice v) { this.invoice = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal v) { this.qty = v; }
    public BigDecimal getHargaSatuan() { return hargaSatuan; }
    public void setHargaSatuan(BigDecimal v) { this.hargaSatuan = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }
}
