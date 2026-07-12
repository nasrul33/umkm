package com.siaumkm.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** SRS-A-05: testimoni pelanggan, dapat dikelola admin (toggle tampil/sembunyi). */
@Entity
@Table(name = "testimonial")
public class Testimonial {
    @Id @GeneratedValue private UUID id;
    @Column(name = "business_profile_id", nullable = false) private UUID businessProfileId;
    @Column(name = "nama_pelanggan", nullable = false) private String namaPelanggan;
    @Column(name = "isi_testimoni", nullable = false) private String isiTestimoni;
    @Column(name = "ditampilkan") private boolean ditampilkan = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getNamaPelanggan() { return namaPelanggan; }
    public void setNamaPelanggan(String v) { this.namaPelanggan = v; }
    public String getIsiTestimoni() { return isiTestimoni; }
    public void setIsiTestimoni(String v) { this.isiTestimoni = v; }
    public boolean isDitampilkan() { return ditampilkan; }
    public void setDitampilkan(boolean v) { this.ditampilkan = v; }
}
