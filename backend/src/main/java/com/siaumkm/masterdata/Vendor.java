package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** SRS-B2-04: master pemasok/vendor. */
@Entity
@Table(name = "vendor")
public class Vendor {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String nama;
    @Column(name = "kontak_telepon") private String kontakTelepon;
    private String alamat;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
}
