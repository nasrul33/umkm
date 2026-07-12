---
description: Menambah atau mengubah parameter pajak (tax_rule) dengan jejak regulasi yang jelas
---

Gunakan agent `tax-compliance-specialist` untuk workflow ini. JANGAN edit tarif/ambang batas langsung di kode Java.

1. Minta pengguna menyebutkan: kode aturan yang terdampak, regulasi acuan (nomor PP/PMK/PER beserta tanggal), nilai baru (tarif/ambang batas), dan tanggal mulai berlaku.
2. Cek apakah ini perubahan aturan yang sudah ada (insert baris baru dengan `berlaku_dari` baru, set `berlaku_sampai` pada baris lama) atau aturan benar-benar baru.
3. Insert/update baris di tabel `tax_rule` — jangan pernah UPDATE nilai tarif pada baris yang sudah pernah dipakai untuk kalkulasi transaksi historis (itu akan mengubah retroaktif hasil kalkulasi lama yang sudah benar pada saat itu).
4. Catat perubahan ini di `artifacts/tax-rule-changelog-template.md` (salin formatnya, isi entri baru).
5. Tambahkan/perbarui test case di `TaxCalculationEngineTest` yang memverifikasi kalkulasi benar untuk periode SEBELUM dan SESUDAH tanggal `berlaku_dari` yang baru.
6. Jalankan seluruh test suite modul B5 sebelum menandai tugas selesai — perubahan tax_rule punya risiko regresi tinggi ke kalkulasi historis.
