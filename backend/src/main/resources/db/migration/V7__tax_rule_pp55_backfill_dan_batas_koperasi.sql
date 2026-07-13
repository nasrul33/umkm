-- ============================================================================
-- V7 — Dua koreksi kepatuhan hasil konsultasi tax-compliance-specialist
-- (alur /add-tax-rule; entri changelog: artifacts/tax-rule-changelog-template.md)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) Batas waktu sebagai DATA, bukan kode (Aturan Emas #3):
--     batas_tahun_pajak: jumlah tahun pajak maksimal sejak terdaftar
--       (inklusif tahun terdaftar); NULL = tanpa batas waktu.
--     tahun_pajak_akhir_transisi: tahun pajak terakhir bagi WP yang terdaftar
--       SEBELUM berlaku_dari aturan (masa transisi flat); NULL = tanpa transisi.
-- ----------------------------------------------------------------------------
ALTER TABLE tax_rule ADD COLUMN batas_tahun_pajak INTEGER;
ALTER TABLE tax_rule ADD COLUMN tahun_pajak_akhir_transisi INTEGER;

-- Dasar cek batas waktu koperasi: tanggal terdaftar sebagai WP.
ALTER TABLE business_entity ADD COLUMN tanggal_terdaftar_pajak DATE;

-- ----------------------------------------------------------------------------
-- (2) Backfill rezim PP 55/2022 (20 Des 2022 s.d. 21 Apr 2026) — tanpa baris
--     ini findAturanAktif melempar utk seluruh transaksi 2023 s.d. 21 Apr 2026.
--     Periode < 2022-12-20 (rezim PP 23/2018) SENGAJA tidak dicakup, dan
--     CV/FIRMA/PT_BIASA sengaja tanpa baris (engine tidak pernah query utk
--     bentuk badan itu) — lihat limitation di changelog.
-- ----------------------------------------------------------------------------
INSERT INTO tax_rule (kode_aturan, deskripsi, tarif_persen, ambang_bawah, ambang_atas,
                       bentuk_badan_berlaku, berlaku_dari, berlaku_sampai, regulasi_acuan) VALUES
    ('PPH-FINAL-UMKM-OP',
     'PPh Final UMKM 0,5% rezim PP 55/2022 untuk WP OP (pengecualian omzet s.d. Rp500 juta sejak UU HPP)',
     0.5, 500000000, 4800000000, ARRAY['OP']::bentuk_badan_usaha[],
     '2022-12-20', '2026-04-21', 'PP 55/2022 jo. UU 7/2021 (HPP)'),
    ('PPH-FINAL-UMKM-PT-PERORANGAN',
     'PPh Final UMKM 0,5% rezim PP 55/2022 untuk PT Perorangan',
     0.5, NULL, 4800000000, ARRAY['PT_PERORANGAN']::bentuk_badan_usaha[],
     '2022-12-20', '2026-04-21', 'PP 55/2022 jo. UU 7/2021 (HPP)'),
    -- Koperasi berkode sendiri agar kontinu dgn pemisahan di rezim baru (bawah).
    -- Batas 4 tahun rezim LAMA (PP 23/2018) sengaja tidak dimodelkan — kolom
    -- batas NULL; lihat limitation di changelog.
    ('PPH-FINAL-UMKM-KOPERASI',
     'PPh Final UMKM 0,5% rezim PP 55/2022 untuk Koperasi (batas waktu rezim lama tidak dimodelkan)',
     0.5, NULL, 4800000000, ARRAY['KOPERASI']::bentuk_badan_usaha[],
     '2022-12-20', '2026-04-21', 'PP 55/2022 jo. UU 7/2021 (HPP)');

-- ----------------------------------------------------------------------------
-- (3) Koreksi seed V1 yang tidak akurat: PP 20/2026 membatasi KOPERASI maks
--     4 tahun pajak sejak terdaftar (transisi s.d. Tahun Pajak 2029 bagi yang
--     terdaftar sebelum PP berlaku) — bukan "tanpa batas waktu". Koperasi
--     dipisah ke baris sendiri; tax_rule bukan jurnal, koreksi diperbolehkan
--     (baris PP 20/2026 belum pernah dipakai kalkulasi koperasi yang benar).
-- ----------------------------------------------------------------------------
UPDATE tax_rule
SET bentuk_badan_berlaku = ARRAY['PT_PERORANGAN']::bentuk_badan_usaha[],
    deskripsi = 'PPh Final UMKM 0,5% tanpa batas waktu untuk PT Perorangan'
WHERE kode_aturan = 'PPH-FINAL-UMKM-PT-PERORANGAN'
  AND berlaku_dari = '2026-04-22';

INSERT INTO tax_rule (kode_aturan, deskripsi, tarif_persen, ambang_bawah, ambang_atas,
                       bentuk_badan_berlaku, berlaku_dari, regulasi_acuan,
                       batas_tahun_pajak, tahun_pajak_akhir_transisi) VALUES
    ('PPH-FINAL-UMKM-KOPERASI',
     'PPh Final UMKM 0,5% untuk Koperasi, maksimal 4 tahun pajak sejak terdaftar (transisi s.d. Tahun Pajak 2029 bagi yang terdaftar sebelum 22 Apr 2026)',
     0.5, NULL, 4800000000, ARRAY['KOPERASI']::bentuk_badan_usaha[],
     '2026-04-22', 'PP 20/2026', 4, 2029);
