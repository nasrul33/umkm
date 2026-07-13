package com.siaumkm.tax;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * NFR-09/Aturan Emas #6: tanpa PJAP_BASE_URL (instalasi belum punya mitra
 * PJAP) TIDAK ada bean PjapClient — BillingService mendeteksinya via
 * ObjectProvider dan menjatuhkan billing ke PENDING_MANUAL, sistem tetap
 * hidup dan pencatatan internal jalan terus.
 */
@Configuration
public class PjapConfig {

    @Bean
    @ConditionalOnExpression("'${siaumkm.pjap.base-url:}'.trim().length() > 0")
    PjapClient pjapClient(RestClient.Builder builder, PjapProperties properties) {
        // Timeout ketat (properti, bukan konstanta): API pajak eksternal sering
        // lambat/down — jangan biarkan thread menggantung (NFR-09).
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.timeoutAtauDefault().connectMsAtauDefault());
        factory.setReadTimeout((int) properties.timeoutAtauDefault().readMsAtauDefault());
        return new HttpPjapClient(builder.requestFactory(factory), properties);
    }
}
