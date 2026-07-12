package com.siaumkm.identity;

import com.siaumkm.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-B1-02: entitas usaha lain dalam lingkaran Pasal 58 PP 20/2026 — milik
 * pemilik sendiri maupun anggota keluarga (tautkan via familyMemberId).
 * omzet_tahunan_diketahui adalah input manual yang memengaruhi angka pajak —
 * perubahan tercatat di audit hash-chain (trg_audit_related_entity, V6).
 */
@Entity
@Table(name = "related_entity")
public class RelatedEntity {

    @Id @GeneratedValue private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "nama_entitas_lain", nullable = false)
    private String namaEntitasLain;

    // NFR-05: ciphertext AES-256-GCM; format divalidasi atas plaintext.
    @Pattern(regexp = "^[0-9]{16}$", message = "NPWP harus 16 digit (format Coretax)")
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "npwp_entitas_lain", length = 128)
    private String npwpEntitasLain;

    @Column(name = "omzet_tahunan_diketahui", precision = 19, scale = 2)
    private BigDecimal omzetTahunanDiketahui; // NULL = belum diketahui, agregasi diberi flag tidak lengkap

    @Column(name = "family_member_id")
    private UUID familyMemberId; // NULL = milik pemilik sendiri

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public void setBusinessEntityId(UUID v) { this.businessEntityId = v; }
    public String getNamaEntitasLain() { return namaEntitasLain; }
    public void setNamaEntitasLain(String v) { this.namaEntitasLain = v; }
    public String getNpwpEntitasLain() { return npwpEntitasLain; }
    public void setNpwpEntitasLain(String v) { this.npwpEntitasLain = v; }
    public BigDecimal getOmzetTahunanDiketahui() { return omzetTahunanDiketahui; }
    public void setOmzetTahunanDiketahui(BigDecimal v) { this.omzetTahunanDiketahui = v; }
    public UUID getFamilyMemberId() { return familyMemberId; }
    public void setFamilyMemberId(UUID v) { this.familyMemberId = v; }
}
