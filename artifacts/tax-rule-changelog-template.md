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

## Contoh Entri Terisi (referensi format, boleh dihapus setelah entri asli pertama ditambahkan)

## PPH-FINAL-UMKM — 12 Juli 2026

- **Regulasi acuan**: Peraturan Pemerintah Nomor 20 Tahun 2026, ditetapkan 22 April 2026, LN 2026 No. 43.
- **Perubahan**: Menghapus batas waktu pemanfaatan PPh Final 0,5% bagi WP OP dan PT Perorangan (sebelumnya 7 tahun/4 tahun); CV/Firma/PT non-perorangan/BUMDes tidak lagi berhak atas fasilitas ini kecuali masa transisi ketentuan lama belum berakhir.
- **Berlaku dari tanggal**: 22 April 2026
- **Baris tax_rule terdampak**: PPH-FINAL-UMKM-OP, PPH-FINAL-UMKM-BADAN
- **Test yang diperbarui**: `TaxCalculationEngineTest#testNoTimeLimitForIndividualPostPP20_2026`
- **Dampak retroaktif**: Tidak — kalkulasi sebelum 22 April 2026 tetap memakai aturan PP 55/2022 asli.
- **Dicatat oleh**: Nasrullah
