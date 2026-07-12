-- ============================================================================
-- V4 — Perbaikan bug timezone "Laba Bulan Ini" (SRS-B4-08)
-- date_trunc('month', <date>) menghasilkan TIMESTAMPTZ, sehingga nilai periode
-- yang tersimpan saat REFRESH tergantung timezone sesi penulis, dan perbandingan
-- WHERE periode = date_trunc('month', CURRENT_DATE) di sesi backend (Asia/Jakarta)
-- meleset dari baris yang di-refresh sesi lain (UTC) — laba tampil 0.
-- Solusi: periode bertipe DATE murni, bebas timezone.
-- ============================================================================

DROP MATERIALIZED VIEW vw_income_statement;

CREATE MATERIALIZED VIEW vw_income_statement AS
SELECT coa.tipe,
       coa.kode_akun,
       coa.nama_akun,
       (date_trunc('month', je.tanggal_transaksi))::date AS periode,
       COALESCE(SUM(jl.kredit) - SUM(jl.debit), 0) AS saldo -- pendapatan positif, beban dibalik di aplikasi
FROM chart_of_account coa
JOIN journal_line jl ON jl.chart_of_account_id = coa.id
JOIN journal_entry je ON je.id = jl.journal_entry_id AND je.status = 'POSTED'
WHERE coa.tipe IN ('PENDAPATAN', 'BEBAN')
GROUP BY coa.tipe, coa.kode_akun, coa.nama_akun, date_trunc('month', je.tanggal_transaksi);
