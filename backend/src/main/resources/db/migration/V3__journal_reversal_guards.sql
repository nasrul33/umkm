-- ============================================================================
-- V3 — Penjaga integritas jurnal pembalik (BR-B3-07, konsultasi
-- accounting-engine-architect)
-- ============================================================================

-- Satu jurnal POSTED hanya boleh dibalik SEKALI. Cek di service layer ada
-- untuk pesan ramah; index inilah penegakan sesungguhnya (race-safe).
CREATE UNIQUE INDEX uq_journal_entry_reversal_of
    ON journal_entry(reversal_of_id) WHERE reversal_of_id IS NOT NULL;

-- Celah BR-B3-07: trigger V1 hanya melindungi journal_entry — baris jurnal
-- (nilai debit/kredit!) masih bisa di-UPDATE/DELETE langsung. Trigger
-- pendamping ini menutupnya: baris milik jurnal POSTED ikut immutable.
CREATE OR REPLACE FUNCTION prevent_update_line_of_posted_journal() RETURNS TRIGGER AS $$
DECLARE
    v_status status_jurnal;
    v_nomor  VARCHAR(30);
BEGIN
    SELECT status, nomor_jurnal INTO v_status, v_nomor
        FROM journal_entry
        WHERE id = COALESCE(OLD.journal_entry_id, NEW.journal_entry_id);

    IF v_status = 'POSTED' THEN
        RAISE EXCEPTION 'Baris jurnal % berstatus POSTED tidak dapat diubah/dihapus. Gunakan jurnal pembalik (reversal_of_id).', v_nomor;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_line_posted_journal
    BEFORE UPDATE OR DELETE ON journal_line
    FOR EACH ROW EXECUTE FUNCTION prevent_update_line_of_posted_journal();
