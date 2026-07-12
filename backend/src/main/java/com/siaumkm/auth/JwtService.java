package com.siaumkm.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Token akses stateless untuk SPA (lihat SecurityConfig). Secret per instalasi
 * klien via env JWT_SECRET — single-tenant, tidak ada secret bersama antar klien.
 */
@Service
public class JwtService {

    public record TokenPayload(UUID userId, AppUser.PeranPengguna peran) {}

    private final SecretKey key;
    private final Duration masaBerlaku;

    public JwtService(@Value("${siaumkm.security.jwt-secret}") String secret,
                      @Value("${siaumkm.security.jwt-expiry-minutes:480}") long expiryMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET wajib diisi minimal 32 karakter — set di .env per instalasi klien.");
        }
        if (secret.startsWith("changeme") || secret.startsWith("dev-only")) {
            throw new IllegalStateException(
                "JWT_SECRET masih nilai placeholder — generate nilai acak unik per klien, "
                + "mis. `openssl rand -base64 48`.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.masaBerlaku = Duration.ofMinutes(expiryMinutes);
    }

    public String terbitkan(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("peran", user.getPeran().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(masaBerlaku)))
                .signWith(key)
                .compact();
    }

    /** Kembalikan payload bila token valid & belum kedaluwarsa; empty bila tidak. */
    public Optional<TokenPayload> verifikasi(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new TokenPayload(
                    UUID.fromString(claims.getSubject()),
                    AppUser.PeranPengguna.valueOf(claims.get("peran", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
