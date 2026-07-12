package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** SRS-B2-03: master pelanggan, dirujuk invoice (B3). */
@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String nama;
    @Column(name = "kontak_telepon") private String kontakTelepon;
    private String alamat;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public String getKontakTelepon() { return kontakTelepon; }
    public void setKontakTelepon(String v) { this.kontakTelepon = v; }
}
