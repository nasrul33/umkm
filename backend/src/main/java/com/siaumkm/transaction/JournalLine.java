package com.siaumkm.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * SRS-B3: baris jurnal. Aturan emas #1 (CLAUDE.md) — uang selalu BigDecimal,
 * tidak pernah float/double. Kolom database NUMERIC(19,2) (lihat schema.sql).
 */
@Entity
@Table(name = "journal_line")
public class JournalLine {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "chart_of_account_id", nullable = false)
    private UUID chartOfAccountId;

    @Column(name = "debit", nullable = false, precision = 19, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "kredit", nullable = false, precision = 19, scale = 2)
    private BigDecimal kredit = BigDecimal.ZERO;

    @Column(name = "product_id")
    private UUID productId;

    public JournalLine() {}

    public JournalLine(UUID chartOfAccountId, BigDecimal debit, BigDecimal kredit) {
        this.chartOfAccountId = chartOfAccountId;
        this.debit = debit;
        this.kredit = kredit;
    }

    public void setJournalEntry(JournalEntry je) { this.journalEntry = je; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getKredit() { return kredit; }
    public UUID getChartOfAccountId() { return chartOfAccountId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
}
