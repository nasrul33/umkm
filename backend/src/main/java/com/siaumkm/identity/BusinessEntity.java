package com.siaumkm.identity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SRS-B1-01/02: fondasi identitas legal usaha, penentu seluruh logika perpajakan.
 * Rujuk tax-compliance-specialist agent sebelum mengubah enum BentukBadanUsaha —
 * bentuk badan menentukan kelayakan PPh Final sesuai PP 20/2026.
 */
@Entity
@Table(name = "business_entity")
public class BusinessEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "nama_usaha", nullable = false, length = 200)
    private String namaUsaha;

    @NotNull
    @Column(name = "nama_pemilik", nullable = false, length = 200)
    private String namaPemilik;

    @Pattern(regexp = "^[0-9]{16}$", message = "NPWP harus 16 digit (format Coretax)")
    @Column(name = "npwp", unique = true, length = 16)
    private String npwp;

    @NotNull
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK harus 16 digit")
    @Column(name = "nik_pemilik", nullable = false, length = 16)
    private String nikPemilik;

    @Column(name = "nib", length = 20)
    private String nib;

    // NAMED_ENUM: kolom PG bertipe enum bernama (bentuk_badan_usaha) —
    // tanpa ini Hibernate mengikat parameter sebagai varchar dan PG menolak.
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bentuk_badan", nullable = false)
    private BentukBadanUsaha bentukBadan;

    @Column(name = "klu_code", length = 10)
    private String kluCode;

    @Column(name = "status_pkp", nullable = false)
    private boolean statusPkp = false;

    @Column(name = "tanggal_pengukuhan_pkp")
    private LocalDate tanggalPengukuhanPkp;

    @Column(name = "alamat_usaha")
    private String alamatUsaha;

    @Column(name = "alamat_domisili")
    private String alamatDomisili;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_transisi_pajak", nullable = false)
    private StatusTransisiPajak statusTransisiPajak = StatusTransisiPajak.NONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // getters & setters
    public UUID getId() { return id; }
    public String getNamaUsaha() { return namaUsaha; }
    public void setNamaUsaha(String v) { this.namaUsaha = v; }
    public String getNamaPemilik() { return namaPemilik; }
    public void setNamaPemilik(String v) { this.namaPemilik = v; }
    public String getNpwp() { return npwp; }
    public void setNpwp(String v) { this.npwp = v; }
    public String getNikPemilik() { return nikPemilik; }
    public void setNikPemilik(String v) { this.nikPemilik = v; }
    public BentukBadanUsaha getBentukBadan() { return bentukBadan; }
    public void setBentukBadan(BentukBadanUsaha v) { this.bentukBadan = v; }
    public boolean isStatusPkp() { return statusPkp; }
    public void setStatusPkp(boolean v) { this.statusPkp = v; }
    public StatusTransisiPajak getStatusTransisiPajak() { return statusTransisiPajak; }
    public void setStatusTransisiPajak(StatusTransisiPajak v) { this.statusTransisiPajak = v; }

    public enum BentukBadanUsaha { OP, PT_PERORANGAN, CV, FIRMA, KOPERASI, PT_BIASA }
    public enum StatusTransisiPajak { NONE, MONITORING, TRANSITION_REQUIRED, TRANSITIONED }
}
