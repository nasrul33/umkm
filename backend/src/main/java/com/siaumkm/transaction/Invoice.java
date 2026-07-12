package com.siaumkm.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B3-05: invoice/tagihan pelanggan. Tercakup audit hash-chain
 * (trg_audit_invoice di schema.sql) — jangan tulis langsung via SQL mentah.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id @GeneratedValue private UUID id;

    @Column(name = "nomor_invoice", nullable = false, unique = true, length = 30)
    private String nomorInvoice;

    @Column(name = "customer_id") private UUID customerId;

    @Column(name = "journal_entry_id") private UUID journalEntryId;

    @Column(name = "tanggal_invoice", nullable = false)
    private LocalDate tanggalInvoice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "UNPAID";

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    public UUID getId() { return id; }
    public String getNomorInvoice() { return nomorInvoice; }
    public void setNomorInvoice(String v) { this.nomorInvoice = v; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID v) { this.customerId = v; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID v) { this.journalEntryId = v; }
    public LocalDate getTanggalInvoice() { return tanggalInvoice; }
    public void setTanggalInvoice(LocalDate v) { this.tanggalInvoice = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public List<InvoiceLine> getLines() { return lines; }

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }
}
