-- ============================================================================
-- V9 — Fondasi integrasi e-billing PJAP (BR-B5-07 / SRS-B5-05, konsultasi
-- api-integration-engineer)
-- ============================================================================

-- Durasi DRAFT -> terminal terukur; diperbarui aplikasi pada tiap transisi.
ALTER TABLE billing_request
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Idempotensi lapisan database (penjaga terakhir): satu billing_request
-- "hidup" per baris kalkulasi; FAILED boleh dicoba ulang sebagai baris baru.
-- Saat kelak menambah status EXPIRED/void, perluas predikat — jangan longgarkan.
CREATE UNIQUE INDEX uq_billing_request_active_per_log
    ON billing_request(tax_calculation_log_id) WHERE status <> 'FAILED';

-- Artefak keuangan dengan transisi status — wajib audit hash-chain (Aturan
-- Emas #4). Payload dipersist SUDAH dalam bentuk masked (NPWP tidak utuh),
-- sehingga changed_data di audit_log ikut bersih.
CREATE TRIGGER trg_audit_billing_request
    AFTER INSERT OR UPDATE ON billing_request
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();

-- KAP/KJS adalah parameter regulasi = DATA (Aturan Emas #3), bukan konstanta
-- Java. PPh Final UMKM setor sendiri: KAP 411128, KJS 420.
ALTER TABLE tax_rule ADD COLUMN kode_akun_pajak VARCHAR(6);
ALTER TABLE tax_rule ADD COLUMN kode_jenis_setoran VARCHAR(3);

UPDATE tax_rule
SET kode_akun_pajak = '411128', kode_jenis_setoran = '420'
WHERE kode_aturan LIKE 'PPH-FINAL-UMKM%';
