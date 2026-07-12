# SIA-UMKM Premium — Skeleton Project

Skeleton ini adalah turunan implementasi dari:
- `BRD-UMKM-01 v1.0`
- `SRS-UMKM-01 v1.0`
- `schema.sql` (tervalidasi nyata di PostgreSQL 18.0)
- `claude-code-scaffold.zip` (CLAUDE.md, agents, commands, artifacts)

## Struktur

```
backend/            Spring Boot 4.1 (Java 25 LTS)
  src/main/java/com/siaumkm/
    config/          SecurityConfig (pemisahan /public vs /app)
    profile/         Modul A — BusinessProfile, ProductShowcase, GalleryItem, Testimonial, BrandTheme, PublicProfileController
    identity/        Modul B1 — BusinessEntity
    masterdata/      Modul B2 — ChartOfAccount, Product, Customer, Vendor, CashAccount (+ ProductController sbg pola CRUD acuan)
    transaction/     Modul B3 — JournalEntry, JournalLine, TransactionWizardService, TransactionController
    report/          Modul B4 — FinancialReportService (query vw_balance_sheet/vw_income_statement), ReportController
    tax/             Modul B5 — TaxRule, TaxCalculationEngine
    cost/            Modul B6 — Budget, CostAnalysisService (query vw_product_margin)
  src/main/resources/
    application.yml
    db/migration/V1__init_schema.sql   (= schema.sql yang sudah tervalidasi)
  src/test/java/com/siaumkm/tax/TaxCalculationEngineTest.java  (Testcontainers, PostgreSQL 18 asli)

frontend/            Vue 3 + Tailwind (sudah di-build & diverifikasi npm run build)
  src/router/        Pemisahan route publik vs /app (meta.requiresAuth)
  src/views/public/  Modul A — ProfileLanding.vue
  src/views/app/     Login, Dashboard, TransactionWizard (bahasa awam)
  src/stores/        Pinia auth store
  src/api/           Axios client dengan JWT interceptor

docker-compose.template.yml   Template deployment single-tenant per klien
.env.template                  Template variabel per instalasi klien
```

## Status Validasi

- ✅ `schema.sql` — dijalankan nyata di PostgreSQL 18.0 (compiled dari source), trigger immutability & audit hash-chain diuji lolos.
- ✅ Frontend — `npm install && npm run build` sukses, seluruh view ter-resolve tanpa error.
- ⚠️ Backend Java — **belum di-build** di lingkungan ini karena Maven Central tidak dapat diakses dari sandbox (bukan masalah kode, melainkan keterbatasan jaringan). Jalankan `./mvnw clean verify` di environment developer untuk memverifikasi kompilasi & test Testcontainers sebelum melanjutkan implementasi modul lain.

## Langkah Selanjutnya (pakai agent yang sudah dibuat)

1. Ekstrak `claude-code-scaffold.zip` ke root repo ini (CLAUDE.md + `.claude/`).
2. Jalankan `mvn wrapper:wrapper` untuk generate `mvnw` (belum disertakan di skeleton ini), lalu `./mvnw clean verify` untuk memverifikasi kompilasi & test Testcontainers.
3. Ketujuh modul (A, B1-B6) sudah punya implementasi awal (entity + repository + minimal 1 controller per modul) — lanjutkan melengkapi endpoint CRUD yang belum ada (Employee, FixedAsset, Invoice, dst.) dengan pola yang identik dengan ProductController.
4. `TransactionController` baru menangani template `JUAL_BARANG_JASA`; tambahkan method baru di `TransactionWizardService` untuk `TERIMA_PEMBAYARAN`, `BELI_BAHAN`, `BAYAR_BIAYA` sebelum wizard frontend berfungsi penuh.
5. Setiap modul baru WAJIB lolos `artifacts/module-definition-of-done.md` sebelum dianggap selesai.
