-- ============================================================================
-- SIA-UMKM Premium — Skema Database PostgreSQL 18
-- Turunan dari SRS-UMKM-01 v1.0 Bagian 3 (kolom "Entitas/Tabel" per modul)
-- Model deployment: single-tenant per klien — TIDAK ada kolom tenant_id.
-- Aturan emas: uang = NUMERIC (dimanipulasi sebagai BigDecimal di Java),
-- jurnal POSTED immutable via trigger, audit_log hash-chain, tax_rule effective-dated.
-- ============================================================================

-- Tidak perlu ekstensi tambahan: PostgreSQL 18 menyediakan gen_random_uuid()
-- dan sha256() sebagai fungsi core sejak versi 13+, jadi pgcrypto tidak diperlukan
-- untuk fondasi UUID PK dan hash-chain audit trail di skema ini.

-- ============================================================================
-- ENUM TYPES
-- ============================================================================
CREATE TYPE bentuk_badan_usaha AS ENUM ('OP', 'PT_PERORANGAN', 'CV', 'FIRMA', 'KOPERASI', 'PT_BIASA');
CREATE TYPE metode_pembayaran   AS ENUM ('CASH', 'TRANSFER', 'QRIS', 'RECEIVABLE', 'PAYABLE');
CREATE TYPE status_jurnal       AS ENUM ('DRAFT', 'POSTED');
CREATE TYPE tipe_akun            AS ENUM ('ASET', 'KEWAJIBAN', 'MODAL', 'PENDAPATAN', 'BEBAN');
CREATE TYPE perilaku_biaya       AS ENUM ('FIXED', 'VARIABLE');
CREATE TYPE peran_pengguna       AS ENUM ('OWNER', 'STAFF');
CREATE TYPE status_billing       AS ENUM ('DRAFT', 'REQUESTED', 'ISSUED', 'PENDING_MANUAL', 'FAILED');
CREATE TYPE status_transisi_pajak AS ENUM ('NONE', 'MONITORING', 'TRANSITION_REQUIRED', 'TRANSITIONED');

-- ============================================================================
-- MODUL B1 — IDENTITAS USAHA  (SRS-B1-01..03)
-- ============================================================================
CREATE TABLE business_entity (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama_usaha          VARCHAR(200) NOT NULL,
    nama_pemilik        VARCHAR(200) NOT NULL,
    npwp                VARCHAR(16)  UNIQUE,              -- format 16 digit Coretax
    nik_pemilik         VARCHAR(16)  NOT NULL,
    nib                 VARCHAR(20),
    bentuk_badan        bentuk_badan_usaha NOT NULL,
    klu_code            VARCHAR(10),
    status_pkp          BOOLEAN NOT NULL DEFAULT FALSE,
    tanggal_pengukuhan_pkp DATE,
    alamat_usaha        TEXT,
    alamat_domisili     TEXT,
    kontak_telepon      VARCHAR(20),
    kontak_email        VARCHAR(150),
    tahun_berdiri       INTEGER,
    status_transisi_pajak status_transisi_pajak NOT NULL DEFAULT 'NONE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_npwp_format CHECK (npwp IS NULL OR npwp ~ '^[0-9]{16}$'),
    CONSTRAINT chk_nik_format CHECK (nik_pemilik ~ '^[0-9]{16}$')
);
COMMENT ON TABLE business_entity IS 'SRS-B1-01: fondasi identitas legal usaha, penentu seluruh logika perpajakan.';

-- Untuk agregasi omzet lintas anggota keluarga (Pasal 58 PP 20/2026) — SRS-B1-02
CREATE TABLE family_member (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id) ON DELETE CASCADE,
    nama            VARCHAR(200) NOT NULL,
    nik             VARCHAR(16),
    npwp            VARCHAR(16),
    hubungan        VARCHAR(50) NOT NULL, -- contoh: 'Pasangan', 'Anak'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Entitas usaha lain milik pemilik yang sama — SRS-B1-02
