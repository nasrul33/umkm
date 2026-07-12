package com.siaumkm.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/** NFR-10: pengguna aplikasi akuntansi — peran OWNER (penuh) / STAFF (terbatas). */
@Entity
@Table(name = "app_user")
public class AppUser {

    public enum PeranPengguna { OWNER, STAFF }

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    // BCrypt — TIDAK PERNAH keluar dari server: jangan tambahkan getter ini
    // ke DTO/respons API mana pun.
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private String nama;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PeranPengguna peran = PeranPengguna.STAFF;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public PeranPengguna getPeran() { return peran; }
    public void setPeran(PeranPengguna v) { this.peran = v; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean v) { this.isActive = v; }
}
