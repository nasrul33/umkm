-- ============================================================================
-- V2 — Tabel pengguna aplikasi (NFR-10: peran OWNER penuh / STAFF terbatas)
-- Melengkapi catatan di V1: journal_entry.created_by adalah FK ke app_user.
-- ============================================================================

CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,          -- BCrypt, TIDAK PERNAH plaintext
    nama            VARCHAR(200) NOT NULL,
    peran           peran_pengguna NOT NULL DEFAULT 'STAFF',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE app_user IS 'NFR-10: pengguna aplikasi akuntansi. User OWNER pertama dibuat saat provisioning klien (lihat client-onboarding-checklist), BUKAN di-seed dengan password default di migrasi.';

-- Tegakkan integritas pembuat jurnal di level database, bukan hanya Java.
ALTER TABLE journal_entry
    ADD CONSTRAINT fk_journal_entry_created_by
    FOREIGN KEY (created_by) REFERENCES app_user(id);
