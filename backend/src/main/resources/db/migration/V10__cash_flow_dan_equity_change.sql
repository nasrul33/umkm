-- ============================================================================
-- V10 — Laporan Arus Kas (BR-B4-03, metode LANGSUNG) & Perubahan Modal
-- (BR-B4-04, SAK EMKM) — desain accounting-engine-architect.
-- ============================================================================

-- Klasifikasi arus kas melekat pada SIFAT AKUN (bukan template): akun baru
-- buatan user otomatis ikut aturan. Akun kas/setara kas ber-flag sendiri —
-- JANGAN hardcode kode 1000/1100 (tidak tahan e-wallet ber-CoA sendiri).
CREATE TYPE kategori_arus_kas AS ENUM ('OPERASI', 'INVESTASI', 'PENDANAAN');

ALTER TABLE chart_of_account
    ADD COLUMN is_kas_setara_kas  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cash_flow_category kategori_arus_kas;

-- Sub-akun Prive: arah debit/kredit pada 3000 tunggal TIDAK cukup — jurnal
-- pembalik setoran modal akan salah tampil sebagai prive. 3100 memisahkannya.
INSERT INTO chart_of_account (kode_akun, nama_akun, tipe, parent_id)
SELECT '3100', 'Prive (Pengambilan Pemilik)', 'MODAL', id
FROM chart_of_account WHERE kode_akun = '3000';

UPDATE chart_of_account SET is_kas_setara_kas = TRUE  WHERE kode_akun IN ('1000', '1100');
UPDATE chart_of_account SET cash_flow_category = 'OPERASI'
    WHERE kode_akun IN ('1200', '1300', '2000', '4000', '5000', '5100');
UPDATE chart_of_account SET cash_flow_category = 'INVESTASI' WHERE kode_akun = '1500';
UPDATE chart_of_account SET cash_flow_category = 'PENDANAAN' WHERE kode_akun IN ('3000', '3100');

-- Setiap akun wajib salah satu: kas/setara kas ATAU punya kategori arus kas.
ALTER TABLE chart_of_account
    ADD CONSTRAINT chk_cash_flow_category CHECK (
        (is_kas_setara_kas AND cash_flow_category IS NULL)
        OR (NOT is_kas_setara_kas AND cash_flow_category IS NOT NULL));

-- ----------------------------------------------------------------------------
-- Arus kas metode langsung, teknik "akun lawan": utk tiap jurnal POSTED yang
-- menyentuh akun kas, baris NON-kas-nya diklasifikasikan; kredit-debit baris
-- non-kas = kas masuk bersih (eksak karena jurnal balance). Penjualan kredit
-- (tanpa baris kas) tidak muncul — benar, tidak ada kas bergerak; transfer
-- antar akun kas ter-eksklusi otomatis (semua barisnya kas).
-- periode DATE murni — pelajaran bug timezone V4.
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW vw_cash_flow AS
SELECT (date_trunc('month', je.tanggal_transaksi))::date AS periode,
       coa.cash_flow_category                            AS kategori,
       coa.kode_akun,
       coa.nama_akun,
       SUM(jl.kredit)            AS arus_masuk,
       SUM(jl.debit)             AS arus_keluar,
       SUM(jl.kredit - jl.debit) AS arus_bersih
FROM journal_entry je
JOIN journal_line jl      ON jl.journal_entry_id = je.id
JOIN chart_of_account coa ON coa.id = jl.chart_of_account_id
WHERE je.status = 'POSTED'
  AND coa.is_kas_setara_kas = FALSE
  AND EXISTS (SELECT 1 FROM journal_line k
              JOIN chart_of_account kc ON kc.id = k.chart_of_account_id
              WHERE k.journal_entry_id = je.id AND kc.is_kas_setara_kas)
GROUP BY 1, 2, 3, 4;

-- Perubahan modal (SAK EMKM): setoran (3000) & prive (3100) per bulan;
-- komponen laba diambil dari vw_income_statement (satu definisi laba),
-- modal awal kumulatif dirakit di FinancialReportService (BigDecimal).
CREATE MATERIALIZED VIEW vw_equity_change AS
SELECT (date_trunc('month', je.tanggal_transaksi))::date AS periode,
       SUM(CASE WHEN coa.kode_akun = '3000' THEN jl.kredit - jl.debit ELSE 0 END) AS setoran_bersih,
       SUM(CASE WHEN coa.kode_akun = '3100' THEN jl.debit - jl.kredit ELSE 0 END) AS prive_bersih
FROM journal_entry je
JOIN journal_line jl      ON jl.journal_entry_id = je.id
JOIN chart_of_account coa ON coa.id = jl.chart_of_account_id
WHERE je.status = 'POSTED' AND coa.tipe = 'MODAL'
GROUP BY 1;
