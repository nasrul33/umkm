package com.siaumkm.tax;

import java.time.Instant;

/**
 * BR-B5-07: kontrak generik pembuatan kode billing via PJAP mitra. Semua
 * provider (Pajakku/Mekari/OnlinePajak dst.) pada intinya membungkus tuple
 * NPWP + KAP + KJS + masa + tahun + jumlah; mapping spesifik provider dibuat
 * saat kredensial mitra tersedia.
 *
 * Klasifikasi kegagalan adalah BAGIAN KONTRAK (bukan urusan BillingService
 * membedah status HTTP):
 * - {@link PjapTransientException}: layak retry (5xx/429/408, koneksi gagal).
 * - {@link PjapPermanentException}: request kita yang salah (4xx lain) — FAILED.
 * - {@link PjapAmbiguousResponseException}: sistem tidak tahu hasil sebenarnya
 *   (read timeout tanpa dukungan idempotency, 2xx ber-body tidak valid) —
 *   JANGAN retry (risiko kode billing ganda), jatuhkan ke PENDING_MANUAL.
 */
public interface PjapClient {

    BillingResult buatKodeBilling(BillingOrder order);

    /**
     * @param clientReference billing_request.id — dikirim sebagai idempotency key
     * @param jumlah rupiah utuh (validasi integer di BillingService)
     */
    record BillingOrder(String clientReference,
                        String npwp,
                        String kodeAkunPajak,
                        String kodeJenisSetoran,
                        int masaAwal, int masaAkhir,
                        int tahunPajak,
                        long jumlah) {}

    record BillingResult(String kodeBilling,
                         Instant masaAktifSampai,
                         String providerRequestId,
                         String rawResponseBody) {}

    class PjapTransientException extends RuntimeException {
        public PjapTransientException(String message, Throwable cause) { super(message, cause); }
        public PjapTransientException(String message) { super(message); }
    }

    class PjapPermanentException extends RuntimeException {
        public PjapPermanentException(String message) { super(message); }
    }

    class PjapAmbiguousResponseException extends RuntimeException {
        public PjapAmbiguousResponseException(String message, Throwable cause) { super(message, cause); }
        public PjapAmbiguousResponseException(String message) { super(message); }
    }
}
