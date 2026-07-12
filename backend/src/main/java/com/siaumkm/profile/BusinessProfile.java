package com.siaumkm.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-A-01/08: etalase digital publik. Endpoint yang menyajikan data ini
 * (PublicProfileController) HARUS berada di bawah /public/** — lihat SecurityConfig.
 */
@Entity
@Table(name = "business_profile")
public class BusinessProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    private String tagline;

    @Column(name = "deskripsi_singkat")
    private String deskripsiSingkat;

    @Column(name = "cerita_usaha")
    private String ceritaUsaha;

    @Column(name = "domain_kustom", unique = true)
    private String domainKustom;

    @Column(name = "ssl_status")
    private String sslStatus = "PENDING";

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getTagline() { return tagline; }
    public void setTagline(String v) { this.tagline = v; }
    public String getDeskripsiSingkat() { return deskripsiSingkat; }
    public void setDeskripsiSingkat(String v) { this.deskripsiSingkat = v; }
    public String getCeritaUsaha() { return ceritaUsaha; }
    public void setCeritaUsaha(String v) { this.ceritaUsaha = v; }
    public String getDomainKustom() { return domainKustom; }
    public void setDomainKustom(String v) { this.domainKustom = v; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String v) { this.whatsappNumber = v; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public void setBusinessEntityId(UUID v) { this.businessEntityId = v; }
}
