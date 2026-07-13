-- ============================================================================
-- V8 — Pengerasan tax_calculation_log utk kalkulasi PPh Final bulanan
-- (BR-B5-01, konsultasi tax-compliance-specialist / PMK 164/2023)
-- ============================================================================

-- DPP bulan berjalan (bagian omzet di atas pengecualian Rp500 jt utk OP) —
-- disimpan eksplisit agar auditable, terpisah dari omzet bruto masa.
ALTER TABLE tax_calculation_log
    ADD COLUMN omzet_kena_pajak NUMERIC(19,2) NOT NULL DEFAULT 0;

-- Kalkulasi ulang = INSERT baris baru (append-only); "yang berlaku" = baris
-- calculated_at terbaru per masa. billing_request menunjuk baris historis
-- via FK — baris lama tidak boleh berubah.
CREATE INDEX idx_tax_calc_log_masa
    ON tax_calculation_log (business_entity_id, periode_tahun, periode_bulan, calculated_at DESC);

-- Aturan Emas #4: nilai pajak — append-only ditegakkan database, bukan Java.
CREATE OR REPLACE FUNCTION prevent_update_tax_calculation_log() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'tax_calculation_log bersifat append-only — kalkulasi ulang = baris baru, bukan UPDATE/DELETE.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_tax_calculation_log
    BEFORE UPDATE OR DELETE ON tax_calculation_log
    FOR EACH ROW EXECUTE FUNCTION prevent_update_tax_calculation_log();

CREATE TRIGGER trg_audit_tax_calculation_log
    AFTER INSERT ON tax_calculation_log
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();
