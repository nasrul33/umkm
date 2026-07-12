package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity.BentukBadanUsaha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {

    /**
     * SRS-B5-01: pilih aturan aktif berdasarkan TANGGAL TRANSAKSI, bukan tanggal
     * sistem saat kalkulasi dijalankan — penting untuk kalkulasi ulang/audit
     * periode lampau yang tetap harus memakai aturan yang berlaku saat itu.
     */
    @Query(value = """
        SELECT * FROM tax_rule r
        WHERE r.kode_aturan = :kodeAturan
        AND CAST(:bentukBadan AS bentuk_badan_usaha) = ANY(r.bentuk_badan_berlaku)
        AND r.berlaku_dari <= :tanggalTransaksi
        AND (r.berlaku_sampai IS NULL OR r.berlaku_sampai >= :tanggalTransaksi)
        ORDER BY r.berlaku_dari DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<TaxRule> findAturanAktifNative(@Param("kodeAturan") String kodeAturan,
                                             @Param("bentukBadan") String bentukBadan,
                                             @Param("tanggalTransaksi") LocalDate tanggalTransaksi);

    default Optional<TaxRule> findAturanAktif(String kodeAturan,
                                               BentukBadanUsaha bentukBadan,
                                               LocalDate tanggalTransaksi) {
        return findAturanAktifNative(kodeAturan, bentukBadan.name(), tanggalTransaksi);
    }
}
