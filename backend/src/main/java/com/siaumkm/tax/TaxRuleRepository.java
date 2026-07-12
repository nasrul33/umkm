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
    @Query("""
        SELECT r FROM TaxRule r
        WHERE r.kodeAturan = :kodeAturan
        AND :bentukBadan MEMBER OF r.bentukBadanBerlaku
        AND r.berlakuDari <= :tanggalTransaksi
        AND (r.berlakuSampai IS NULL OR r.berlakuSampai >= :tanggalTransaksi)
        ORDER BY r.berlakuDari DESC
        """)
    Optional<TaxRule> findAturanAktif(@Param("kodeAturan") String kodeAturan,
                                       @Param("bentukBadan") BentukBadanUsaha bentukBadan,
                                       @Param("tanggalTransaksi") LocalDate tanggalTransaksi);
}
