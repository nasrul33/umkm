package com.siaumkm.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * NFR-05: enkripsi application-layer untuk data sensitif (NPWP, NIK, no. rekening)
 * — disk-level encryption saja tidak cukup. AES-256-GCM, IV acak per nilai,
 * ciphertext berprefix "enc:v1:" agar terdeteksi dan versi skema kunci bisa
 * dirotasi kelak.
 *
 * Kunci per instalasi klien via env FIELD_ENCRYPTION_KEY (base64 dari 32 byte
 * acak, mis. `openssl rand -base64 32`) — single-tenant, tidak ada kunci
 * bersama antar klien. Konsekuensi yang DISENGAJA: kolom terenkripsi tidak
 * bisa di-query LIKE/equality di SQL; pencarian by-NPWP harus lewat aplikasi.
 */
@Service
public class FieldEncryptionService {

    static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;   // rekomendasi NIST utk GCM
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptionService(
            @Value("${siaumkm.security.field-encryption-key}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()
                || encodedKey.startsWith("changeme") || encodedKey.startsWith("dev-only")) {
            throw new IllegalStateException(
                "FIELD_ENCRYPTION_KEY wajib diisi nilai unik per instalasi klien — "
                + "generate dengan `openssl rand -base64 32`.");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "FIELD_ENCRYPTION_KEY bukan base64 valid — generate dengan `openssl rand -base64 32`.", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException(
                "FIELD_ENCRYPTION_KEY harus 32 byte (AES-256), didapat " + raw.length + " byte.");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Gagal mengenkripsi data sensitif", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) return null;
        if (!stored.startsWith(PREFIX)) {
            // Nilai legacy pra-enkripsi (instalasi lama) — dikembalikan apa adanya
            // agar data tetap terbaca; akan terenkripsi saat baris disimpan ulang.
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] pt = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Tag GCM tidak cocok = data diubah di luar aplikasi atau kunci salah —
            // JANGAN kembalikan ciphertext mentah, gagalkan dengan jelas.
            throw new IllegalStateException(
                "Gagal mendekripsi data sensitif — kemungkinan data diubah di luar aplikasi atau FIELD_ENCRYPTION_KEY salah.", e);
        }
    }
}
