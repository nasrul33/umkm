package com.siaumkm.tax;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Map;

/**
 * Implementasi HTTP generik PjapClient (NFR-09). Retry HANYA di sini —
 * BillingService tidak boleh me-retry di atasnya (risiko billing ganda).
 * Kebijakan (konsultasi api-integration-engineer):
 * - Retry: 5xx/429/408 + kegagalan KONEKSI (belum terkirim).
 * - Read timeout: request mungkin sudah sampai — TANPA dukungan idempotency
 *   provider, JANGAN retry -> ambiguous (PENDING_MANUAL).
 * - 4xx lain: permanent. 2xx ber-body tidak valid: ambiguous (billing mungkin
 *   sudah terbit di sisi PJAP — retry = risiko duplikat).
 * - API key tidak pernah masuk log/payload tersimpan.
 */
public class HttpPjapClient implements PjapClient {

    private static final Logger log = LoggerFactory.getLogger(HttpPjapClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final PjapProperties properties;

    public HttpPjapClient(RestClient.Builder builder, PjapProperties properties) {
        this.properties = properties;
        if (properties.baseUrl() != null && !properties.baseUrl().startsWith("https://")
                && !properties.baseUrl().startsWith("http://localhost")) {
            throw new IllegalStateException(
                "PJAP_BASE_URL wajib https:// — payload memuat NPWP (NFR-05).");
        }
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BillingResult buatKodeBilling(BillingOrder order) {
        int maxAttempts = properties.retryAtauDefault().maxAttemptsAtauDefault();
        long backoffMs = properties.retryAtauDefault().backoffMsAtauDefault();

        PjapTransientException terakhir = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return kirim(order, attempt);
            } catch (PjapTransientException e) {
                terakhir = e;
                log.info("PJAP billing ref={} attempt {}/{} gagal transient: {}",
                        order.clientReference(), attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PjapTransientException("Retry diinterupsi", ie);
                    }
                }
            }
        }
        throw terakhir;
    }

    private BillingResult kirim(BillingOrder order, int attempt) {
        try {
            return restClient.post()
                    .uri("/billing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("X-Idempotency-Key", order.clientReference())
                    .body(Map.of(
                        "clientReference", order.clientReference(),
                        "npwp", order.npwp(),
                        "kodeAkunPajak", order.kodeAkunPajak(),
                        "kodeJenisSetoran", order.kodeJenisSetoran(),
                        "masaAwal", order.masaAwal(),
                        "masaAkhir", order.masaAkhir(),
                        "tahunPajak", order.tahunPajak(),
                        "jumlah", order.jumlah()))
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        String body = response.bodyTo(String.class);
                        if (status.is2xxSuccessful()) {
                            return parseSukses(body);
                        }
                        int code = status.value();
                        if (status.is5xxServerError() || code == 429 || code == 408) {
                            throw new PjapTransientException("PJAP membalas HTTP " + code);
                        }
                        throw new PjapPermanentException(
                            "PJAP menolak request (HTTP " + code + ") — periksa data/kredensial.");
                    });
        } catch (ResourceAccessException e) {
            // Koneksi belum terjalin -> aman di-retry; read timeout -> ambiguous.
            if (e.getCause() instanceof SocketTimeoutException
                    && !(e.getCause() instanceof ConnectException)) {
                throw new PjapAmbiguousResponseException(
                    "Read timeout — request mungkin sudah diterima PJAP, tidak di-retry demi mencegah kode billing ganda.", e);
            }
            throw new PjapTransientException("Koneksi ke PJAP gagal: " + e.getMessage(), e);
        }
    }

    private BillingResult parseSukses(String body) {
        try {
            JsonNode json = MAPPER.readTree(body);
            String kode = json.path("kodeBilling").asText(null);
            if (kode == null || !kode.matches("^[0-9]{15}$")) {
                throw new PjapAmbiguousResponseException(
                    "Respons 2xx tanpa kodeBilling 15 digit yang valid — billing mungkin sudah terbit di sisi PJAP.");
            }
            Instant masaAktif = json.hasNonNull("masaAktifSampai")
                    ? Instant.parse(json.get("masaAktifSampai").asText()) : null;
            return new BillingResult(kode, masaAktif,
                    json.path("requestId").asText(null), body);
        } catch (PjapAmbiguousResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new PjapAmbiguousResponseException(
                "Respons 2xx tidak dapat diurai: " + e.getMessage(), e);
        }
    }
}
