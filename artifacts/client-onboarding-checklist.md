# Checklist Onboarding Klien Baru (Instalasi Single-Tenant)

## Sebelum Provisioning
- [ ] Nama usaha, bentuk badan usaha, dan domain kustom klien sudah dikonfirmasi.
- [ ] Domain klien sudah diarahkan (DNS A/CNAME record) ke IP server sebelum request SSL — hindari gagal Certbot akibat DNS belum propagasi.
- [ ] Mitra PJAP yang akan dipakai klien sudah ditentukan (jika Modul B5 langsung diaktifkan).

## Provisioning
- [ ] File `.env` instance baru dibuat dari template, berisi domain, kredensial DB, kredensial PJAP (terenkripsi).
- [ ] Container Docker Compose instance baru berjalan tanpa error (`docker compose ps` semua service healthy).
- [ ] Migrasi Flyway awal berhasil: COA standar UMKM ter-seed, `tax_rule` aktif per tanggal hari ini ter-seed.
- [ ] Nginx server block + Certbot SSL untuk domain baru aktif dan terverifikasi (cek `https://` domain klien tidak warning sertifikat).

## Verifikasi
- [ ] Halaman profil publik (`/public/`) dapat diakses dan menampilkan data default/placeholder yang benar.
- [ ] Halaman login aplikasi akuntansi (`/app/login`) dapat diakses dan terpisah dari halaman publik.
- [ ] Wizard onboarding identitas usaha (Modul B1) dapat diselesaikan end-to-end oleh akun OWNER pertama.
- [ ] Backup harian (`pg_dump`) terjadwal dan sudah diverifikasi berjalan minimal satu kali.

## Serah Terima ke Klien
- [ ] Kredensial login pertama (OWNER) diserahkan ke klien melalui kanal aman (bukan email plaintext ke sekali kirim tanpa reset wajib).
- [ ] Klien diarahkan untuk mengganti password default pada login pertama.
- [ ] Dokumentasi singkat cara pakai wizard transaksi harian diserahkan (bahasa awam, bukan manual teknis).
