---
description: Membuat boilerplate laporan keuangan baru (controller-service-view-test)
---

Gunakan agent `accounting-engine-architect` untuk workflow ini.

1. Konfirmasi nama laporan baru dan sumber datanya (akun COA mana saja yang relevan).
2. Buat/perbarui view SQL materialized (`vw_<nama_laporan>`) yang mengagregasi `journal_entry` — jangan buat tabel ringkasan manual.
3. Buat entity/DTO read-only yang memetakan hasil view.
4. Buat endpoint REST `GET /app/reports/<nama-laporan>` dengan parameter periode (dari-sampai tanggal).
5. Buat test JUnit 5 + Testcontainers yang memverifikasi angka laporan terhadap skenario jurnal seed yang diketahui hasilnya secara manual.
6. Tambahkan opsi ekspor PDF/Excel mengikuti pola modul laporan yang sudah ada (jangan bangun mekanisme ekspor baru per laporan).
