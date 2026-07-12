package com.siaumkm.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR-10 & Aturan Emas #7: verifikasi end-to-end pemisahan publik vs /app,
 * login JWT, dan pembatasan role OWNER/STAFF — di PostgreSQL asli.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSecurityTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        pastikanUser("owner@uji.id", "rahasia-owner", AppUser.PeranPengguna.OWNER);
        pastikanUser("staff@uji.id", "rahasia-staff", AppUser.PeranPengguna.STAFF);
    }

    @Test
    void loginBenar_menerimaTokenDanRole() {
        var resp = kirim(HttpMethod.POST, "/app/auth/login", null,
                Map.of("username", "owner@uji.id", "password", "rahasia-owner"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) resp.getBody().get("token")).isNotBlank();
        assertThat(resp.getBody().get("role")).isEqualTo("OWNER");
    }

    @Test
    void loginPasswordSalah_401_tanpaDetail() {
        var resp = kirim(HttpMethod.POST, "/app/auth/login", null,
                Map.of("username", "owner@uji.id", "password", "salah"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aksesAppTanpaToken_401() {
        var resp = kirim(HttpMethod.GET, "/app/master-data/products", null, null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aksesAppDenganTokenValid_200() {
        var resp = kirim(HttpMethod.GET, "/app/master-data/products",
                token("owner@uji.id", "rahasia-owner"), null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void laporanKeuangan_staffDitolak403() {
        // NFR-10: laporan keuangan sensitif hanya untuk OWNER
        var resp = kirim(HttpMethod.GET, "/app/reports/balance-sheet",
                token("staff@uji.id", "rahasia-staff"), null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reverseJurnal_staffDitolak403() {
        // BR-B3-07 + NFR-10: hanya OWNER yang boleh membalik jurnal final —
        // 403 dari method security, sebelum ID sempat dicari.
        var resp = kirim(HttpMethod.POST, "/app/transaksi/" + java.util.UUID.randomUUID() + "/reverse",
                token("staff@uji.id", "rahasia-staff"), null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void dataSensitif_identitasDanKaryawan_staffDitolak403() {
        // Temuan audit NFR-05: respons memuat NPWP/NIK/gaji plaintext —
        // OWNER-only meski datanya terenkripsi di database.
        String staffToken = token("staff@uji.id", "rahasia-staff");

        var identitas = kirim(HttpMethod.GET, "/app/identity/" + java.util.UUID.randomUUID(),
                staffToken, null);
        assertThat(identitas.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var karyawan = kirim(HttpMethod.GET, "/app/master-data/employees", staffToken, null);
        assertThat(karyawan.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenNgawur_ditolak401() {
        var resp = kirim(HttpMethod.GET, "/app/master-data/products", "token.palsu.ngawur", null);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void hapusMasterData_staffDitolak403_ownerBoleh() {
        String staffToken = token("staff@uji.id", "rahasia-staff");
        String ownerToken = token("owner@uji.id", "rahasia-owner");

        // STAFF boleh menambah master data (NFR-10)
        var created = kirim(HttpMethod.POST, "/app/master-data/products",
                staffToken, Map.of("nama", "Produk Uji Auth"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        String productId = (String) created.getBody().get("id");

        // ...tapi hanya OWNER yang boleh menghapus
        var ditolak = kirim(HttpMethod.DELETE, "/app/master-data/products/" + productId,
                staffToken, null);
        assertThat(ditolak.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var sukses = kirim(HttpMethod.DELETE, "/app/master-data/products/" + productId,
                ownerToken, null);
        assertThat(sukses.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ---- helpers ----

    private void pastikanUser(String username, String password, AppUser.PeranPengguna peran) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            AppUser u = new AppUser();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setNama("Pengguna Uji");
            u.setPeran(peran);
            appUserRepository.save(u);
        }
    }

    private String token(String username, String password) {
        var resp = kirim(HttpMethod.POST, "/app/auth/login", null,
                Map.of("username", username, "password", password));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resp.getBody().get("token");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Map<String, Object>> kirim(HttpMethod method, String uri,
                                                      String bearerToken, Object body) {
        RestClient.RequestBodySpec spec = RestClient.builder()
                .baseUrl("http://localhost:" + port).build()
                .method(method).uri(uri);
        if (bearerToken != null) spec.header("Authorization", "Bearer " + bearerToken);
        if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
        return spec.exchange((request, response) -> {
            Map bodyMap = null;
            try {
                bodyMap = response.bodyTo(Map.class);
            } catch (Exception ignored) {
                // respons tanpa body (mis. 204/401) — cukup status code
            }
            return new ResponseEntity<>(bodyMap, response.getStatusCode());
        });
    }
}
