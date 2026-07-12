-- ============================================================================
-- V5 — Enkripsi application-layer kolom sensitif (NFR-05)
-- Kolom NPWP/NIK kini menyimpan ciphertext AES-256-GCM berprefix 'enc:v1:'
-- (~67 karakter), bukan plaintext 16 digit:
-- 1. Lebarkan kolom.
-- 2. CHECK format 16-digit di database DIHAPUS — tidak mungkin memvalidasi
--    ciphertext; validasi format pindah SELURUHNYA ke @Pattern pada entity
--    (berjalan atas plaintext sebelum enkripsi).
-- ============================================================================

ALTER TABLE business_entity DROP CONSTRAINT chk_npwp_format;
ALTER TABLE business_entity DROP CONSTRAINT chk_nik_format;

ALTER TABLE business_entity ALTER COLUMN npwp        TYPE VARCHAR(128);
ALTER TABLE business_entity ALTER COLUMN nik_pemilik TYPE VARCHAR(128);
ALTER TABLE family_member   ALTER COLUMN nik         TYPE VARCHAR(128);
ALTER TABLE family_member   ALTER COLUMN npwp        TYPE VARCHAR(128);
ALTER TABLE related_entity  ALTER COLUMN npwp_entitas_lain TYPE VARCHAR(128);
ALTER TABLE employee        ALTER COLUMN npwp        TYPE VARCHAR(128);

COMMENT ON COLUMN business_entity.npwp IS 'Ciphertext AES-256-GCM (enc:v1:...) — NFR-05. Format 16 digit divalidasi di application layer sebelum enkripsi.';
