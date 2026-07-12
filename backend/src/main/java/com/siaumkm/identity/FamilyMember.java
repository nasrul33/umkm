package com.siaumkm.identity;

import com.siaumkm.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-B1-02: registri lingkaran keluarga Pasal 58 PP 20/2026 (pasangan, anak
 * belum dewasa). Kontribusi omzet keluarga TIDAK dicatat di sini — setiap
 * usaha milik anggota keluarga didaftarkan sebagai RelatedEntity.
 */
@Entity
@Table(name = "family_member")
public class FamilyMember {

    @Id @GeneratedValue private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(nullable = false) private String nama;

    // NFR-05: ciphertext AES-256-GCM; format divalidasi atas plaintext.
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK harus 16 digit")
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 128) private String nik;

    @Pattern(regexp = "^[0-9]{16}$", message = "NPWP harus 16 digit (format Coretax)")
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 128) private String npwp;

    @Column(nullable = false, length = 50) private String hubungan; // 'Pasangan', 'Anak'

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public void setBusinessEntityId(UUID v) { this.businessEntityId = v; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public String getNik() { return nik; }
    public void setNik(String v) { this.nik = v; }
    public String getNpwp() { return npwp; }
    public void setNpwp(String v) { this.npwp = v; }
    public String getHubungan() { return hubungan; }
    public void setHubungan(String v) { this.hubungan = v; }
}
