package com.siaumkm.profile;

import jakarta.persistence.*;
import java.util.UUID;

/** SRS-A-04: galeri foto usaha. */
@Entity
@Table(name = "gallery_item")
public class GalleryItem {
    @Id @GeneratedValue private UUID id;
    @Column(name = "business_profile_id", nullable = false) private UUID businessProfileId;
    @Column(name = "foto_url", nullable = false) private String fotoUrl;
    private String caption;
    @Column(name = "urutan_tampil") private Integer urutanTampil = 0;

    public UUID getId() { return id; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String v) { this.fotoUrl = v; }
    public String getCaption() { return caption; }
    public void setCaption(String v) { this.caption = v; }
}
