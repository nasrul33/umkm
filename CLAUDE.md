# SIA-UMKM Premium — Project Memory

## Ringkasan Produk
Platform dua-muka per klien UMKM (deployment single-tenant, bukan SaaS multi-tenant):
1. **Halaman profil bisnis premium** (publik, tanpa login) — etalase digital resmi UMKM, domain kustom.
2. **Aplikasi akuntansi** (privat, di balik login) — identitas usaha, master data, transaksi harian bahasa awam, laporan keuangan, perpajakan, analisis biaya.

Dokumen acuan: `BRD-UMKM-01 v1.0`, `SRS-UMKM-01 v1.0`. Selalu cek dokumen ini dulu sebelum mengambil keputusan lingkup — jangan menebak requirement yang tidak ada di sana.

## Stack Teknis (non-negotiable, jangan diganti tanpa alasan kuat)
- **Backend**: Java 25 LTS, Spring Boot 4.1 (Spring Framework 7, Jakarta EE 11, Hibernate 7, Jackson 3)
- **Frontend**: Vue 3 + Tailwind CSS (SPA terpisah, konsumsi REST API)
- **Database**: PostgreSQL 18
- **Deployment**: Docker Compose per klien (single-tenant), Nginx + Certbot per domain kustom
- **Testing**: JUnit 5 + Testcontainers (wajib pakai PostgreSQL asli untuk test kalkulasi pajak, jangan mock DB untuk logic uang)

## Aturan Emas (Golden Rules) — berlaku di semua modul
1. **Uang selalu `BigDecimal` + kolom `NUMERIC`.** Tidak pernah `float`/`double`. Selalu set `RoundingMode` eksplisit.
2. **Jurnal ter-posting itu immutable.** Koreksi = jurnal pembalik baru (`reversal_of_id`), ditegakkan oleh trigger PostgreSQL, bukan cuma validasi Java.
3. **Parameter pajak itu data, bukan kode.** Simpan di tabel `tax_rule` dengan `berlaku_dari`/`berlaku_sampai`. Jangan pernah hardcode tarif/ambang batas di Java — regulasi pajak UMKM sudah berubah 2x dalam 12 bulan terakhir (PP 55/2022 → PP 20/2026).
4. **Audit trail hash-chain wajib** di tabel `audit_log` untuk semua transaksi keuangan — pola identik SIA-PDAM.
5. **Produk BUKAN jasa konsultasi pajak.** Modul B5 hanya menampilkan hasil kalkulasi + rujukan pasal. Jangan pernah menulis UI copy yang berbunyi seperti opini/rekomendasi pajak personal — ini batasan hukum, bukan preferensi gaya.
6. **Semua panggilan API eksternal (PJAP/Coretax) butuh retry + fallback.** Coretax sering down; sistem tidak boleh berhenti mencatat transaksi internal hanya karena API pajak eksternal gagal.
7. **Halaman publik dan aplikasi akuntansi dipisah tegas** di level Spring Security filter chain (`/public/**` vs `/app/**`).

## Struktur Modul (rujukan cepat)
| Kode | Modul | Agent utama |
|---|---|---|
| A | Halaman Profil Bisnis | premium-ux-designer |
| B1 | Identitas Usaha | database-schema-designer |
| B2 | Master Data | database-schema-designer |
| B3 | Transaksi Harian | accounting-engine-architect |
| B4 | Laporan Keuangan | accounting-engine-architect |
| B5 | Perpajakan | tax-compliance-specialist, api-integration-engineer |
| B6 | Analisis Biaya | accounting-engine-architect |

## Kapan Memanggil Agent
Lihat `.claude/agents/*.md` untuk domain masing-masing. Jangan implementasi modul B3/B4/B5 tanpa konsultasi agent terkait terlebih dahulu — logic uang dan pajak adalah area paling gampang salah dan paling mahal kalau salah.

## Definition of Done
Setiap modul dianggap selesai hanya jika memenuhi `artifacts/module-definition-of-done.md`.
