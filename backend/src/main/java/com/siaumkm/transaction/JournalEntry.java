package com.siaumkm.transaction;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B3-01/04: jurnal double-entry. STATUS POSTED bersifat immutable —
 * ditegakkan oleh trigger PostgreSQL prevent_update_posted_journal (lihat schema.sql),
 * bukan hanya validasi di sini. Jangan hapus trigger itu meski ada validasi Java ini.
 */
@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "nomor_jurnal", nullable = false, unique = true, length = 30)
    private String nomorJurnal;

    @Column(name = "tanggal_transaksi", nullable = false)
    private LocalDate tanggalTransaksi;

    private String keterangan;

    // NAMED_ENUM: kolom PostgreSQL bertipe enum bernama (status_jurnal) —
    // tanpa ini Hibernate mengikat parameter sebagai varchar dan PG menolak.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "metode_pembayaran")
    private MetodePembayaran metodePembayaran;

    @Column(name = "reversal_of_id")
    private UUID reversalOfId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "posted_at")
    private Instant postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    public enum Status { DRAFT, POSTED }

    // getters & setters
    public UUID getId() { return id; }
    public String getNomorJurnal() { return nomorJurnal; }
    public void setNomorJurnal(String v) { this.nomorJurnal = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public List<JournalLine> getLines() { return lines; }
    public void setTanggalTransaksi(LocalDate v) { this.tanggalTransaksi = v; }
    public LocalDate getTanggalTransaksi() { return tanggalTransaksi; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String v) { this.keterangan = v; }
    public MetodePembayaran getMetodePembayaran() { return metodePembayaran; }
    public void setMetodePembayaran(MetodePembayaran v) { this.metodePembayaran = v; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public UUID getReversalOfId() { return reversalOfId; }
    public void setReversalOfId(UUID v) { this.reversalOfId = v; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant v) { this.postedAt = v; }

    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }
}