CREATE TABLE related_entity (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id) ON DELETE CASCADE,
    nama_entitas_lain   VARCHAR(200) NOT NULL,
    npwp_entitas_lain   VARCHAR(16),
    omzet_tahunan_diketahui NUMERIC(19,2), -- input manual/estimasi utk agregasi
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE onboarding_progress (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID REFERENCES business_entity(id) ON DELETE CASCADE,
    step_terakhir       VARCHAR(50) NOT NULL,
    payload_draf        JSONB,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- MODUL A — HALAMAN PROFIL BISNIS PUBLIK  (SRS-A-01..06)
-- ============================================================================
CREATE TABLE business_profile (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id) ON DELETE CASCADE,
    tagline         VARCHAR(300),
    deskripsi_singkat TEXT,
    cerita_usaha    TEXT,
    domain_kustom   VARCHAR(255) UNIQUE,
    ssl_status      VARCHAR(20) DEFAULT 'PENDING',
    whatsapp_number VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE brand_theme (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_profile_id UUID NOT NULL REFERENCES business_profile(id) ON DELETE CASCADE,
    warna_primer        VARCHAR(7)  DEFAULT '#1F2A44',
    warna_sekunder       VARCHAR(7)  DEFAULT '#8A6D3B',
    logo_url             VARCHAR(500),
    font_display          VARCHAR(100),
    font_body             VARCHAR(100)
);

CREATE TABLE product_showcase (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_profile_id UUID NOT NULL REFERENCES business_profile(id) ON DELETE CASCADE,
    nama                VARCHAR(200) NOT NULL,
    deskripsi           TEXT,
    harga_ditampilkan   NUMERIC(19,2),
    tampilkan_harga     BOOLEAN NOT NULL DEFAULT TRUE,
    foto_url            VARCHAR(500),
    urutan_tampil       INTEGER DEFAULT 0
);

CREATE TABLE gallery_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_profile_id UUID NOT NULL REFERENCES business_profile(id) ON DELETE CASCADE,
    foto_url            VARCHAR(500) NOT NULL,
    caption             VARCHAR(300),
    urutan_tampil       INTEGER DEFAULT 0
);

