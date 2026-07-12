# Definition of Done — Modul SIA-UMKM Premium

Sebuah modul BELUM dianggap selesai sampai seluruh item berikut tercentang. Rujuk BR-xxx dan SRS-xxx terkait saat mencentang.

## Fungsional
- [ ] Seluruh BR-xxx terkait modul ini terimplementasi (cek silang ke BRD-UMKM-01).
- [ ] Seluruh SRS-xxx terkait modul ini terimplementasi (cek silang ke SRS-UMKM-01 Bagian 3).
- [ ] Test JUnit 5 + Testcontainers lulus untuk seluruh service/kalkulasi kritis (bukan hanya happy path — sertakan test kasus tepi/edge case).

## Uang & Akuntansi (jika modul menyentuh nilai uang)
- [ ] Seluruh nilai uang memakai `BigDecimal`, tidak ada `float`/`double`.
- [ ] Kolom database uang memakai `NUMERIC`, bukan `FLOAT`/`MONEY`.
- [ ] Jurnal balance (debit = kredit) divalidasi di service layer sebelum insert.
- [ ] Jurnal POSTED immutable — dikonfirmasi via test yang mencoba UPDATE dan mengharapkan exception dari trigger.

## Perpajakan (jika modul menyentuh Modul B5)
- [ ] Tidak ada tarif/ambang batas yang di-hardcode; semua dari tabel `tax_rule`.
- [ ] Kalkulasi memakai tanggal transaksi, bukan tanggal sistem saat ini.
- [ ] Tidak ada teks UI yang berbunyi seperti opini/rekomendasi pajak personal.

## Keamanan & Audit
- [ ] Audit trail hash-chain aktif untuk tabel transaksi keuangan terkait (lolos `/audit-check`).
- [ ] Data sensitif (NPWP, NIK, no. rekening) dienkripsi di application layer.
- [ ] Endpoint baru diberi `@PreAuthorize` yang sesuai role (OWNER/STAFF).

## UI/UX
- [ ] Tidak memakai komponen template generik tanpa modifikasi visual berarti.
- [ ] Responsif desktop/tablet/mobile diverifikasi manual di ketiga breakpoint.
- [ ] Bahasa yang dipakai di wizard/form sudah diverifikasi "dapat dipahami pemilik usaha non-akuntan" (bukan istilah akuntansi mentah).

## Dokumentasi
- [ ] Traceability BR → SRS → kode terdokumentasi (komentar/README modul, bukan hanya di kepala developer).
- [ ] Perubahan `tax_rule` (jika ada) tercatat di `tax-rule-changelog-template.md`.
