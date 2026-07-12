package com.siaumkm.profile;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * SRS-A-06: warna/logo/font per klien, dikonsumsi frontend sebagai CSS variable
 * runtime (lihat premium-ux-designer agent) — jangan hardcode warna brand di komponen Vue.
 */
@Entity
@Table(name = "brand_theme")
public class BrandTheme {
    @Id @GeneratedValue private UUID id;
    @Column(name = "business_profile_id", nullable = false) private UUID businessProfileId;
    @Column(name = "warna_primer") private String warnaPrimer = "#1F2A44";
    @Column(name = "warna_sekunder") private String warnaSekunder = "#8A6D3B";
    @Column(name = "logo_url") private String logoUrl;
    @Column(name = "font_display") private String fontDisplay;
    @Column(name = "font_body") private String fontBody;

    public UUID getId() { return id; }
    public String getWarnaPrimer() { return warnaPrimer; }
    public void setWarnaPrimer(String v) { this.warnaPrimer = v; }
    public String getWarnaSekunder() { return warnaSekunder; }
    public void setWarnaSekunder(String v) { this.warnaSekunder = v; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String v) { this.logoUrl = v; }
}
