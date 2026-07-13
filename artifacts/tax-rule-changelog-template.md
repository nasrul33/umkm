# Changelog Parameter Pajak (tax_rule)

Setiap perubahan tarif/ambang batas pajak WAJIB dicatat di sini sebelum atau bersamaan dengan perubahan data di tabel `tax_rule`. Salin blok di bawah untuk setiap entri baru — jangan hapus entri lama.

---

## [Kode Aturan] — [Tanggal Perubahan Dicatat]

- **Regulasi acuan**: (contoh: Peraturan Pemerintah Nomor 20 Tahun 2026, ditetapkan 22 April 2026, LN 2026 No. 43)
- **Perubahan**: (contoh: "Menghapus batas waktu 7 tahun PPh Final untuk WP OP; batas waktu kini tidak berlaku selama omzet ≤ Rp4.800.000.000/tahun")
- **Berlaku dari tanggal**: 
- **Baris tax_rule terdampak**: (ID/kode aturan di database)
- **Test yang diperbarui/ditambahkan**: (nama file/method test)
- **Dampak retroaktif**: (Ya/Tidak — jelaskan jika kalkulasi periode lampau ikut terpengaruh atau sengaja tidak diubah)
- **Dicatat oleh**: 

---

## PPH-FINAL-UMKM-OP / PPH-FINAL-UMKM-PT-PERORANGAN / PPH-FINAL-UMKM-KOPERASI — 13 Juli 2026 (V7-a: backfill rezim PP 55/2022)

- **Regulasi acuan**: PP 55/2022 (ditetapkan & diundangkan 20 Desember 2022) jo. UU 7/2021 (HPP, diundangkan 29 Oktober 2021); dicabut-ubah oleh PP 20/2026 sejak 22 April 2026.
- **Perubahan**: Menambah baris effective-dated 2022-12-20 s.d. 2026-04-21 — `PPH-FINAL-UMKM-OP` (0,5%; pengecualian omzet Rp500 juta; ambang atas Rp4,8 M; OP), `PPH-FINAL-UMKM-PT-PERORANGAN` (0,5%; ambang atas Rp4,8 M; PT Perorangan), `PPH-FINAL-UMKM-KOPERASI` (0,5%; ambang atas Rp4,8 M; Koperasi).
- **Berlaku dari tanggal**: 20 Desember 2022 (s.d. 21 April 2026)
- **Baris tax_rule terdampak**: 3 baris INSERT baru (migrasi `V7__tax_rule_pp55_backfill_dan_batas_koperasi.sql`)
- **Test yang diperbarui/ditambahkan**: `TaxCalculationEngineTest#transaksiEraPP55_memakaiAturanLamaBukanAturanBaru`, `#sebelumRezimPP55_tidakAdaAturan_ditolak`, `#koperasiEraPP55_resolveTanpaCekBatas`
- **Dampak retroaktif**: Ya, positif — rekalkulasi/audit periode Des 2022 s.d. 21 Apr 2026 kini resolve (sebelumnya melempar "tidak ada tax_rule aktif"). **Limitation**: (1) periode < 20-12-2022 (rezim PP 23/2018) tidak dicakup; (2) batas waktu rezim lama (OP 7 thn, PT 3 thn, CV/Firma/Koperasi 4 thn sejak terdaftar per PP 23/2018) tidak dimodelkan — rekalkulasi rezim lama bagi WP yang masanya sudah berakhir saat itu akan tetap menghitung 0,5%; (3) CV/FIRMA/PT_BIASA sengaja tanpa baris — engine tidak pernah meng-query tax_rule untuk bentuk badan itu, jalurnya `status_transisi_pajak`.
- **Dicatat oleh**: Claude Code (konsultasi tax-compliance-specialist, sumber DJP/DDTC/Ortax)

---

## PPH-FINAL-UMKM-KOPERASI — 13 Juli 2026 (V7-b: batas waktu koperasi PP 20/2026)

- **Regulasi acuan**: PP 20/2026 (ditetapkan & diundangkan 22 April 2026): koperasi paling lama 4 tahun pajak sejak terdaftar (inklusif tahun terdaftar); koperasi yang terdaftar sebelum PP berlaku memakai masa transisi s.d. Tahun Pajak 2029. OP dan PT Perorangan tetap tanpa batas waktu.
- **Perubahan**: (a) kolom baru `tax_rule.batas_tahun_pajak` dan `tax_rule.tahun_pajak_akhir_transisi` — batas waktu kini DATA, bukan kode; (b) kolom `business_entity.tanggal_terdaftar_pajak`; (c) baris baru `PPH-FINAL-UMKM-KOPERASI` PP 20/2026 (batas 4 tahun, transisi 2029); (d) KOPERASI dikeluarkan dari baris `PPH-FINAL-UMKM-PT-PERORANGAN` PP 20/2026 dan deskripsi yang keliru ("tanpa batas waktu ... Koperasi") dikoreksi; (e) `TaxCalculationEngine` menolak kalkulasi koperasi yang melewati batas atau tanpa tanggal terdaftar.
- **Berlaku dari tanggal**: 22 April 2026
- **Baris tax_rule terdampak**: `PPH-FINAL-UMKM-PT-PERORANGAN` (UPDATE array+deskripsi), `PPH-FINAL-UMKM-KOPERASI` (INSERT)
- **Test yang diperbarui/ditambahkan**: `TaxCalculationEngineTest#koperasiBaru_dalamEmpatTahunPajak_kenaPajak`, `#koperasiBaru_tahunKelima_ditolak`, `#koperasiLama_masaTransisiFlatSampai2029_bukanTerdaftarPlus3`, `#koperasiTanpaTanggalTerdaftar_ditolakDenganPesanJelas`, `#ptPerorangan_tanpaBatasWaktu_tanggalTerdaftarTidakDiwajibkan`
- **Dampak retroaktif**: Kalkulasi koperasi yang sebelumnya lolos padahal melewati batas kini ditolak — koreksi salah hitung, bukan regresi; `tax_calculation_log` lama tidak diubah.
- **Dicatat oleh**: Claude Code (konsultasi tax-compliance-specialist, sumber DJP/DDTC/Ortax)
