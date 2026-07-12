---
description: Provisioning instalasi baru untuk satu klien UMKM (single-tenant)
---

Jalankan proses onboarding klien baru dengan langkah berikut. Gunakan agent `deployment-provisioner` untuk eksekusi teknis.

1. Tanyakan/konfirmasi ke pengguna: nama usaha, domain kustom yang akan dipakai, bentuk badan usaha (untuk seed `tax_rule` awal yang relevan), dan kredensial mitra PJAP yang dipilih (jika sudah ada).
2. Generate file `.env` baru dari `.env.template` — isi `DOMAIN`, `DB_NAME`, `PJAP_PROVIDER`, `PJAP_API_KEY` (tandai sebagai secret, jangan tampilkan ulang di log).
3. Jalankan migrasi Flyway awal: seed Chart of Accounts standar UMKM + baris `tax_rule` yang berlaku per tanggal hari ini.
4. Konfigurasi Nginx server block untuk domain baru + jalankan Certbot untuk penerbitan SSL. Validasi domain sudah mengarah ke IP server sebelum request sertifikat (hindari rate-limit Let's Encrypt akibat percobaan gagal berulang).
5. Jalankan smoke test: akses halaman publik (`/public/`) dan halaman login (`/app/login`) memastikan keduanya merespons dengan benar dan terpisah sesuai `SecurityFilterChain`.
6. Jadwalkan backup harian (`pg_dump`) untuk instance baru ini.
7. Checklist akhir: rujuk `artifacts/client-onboarding-checklist.md` dan tandai setiap item selesai sebelum menyatakan onboarding sukses.
