package com.siaumkm.tax;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Konfigurasi PJAP per instalasi klien (base-url/api-key via .env — NFR-09). */
@ConfigurationProperties(prefix = "siaumkm.pjap")
public record PjapProperties(String provider,
                              String baseUrl,
                              String apiKey,
                              Retry retry,
                              Timeout timeout) {

    public record Retry(Integer maxAttempts, Long backoffMs) {
        public int maxAttemptsAtauDefault() { return maxAttempts != null ? maxAttempts : 3; }
        public long backoffMsAtauDefault() { return backoffMs != null ? backoffMs : 2000L; }
    }

    public record Timeout(Long connectMs, Long readMs) {
        public long connectMsAtauDefault() { return connectMs != null ? connectMs : 3000L; }
        public long readMsAtauDefault() { return readMs != null ? readMs : 10000L; }
    }

    public Retry retryAtauDefault() { return retry != null ? retry : new Retry(null, null); }
    public Timeout timeoutAtauDefault() { return timeout != null ? timeout : new Timeout(null, null); }
}