CREATE TABLE testimonial (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_profile_id UUID NOT NULL REFERENCES business_profile(id) ON DELETE CASCADE,
    nama_pelanggan       VARCHAR(200) NOT NULL,
    isi_testimoni         TEXT NOT NULL,
    ditampilkan            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- MODUL B2 — MASTER DATA  (SRS-B2-01..04)
-- ============================================================================
CREATE TABLE chart_of_account (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kode_akun       VARCHAR(20) NOT NULL UNIQUE,
    nama_akun       VARCHAR(200) NOT NULL,
    tipe            tipe_akun NOT NULL,
    cost_behavior   perilaku_biaya,           -- diisi khusus akun beban, utk BR-B6-06
    parent_id       UUID REFERENCES chart_of_account(id),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);
COMMENT ON TABLE chart_of_account IS 'SRS-B2-01: seed standar UMKM via Flyway saat provisioning klien baru.';

CREATE TABLE product (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama            VARCHAR(200) NOT NULL,
    kategori        VARCHAR(100),
    satuan          VARCHAR(20) DEFAULT 'pcs',
    harga_jual      NUMERIC(19,2) NOT NULL DEFAULT 0,
    hpp_dasar       NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE customer (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama            VARCHAR(200) NOT NULL,
    kontak_telepon  VARCHAR(20),
    alamat          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vendor (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama            VARCHAR(200) NOT NULL,
    kontak_telepon  VARCHAR(20),
    alamat          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cash_account (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama            VARCHAR(100) NOT NULL,     -- contoh: 'Kas Tunai', 'BCA 123456', 'GoPay Merchant'
    tipe            VARCHAR(30) NOT NULL,      -- CASH / BANK / EWALLET
    chart_of_account_id UUID NOT NULL REFERENCES chart_of_account(id),
    saldo_awal      NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE employee (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama            VARCHAR(200) NOT NULL,
    npwp            VARCHAR(16),
    gaji_bulanan    NUMERIC(19,2),
    tanggal_mulai   DATE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE fixed_asset (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama                VARCHAR(200) NOT NULL,
    tanggal_perolehan   DATE NOT NULL,
    nilai_perolehan     NUMERIC(19,2) NOT NULL,
    metode_penyusutan   VARCHAR(30) NOT NULL DEFAULT 'STRAIGHT_LINE',
    umur_manfaat_bulan  INTEGER NOT NULL,
    nilai_residu        NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- MODUL B3 — TRANSAKSI HARIAN  (SRS-B3-01..05)
-- ============================================================================
CREATE TABLE transaction_template_rule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kode_template   VARCHAR(50) NOT NULL UNIQUE, -- contoh: 'JUAL_BARANG_JASA', 'TERIMA_PEMBAYARAN'
    label_awam      VARCHAR(200) NOT NULL,        -- contoh: 'Jual Barang/Jasa'
    deskripsi       TEXT,
    debit_account_rule  JSONB NOT NULL,  -- aturan pemetaan akun debit (strategy pattern config)
    kredit_account_rule JSONB NOT NULL
);
COMMENT ON TABLE transaction_template_rule IS 'SRS-B3-01: setiap wizard bahasa awam punya JournalRuleMapper sendiri.';

CREATE TABLE journal_entry (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nomor_jurnal        VARCHAR(30) NOT NULL UNIQUE,
    tanggal_transaksi   DATE NOT NULL,
    keterangan          TEXT,
    template_rule_id    UUID REFERENCES transaction_template_rule(id),
    metode_pembayaran   metode_pembayaran,
    status              status_jurnal NOT NULL DEFAULT 'DRAFT',
    reversal_of_id       UUID REFERENCES journal_entry(id), -- utk koreksi via jurnal pembalik
    created_by            UUID NOT NULL,  -- FK ke user aplikasi (tabel app_user, dibuat di modul auth)
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    posted_at              TIMESTAMPTZ
);

CREATE TABLE journal_line (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entry(id) ON DELETE CASCADE,
    chart_of_account_id UUID NOT NULL REFERENCES chart_of_account(id),
    debit           NUMERIC(19,2) NOT NULL DEFAULT 0,
    kredit          NUMERIC(19,2) NOT NULL DEFAULT 0,
    product_id      UUID REFERENCES product(id),  -- nullable, utk kalkulasi margin per produk (B6)
    CONSTRAINT chk_debit_kredit_exclusive CHECK (
        (debit > 0 AND kredit = 0) OR (kredit > 0 AND debit = 0)
    )
);

CREATE TABLE recurring_schedule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_rule_id UUID NOT NULL REFERENCES transaction_template_rule(id),
    cron_expression  VARCHAR(50) NOT NULL,
    payload_template JSONB NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    next_run_at       TIMESTAMPTZ
);

CREATE TABLE attachment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID REFERENCES journal_entry(id) ON DELETE CASCADE,
    file_path        VARCHAR(500) NOT NULL, -- path terenkripsi di object storage
    file_type        VARCHAR(50),
    uploaded_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nomor_invoice    VARCHAR(30) NOT NULL UNIQUE,
    customer_id       UUID REFERENCES customer(id),
    journal_entry_id  UUID REFERENCES journal_entry(id),
    tanggal_invoice   DATE NOT NULL,
    total             NUMERIC(19,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
);

CREATE TABLE invoice_line (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id       UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    product_id        UUID REFERENCES product(id),
    qty                NUMERIC(12,2) NOT NULL DEFAULT 1,
    harga_satuan       NUMERIC(19,2) NOT NULL,
    subtotal            NUMERIC(19,2) NOT NULL
);

-- ---- Trigger: jurnal POSTED immutable (SRS-B3-04) ----
CREATE OR REPLACE FUNCTION prevent_update_posted_journal() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'POSTED' THEN
        RAISE EXCEPTION 'Jurnal % berstatus POSTED tidak dapat diubah/dihapus. Gunakan jurnal pembalik (reversal_of_id).', OLD.nomor_jurnal;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_posted_journal
    BEFORE UPDATE OR DELETE ON journal_entry
    FOR EACH ROW EXECUTE FUNCTION prevent_update_posted_journal();

-- ============================================================================
-- AUDIT TRAIL — hash-chain immutable (NFR-04, pola identik SIA-PDAM)
-- ============================================================================
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    table_name      VARCHAR(100) NOT NULL,
    row_id           UUID NOT NULL,
    action           VARCHAR(10) NOT NULL,  -- INSERT / UPDATE
    changed_data     JSONB NOT NULL,
    changed_by        UUID,
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    prev_hash          VARCHAR(64),
    row_hash            VARCHAR(64) NOT NULL
);

-- Larang UPDATE apa pun pada audit_log — append-only
CREATE OR REPLACE FUNCTION prevent_update_audit_log() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log bersifat append-only, UPDATE/DELETE tidak diizinkan.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_audit_log
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_update_audit_log();

-- Fungsi generik penulis hash-chain, dipanggil oleh trigger AFTER INSERT/UPDATE
-- pada tabel transaksi keuangan (journal_entry, journal_line, invoice, dst).
CREATE OR REPLACE FUNCTION write_audit_log() RETURNS TRIGGER AS $$
DECLARE
    v_prev_hash VARCHAR(64);
    v_row_hash   VARCHAR(64);
    v_payload     JSONB;
BEGIN
    SELECT row_hash INTO v_prev_hash FROM audit_log
        WHERE table_name = TG_TABLE_NAME ORDER BY id DESC LIMIT 1;

    v_payload := to_jsonb(NEW);
    v_row_hash := encode(sha256((coalesce(v_prev_hash, '') || v_payload::text)::bytea), 'hex');

    INSERT INTO audit_log (table_name, row_id, action, changed_data, prev_hash, row_hash)
    VALUES (TG_TABLE_NAME, NEW.id, TG_OP, v_payload, v_prev_hash, v_row_hash);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_journal_entry
    AFTER INSERT OR UPDATE ON journal_entry
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();

CREATE TRIGGER trg_audit_journal_line
    AFTER INSERT OR UPDATE ON journal_line
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();

CREATE TRIGGER trg_audit_invoice
    AFTER INSERT OR UPDATE ON invoice
    FOR EACH ROW EXECUTE FUNCTION write_audit_log();

-- ============================================================================
-- MODUL B5 — PERPAJAKAN  (SRS-B5-01..07)
-- ============================================================================
-- Parameter pajak effective-dated — TIDAK PERNAH hardcode tarif di Java.
CREATE TABLE tax_rule (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kode_aturan         VARCHAR(50) NOT NULL,   -- contoh: 'PPH-FINAL-UMKM-OP'
    deskripsi           TEXT,
    tarif_persen         NUMERIC(6,4) NOT NULL,  -- contoh: 0.5000 utk 0,5%
    ambang_bawah          NUMERIC(19,2),           -- contoh: 500000000 (pengecualian omzet)
    ambang_atas            NUMERIC(19,2),           -- contoh: 4800000000
    bentuk_badan_berlaku  bentuk_badan_usaha[],     -- array bentuk badan yang berhak
    berlaku_dari            DATE NOT NULL,
    berlaku_sampai          DATE,                    -- NULL = masih berlaku
    regulasi_acuan           VARCHAR(200) NOT NULL,  -- contoh: 'PP 20/2026'
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tax_rule_lookup ON tax_rule (kode_aturan, berlaku_dari);
COMMENT ON TABLE tax_rule IS 'SRS-B5-01: TaxCalculationEngine memilih baris aktif berdasarkan tanggal TRANSAKSI, bukan tanggal sistem.';

CREATE TABLE tax_calculation_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id),
    periode_bulan        INTEGER NOT NULL,
    periode_tahun          INTEGER NOT NULL,
    omzet_bruto              NUMERIC(19,2) NOT NULL,
    tax_rule_id                UUID NOT NULL REFERENCES tax_rule(id),
    pajak_terhitung           NUMERIC(19,2) NOT NULL,
    calculated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE aggregated_omzet (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id),
    periode_tahun         INTEGER NOT NULL,
    omzet_entitas_utama    NUMERIC(19,2) NOT NULL DEFAULT 0,
    omzet_entitas_terkait  NUMERIC(19,2) NOT NULL DEFAULT 0,
    omzet_gabungan          NUMERIC(19,2) NOT NULL DEFAULT 0,
    calculated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (business_entity_id, periode_tahun)
);
COMMENT ON TABLE aggregated_omzet IS 'SRS-B5-02: hasil OmzetAggregationJob sesuai Pasal 58 PP 20/2026 (anti firm-splitting/bunching).';

CREATE TABLE spt_draft (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id),
    jenis_spt             VARCHAR(30) NOT NULL, -- MASA / TAHUNAN
    periode                 VARCHAR(20) NOT NULL,
    payload_dokumen         JSONB NOT NULL, -- format siap impor Coretax
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    generated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE billing_request (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES business_entity(id),
    tax_calculation_log_id UUID REFERENCES tax_calculation_log(id),
    pjap_provider          VARCHAR(50) NOT NULL,
    kode_billing             VARCHAR(50),
    status                    status_billing NOT NULL DEFAULT 'DRAFT',
    request_payload           JSONB,
    response_payload           JSONB,
    requested_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE billing_request IS 'SRS-B5-05: fallback status PENDING_MANUAL bila API PJAP mitra gagal.';

CREATE TABLE tax_calendar (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nama_kewajiban   VARCHAR(200) NOT NULL,
    jatuh_tempo       DATE NOT NULL,
    reminder_h        INTEGER[] DEFAULT ARRAY[7,3,1],
    business_entity_id UUID REFERENCES business_entity(id)
);

-- ============================================================================
-- MODUL B6 — ANALISIS BIAYA  (SRS-B6-01..03)
-- ============================================================================
CREATE TABLE budget (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chart_of_account_id UUID NOT NULL REFERENCES chart_of_account(id),
    periode_bulan         INTEGER NOT NULL,
    periode_tahun           INTEGER NOT NULL,
    nilai_anggaran            NUMERIC(19,2) NOT NULL,
    UNIQUE (chart_of_account_id, periode_bulan, periode_tahun)
);

-- ============================================================================
-- VIEWS — LAPORAN KEUANGAN  (SRS-B4-01, materialized, refresh terjadwal)
-- ============================================================================
CREATE MATERIALIZED VIEW vw_balance_sheet AS
SELECT coa.tipe,
       coa.kode_akun,
       coa.nama_akun,
       COALESCE(SUM(jl.debit) - SUM(jl.kredit), 0) AS saldo
FROM chart_of_account coa
LEFT JOIN journal_line jl ON jl.chart_of_account_id = coa.id
LEFT JOIN journal_entry je ON je.id = jl.journal_entry_id AND je.status = 'POSTED'
WHERE coa.tipe IN ('ASET', 'KEWAJIBAN', 'MODAL')
GROUP BY coa.tipe, coa.kode_akun, coa.nama_akun;

CREATE MATERIALIZED VIEW vw_income_statement AS
SELECT coa.tipe,
       coa.kode_akun,
       coa.nama_akun,
       date_trunc('month', je.tanggal_transaksi) AS periode,
       COALESCE(SUM(jl.kredit) - SUM(jl.debit), 0) AS saldo -- pendapatan positif, beban dibalik di aplikasi
FROM chart_of_account coa
JOIN journal_line jl ON jl.chart_of_account_id = coa.id
JOIN journal_entry je ON je.id = jl.journal_entry_id AND je.status = 'POSTED'
WHERE coa.tipe IN ('PENDAPATAN', 'BEBAN')
GROUP BY coa.tipe, coa.kode_akun, coa.nama_akun, date_trunc('month', je.tanggal_transaksi);

CREATE MATERIALIZED VIEW vw_product_margin AS
SELECT p.id AS product_id,
       p.nama,
       p.harga_jual,
       p.hpp_dasar,
       (p.harga_jual - p.hpp_dasar) AS margin_kotor,
       CASE WHEN p.harga_jual > 0
            THEN ROUND(((p.harga_jual - p.hpp_dasar) / p.harga_jual) * 100, 2)
            ELSE 0 END AS margin_persen
FROM product p;

-- Catatan: vw_cash_flow dan vw_equity_change (SRS-B4-01) memerlukan logika
-- klasifikasi arus kas (operasi/investasi/pendanaan) yang lebih spesifik per
-- kategori akun — didesain bersama accounting-engine-architect saat implementasi,
-- bukan generic view seperti di atas.

-- ============================================================================
-- SEED DATA MINIMAL — Chart of Accounts standar UMKM
-- ============================================================================
INSERT INTO chart_of_account (kode_akun, nama_akun, tipe) VALUES
    ('1000', 'Kas', 'ASET'),
    ('1100', 'Bank', 'ASET'),
    ('1200', 'Piutang Usaha', 'ASET'),
    ('1300', 'Persediaan', 'ASET'),
    ('1500', 'Aset Tetap', 'ASET'),
    ('2000', 'Hutang Usaha', 'KEWAJIBAN'),
    ('3000', 'Modal Pemilik', 'MODAL'),
    ('4000', 'Pendapatan Usaha', 'PENDAPATAN'),
    ('5000', 'Harga Pokok Penjualan', 'BEBAN'),
    ('5100', 'Biaya Operasional', 'BEBAN');

-- Seed contoh tax_rule sesuai PP 20/2026 (WP OP & PT Perorangan, tanpa batas waktu)
INSERT INTO tax_rule (kode_aturan, deskripsi, tarif_persen, ambang_bawah, ambang_atas,
                       bentuk_badan_berlaku, berlaku_dari, regulasi_acuan) VALUES
    ('PPH-FINAL-UMKM-OP', 'PPh Final UMKM 0,5% tanpa batas waktu untuk WP OP',
     0.5, 500000000, 4800000000, ARRAY['OP']::bentuk_badan_usaha[], '2026-04-22', 'PP 20/2026'),
    ('PPH-FINAL-UMKM-PT-PERORANGAN', 'PPh Final UMKM 0,5% tanpa batas waktu untuk PT Perorangan & Koperasi',
     0.5, NULL, 4800000000, ARRAY['PT_PERORANGAN','KOPERASI']::bentuk_badan_usaha[], '2026-04-22', 'PP 20/2026');
