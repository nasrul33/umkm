package com.siaumkm.tax;

import com.siaumkm.tax.PjapClient.BillingOrder;
import com.siaumkm.tax.PjapClient.BillingResult;
import com.siaumkm.tax.PjapClient.PjapAmbiguousResponseException;
import com.siaumkm.tax.PjapClient.PjapPermanentException;
import com.siaumkm.tax.PjapClient.PjapTransientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * NFR-09: klasifikasi kegagalan & retry HttpPjapClient — tanpa Spring context,
 * MockRestServiceServer diikat langsung ke RestClient.Builder.
 */
class HttpPjapClientTest {

    private static final String BASE = "https://pjap.uji.example";

    private record Rakitan(HttpPjapClient client, MockRestServiceServer server) {}

    private Rakitan rakit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PjapProperties props = new PjapProperties("uji", BASE, "api-key-rahasia",
                new PjapProperties.Retry(3, 1L), new PjapProperties.Timeout(null, null));
        return new Rakitan(new HttpPjapClient(builder, props), server);
    }

    private BillingOrder order() {
        return new BillingOrder("ref-123", "1234567890123456", "411128", "420", 6, 6, 2026, 250000L);
    }

    @Test
    void sukses_mengembalikanKodeBilling_denganIdempotencyKey() {
        Rakitan r = rakit();
        r.server().expect(requestTo(BASE + "/billing"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "ref-123"))
                .andExpect(header("Authorization", "Bearer api-key-rahasia"))
                .andRespond(withSuccess(
                    "{\"kodeBilling\":\"012345678901234\",\"requestId\":\"prov-1\"}",
                    MediaType.APPLICATION_JSON));

        BillingResult hasil = r.client().buatKodeBilling(order());

        assertThat(hasil.kodeBilling()).isEqualTo("012345678901234");
        assertThat(hasil.providerRequestId()).isEqualTo("prov-1");
        r.server().verify();
    }

    @Test
    void gagal5xxDuaKali_laluSukses_totalTigaPanggilan() {
        Rakitan r = rakit();
        r.server().expect(times(2), requestTo(BASE + "/billing")).andRespond(withServerError());
        r.server().expect(requestTo(BASE + "/billing")).andRespond(withSuccess(
                "{\"kodeBilling\":\"012345678901234\"}", MediaType.APPLICATION_JSON));

        BillingResult hasil = r.client().buatKodeBilling(order());

        assertThat(hasil.kodeBilling()).isEqualTo("012345678901234");
        r.server().verify();
    }

    @Test
    void gagal5xxSemuaAttempt_melemparTransient() {
        Rakitan r = rakit();
        r.server().expect(times(3), requestTo(BASE + "/billing")).andRespond(withServerError());

        assertThatThrownBy(() -> r.client().buatKodeBilling(order()))
                .isInstanceOf(PjapTransientException.class);
        r.server().verify(); // tepat 3 attempt, tidak lebih
    }

    @Test
    void gagal4xx_permanent_tepatSatuPanggilan_tanpaRetry() {
        Rakitan r = rakit();
        r.server().expect(times(1), requestTo(BASE + "/billing")).andRespond(withBadRequest());

        assertThatThrownBy(() -> r.client().buatKodeBilling(order()))
                .isInstanceOf(PjapPermanentException.class);
        r.server().verify();
    }

    @Test
    void sukses2xxTapiBodyTidakValid_ambiguous_tanpaRetry() {
        Rakitan r = rakit();
        // kode bukan 15 digit — billing mungkin sudah terbit di sisi PJAP
        r.server().expect(times(1), requestTo(BASE + "/billing")).andRespond(withSuccess(
                "{\"kodeBilling\":\"pendek\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> r.client().buatKodeBilling(order()))
                .isInstanceOf(PjapAmbiguousResponseException.class);
        r.server().verify();
    }

    @Test
    void baseUrlNonHttps_ditolakSaatKonstruksi() {
        PjapProperties props = new PjapProperties("uji", "http://pjap.tidak-aman.example",
                "key", null, null);

        assertThatThrownBy(() -> new HttpPjapClient(RestClient.builder(), props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https");
    }
}
