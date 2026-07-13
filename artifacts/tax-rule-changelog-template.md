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

## Pembulatan pajak terutang rupiah penuh — 13 Juli 2026 (integrasi e-billing V9)

- **Regulasi acuan**: PER-11/PJ/2025 (era Coretax): pembulatan aritmetis — pecahan < 0,50 ke bawah, >= 0,50 ke atas (menggantikan rezim "selalu ke bawah" SE-22/PJ.24/1990 & PER-29/PJ/2015).
- **Perubahan**: `TaxCalculationEngine` membulatkan pajak terutang ke rupiah penuh (HALF_UP scale 0, disimpan xx,00) — di engine, BUKAN di lapisan billing, agar angka di `tax_calculation_log`, kode billing, dan setoran identik; `BillingService` hanya meng-assert nilai sudah bulat (guard, bukan transformasi). DPP TIDAK dipangkas — pembulatan ribuan hanya berlaku PKP tarif umum Pasal 17(4), bukan PPh Final.
- **Berlaku dari tanggal**: semua kalkulasi baru. **Limitation**: rekalkulasi masa pra-2025 secara ketat memakai rezim pembulatan-ke-bawah lama (selisih maks Rp1) — satu mode saja di engine, diterima terdokumentasi.
- **Baris tax_rule terdampak**: tidak ada (rumus). Kolom V9 terkait: `kode_akun_pajak`=411128, `kode_jenis_setoran`=420 utk seluruh PPH-FINAL-UMKM* (KAP/KJS sebagai data).
- **Test yang ditambahkan**: `TaxCalculationEngineTest#pajakDibulatkanAritmetisKeRupiahPenuh_perPER11PJ2025`, `BillingServiceTest#pajakBerdesimal_failFast_tanpaPanggilanHttp`.
- **Dampak retroaktif**: tidak — baris log lama utuh (append-only).
- **Dicatat oleh**: Claude Code (konsultasi tax-compliance-specialist, sumber PAJAK.COM/Ortax/DDTC)

---

## Rumus DPP bulan penembusan Rp500 juta — 13 Juli 2026 (V8: koreksi TaxCalculationEngine)

- **Regulasi acuan**: PP 55/2022 Pasal 60 jo. PMK 164/2023 (dipertahankan PP 20/2026): tarif 0,5% hanya dikenakan atas bagian peredaran bruto di atas Rp500 juta.
- **Perubahan**: (a) rumus engine dikoreksi — sebelumnya bulan penembusan dikenai pajak atas omzet bulan PENUH (over-tax); kini DPP = max(0, min(omzet bulan, kumulatif-sesudah − Rp500 jt)); semantik `omzetKumulatifTahunan` dikunci = kumulatif SESUDAH bulan berjalan; DPP tidak pernah negatif; (b) kolom `tax_calculation_log.omzet_kena_pajak` (DPP eksplisit, auditable) + index masa + trigger append-only & audit hash-chain; (c) `PphMasaService` (BR-B5-01): kalkulasi masa end-to-end dari pembukuan via `OmzetUsahaQuery` (definisi omzet SATU dengan agregasi Pasal 58), jadwal finalisasi masa T di awal T+1, entri `tax_calendar` jatuh tempo setor-sendiri tanggal 15 bulan berikutnya (configurable).
- **Berlaku dari tanggal**: berlaku untuk semua kalkulasi baru (rumus historis PP 55/2022 memang sama — koreksi bug, bukan perubahan aturan).
- **Baris tax_rule terdampak**: tidak ada (perubahan rumus/kolom log, parameter tetap).
- **Test yang diperbarui/ditambahkan**: `TaxCalculationEngineTest#bulanPenembusan500jt_hanyaKelebihanYangKenaPajak`, `#kumulatifTepatRp500jt_belumKenaPajak`, `#bulanPertamaLangsungMelewatiAmbang_dppSebesarKelebihan`, `#omzetBulanNegatif_tidakPernahPajakNegatif`, seluruh `PphMasaServiceTest` (7 test).
- **Dampak retroaktif**: kalkulasi ulang bulan penembusan menghasilkan angka lebih kecil yang BENAR; baris `tax_calculation_log` lama tidak diubah (append-only). **Limitation**: koreksi/pembalik lintas masa di-netting ke masa berjalan dengan DPP di-floor 0 — kaidah resmi pembetulan masa asal (pemindahbukuan) tidak diotomasi.
- **Dicatat oleh**: Claude Code (konsultasi tax-compliance-specialist, sumber PMK 164/2023 via Pajakku/DDTC/DJP)

---

## PPH-FINAL-UMKM-KOPERASI — 13 Juli 2026 (V7-b: batas waktu koperasi PP 20/2026)

- **Regulasi acuan**: PP 20/2026 (ditetapkan & diundangkan 22 April 2026): koperasi paling lama 4 tahun pajak sejak terdaftar (inklusif tahun terdaftar); koperasi yang terdaftar sebelum PP berlaku memakai masa transisi s.d. Tahun Pajak 2029. OP dan PT Perorangan tetap tanpa batas waktu.
- **Perubahan**: (a) kolom baru `tax_rule.batas_tahun_pajak` dan `tax_rule.tahun_pajak_akhir_transisi` — batas waktu kini DATA, bukan kode; (b) kolom `business_entity.tanggal_terdaftar_pajak`; (c) baris baru `PPH-FINAL-UMKM-KOPERASI` PP 20/2026 (batas 4 tahun, transisi 2029); (d) KOPERASI dikeluarkan dari baris `PPH-FINAL-UMKM-PT-PERORANGAN` PP 20/2026 dan deskripsi yang keliru ("tanpa batas waktu ... Koperasi") dikoreksi; (e) `TaxCalculationEngine` menolak kalkulasi koperasi yang melewati batas atau tanpa tanggal terdaftar.
- **Berlaku dari tanggal**: 22 April 2026
- **Baris tax_rule terdampak**: `PPH-FINAL-UMKM-PT-PERORANGAN` (UPDATE array+deskripsi), `PPH-FINAL-UMKM-KOPERASI` (INSERT)
- **Test yang diperbarui/ditambahkan**: `TaxCalculationEngineTest#koperasiBaru_dalamEmpatTahunPajak_kenaPajak`, `#koperasiBaru_tahunKelima_ditolak`, `#koperasiLama_masaTransisiFlatSampai2029_bukanTerdaftarPlus3`, `#koperasiTanpaTanggalTerdaftar_ditolakDenganPesanJelas`, `#ptPerorangan_tanpaBatasWaktu_tanggalTerdaftarTidakDiwajibkan`
- **Dampak retroaktif**: Kalkulasi koperasi yang sebelumnya lolos padahal melewati batas kini ditolak — koreksi salah hitung, bukan regresi; `tax_calculation_log` lama tidak diubah.
- **Dicatat oleh**: Claude Code (konsultasi tax-compliance-specialist, sumber DJP/DDTC/Ortax)
