package com.siaumkm.identity;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NFR-05: NPWP/NIK harus tersimpan sebagai ciphertext di database (bukan hanya
 * disk-level encryption) dan tetap terbaca plaintext lewat aplikasi. Wajib
 * PostgreSQL asli — yang diverifikasi adalah isi kolom sesungguhnya.
 */
@Testcontainers
@SpringBootTest
class SensitiveFieldEncryptionTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("siaumkm_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private BusinessEntityRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String NPWP = "1234567890123456";
    private static final String NIK  = "3175012345678901";

    @Test
    void npwpDanNik_tersimpanTerenkripsi_terbacaPlaintext() {
        UUID id = repository.saveAndFlush(entitasBaru(NPWP, NIK)).getId();

        // Isi kolom sesungguhnya di database: ciphertext berprefix, bukan plaintext
        String npwpDb = jdbcTemplate.queryForObject(
                "SELECT npwp FROM business_entity WHERE id = ?", String.class, id);
        String nikDb = jdbcTemplate.queryForObject(
                "SELECT nik_pemilik FROM business_entity WHERE id = ?", String.class, id);

        assertThat(npwpDb).startsWith("enc:v1:").doesNotContain(NPWP);
        assertThat(nikDb).startsWith("enc:v1:").doesNotContain(NIK);

        // Dibaca lewat aplikasi: kembali plaintext utuh
        BusinessEntity dibaca = repository.findById(id).orElseThrow();
        assertThat(dibaca.getNpwp()).isEqualTo(NPWP);
        assertThat(dibaca.getNikPemilik()).isEqualTo(NIK);
    }

    @Test
    void ivAcak_duaNilaiSama_menghasilkanCiphertextBerbeda() {
        UUID id1 = repository.saveAndFlush(entitasBaru("6543210987654321", NIK)).getId();
        UUID id2 = repository.saveAndFlush(entitasBaru("6543210987654322", NIK)).getId();

        String nik1 = jdbcTemplate.queryForObject(
                "SELECT nik_pemilik FROM business_entity WHERE id = ?", String.class, id1);
        String nik2 = jdbcTemplate.queryForObject(
                "SELECT nik_pemilik FROM business_entity WHERE id = ?", String.class, id2);

        // NIK identik, ciphertext harus berbeda (IV acak per nilai)
        assertThat(nik1).isNotEqualTo(nik2);
    }

    @Test
    void ciphertextDirusakDiLuarAplikasi_gagalDibacaDenganJelas() {
        UUID id = repository.saveAndFlush(entitasBaru("1111222233334444", NIK)).getId();

        jdbcTemplate.update(
                "UPDATE business_entity SET nik_pemilik = 'enc:v1:cnVzYWtydXNha3J1c2FrcnVzYWtydXNhaw==' WHERE id = ?", id);

        assertThatThrownBy(() -> repository.findById(id).orElseThrow().getNikPemilik())
                .hasStackTraceContaining("mendekripsi");
    }

    @Test
    void nilaiLegacyTanpaPrefix_tetapTerbacaApaAdanya() {
        // Toleransi data pra-enkripsi (instalasi lama): nilai tanpa prefix
        // enc:v1: dikembalikan apa adanya — perilaku yang disengaja dan
        // terdokumentasi; akan terenkripsi saat baris disimpan ulang.
        UUID id = jdbcTemplate.queryForObject(
                """
                INSERT INTO business_entity (nama_usaha, nama_pemilik, npwp, nik_pemilik, bentuk_badan)
                VALUES ('Usaha Legacy', 'Pemilik Legacy', '9998887776665554', '3175099988877766', 'OP')
                RETURNING id
                """, UUID.class);

        BusinessEntity dibaca = repository.findById(id).orElseThrow();
        assertThat(dibaca.getNpwp()).isEqualTo("9998887776665554");
        assertThat(dibaca.getNikPemilik()).isEqualTo("3175099988877766");
    }

    @Test
    void formatNpwpTetapDivalidasiAtasPlaintext() {
        // CHECK 16-digit di DB sudah dihapus (V5) — @Pattern harus tetap menolak
        assertThatThrownBy(() -> repository.saveAndFlush(entitasBaru("BUKAN-NPWP", NIK)))
                .hasStackTraceContaining("16 digit");
    }

    private BusinessEntity entitasBaru(String npwp, String nik) {
        BusinessEntity be = new BusinessEntity();
        be.setNamaUsaha("Usaha Uji Enkripsi " + UUID.randomUUID());
        be.setNamaPemilik("Pemilik Uji");
        be.setNpwp(npwp);
        be.setNikPemilik(nik);
        be.setBentukBadan(BentukBadanUsaha.OP);
        return be;
    }
}
