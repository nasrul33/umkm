-- ============================================================================
-- V6 — Fondasi agregasi omzet Pasal 58 PP 20/2026 (SRS-B5-02, BR-B5-03/04)
-- Hasil konsultasi tax-compliance-specialist.
-- ============================================================================

-- Peredaran bruto "tertentu" TIDAK mencakup penghasilan yang dikenai PPh final
-- tersendiri (bunga, sewa 4(2)) atau pekerjaan bebas (PMK 168/2023). Filter
-- agregasi memakai kolom ini — BUKAN prefix kode akun hardcode. Akun pendapatan
-- non-usaha yang dibuat kelak wajib di-set FALSE.
ALTER TABLE chart_of_account ADD COLUMN is_omzet_usaha BOOLEAN NOT NULL DEFAULT TRUE;

-- Auditabilitas: usaha terkait milik siapa dalam lingkaran keluarga Pasal 58
-- (satuan agregasi adalah ENTITAS USAHA — kontribusi omzet keluarga selalu
-- lewat related_entity, bukan kolom omzet di family_member).
ALTER TABLE related_entity
    ADD COLUMN family_member_id UUID REFERENCES family_member(id);

-- Aturan Emas #4: omzet_tahunan_diketahui adalah INPUT MANUAL yang memengaruhi
-- angka pajak — perubahannya wajib berjejak di audit hash-chain. Hasil agregasi
-- juga diaudit agar riwayat rekalkulasi terekam tanpa tabel riwayat terpisah.
CREATE TRIGGER trg_audit_related_entity
    AFTER INSERT OR UPDATE ON related_entity
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();

CREATE TRIGGER trg_audit_aggregated_omzet
    AFTER INSERT OR UPDATE ON aggregated_omzet
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();
