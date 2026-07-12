---
description: Pemeriksaan kepatuhan otomatis terhadap audit trail (NFR-04) dan enkripsi (NFR-05) pada modul yang baru diubah
---

Gunakan agent `security-compliance-auditor` untuk workflow ini. Jalankan sebelum setiap merge/rilis.

1. Identifikasi tabel/entity yang diubah pada perubahan terakhir (`git diff` terhadap migrasi & entity JPA).
2. Untuk setiap tabel transaksi keuangan yang diubah/ditambah: verifikasi trigger penulis `audit_log` hash-chain sudah terpasang dan diuji (insert contoh baris, cek hash berantai benar).
3. Untuk setiap kolom baru yang menyimpan data sensitif (NPWP, NIK, no. rekening, kredensial API): verifikasi enkripsi application-layer sudah diterapkan sebelum data disimpan.
4. Verifikasi tidak ada endpoint baru di bawah `/app/**` yang lupa diberi anotasi otorisasi (`@PreAuthorize`).
5. Laporkan hasil sebagai daftar temuan (lihat format pelaporan di `.claude/agents/security-compliance-auditor.md`) — jangan langsung memperbaiki kecuali diminta eksplisit.
