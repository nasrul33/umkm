package com.siaumkm.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TaxCalculationLogRepository extends JpaRepository<TaxCalculationLog, UUID> {

    /** Baris yang berlaku utk satu masa = kalkulasi terbaru (tabel append-only). */
    Optional<TaxCalculationLog> findFirstByBusinessEntityIdAndPeriodeTahunAndPeriodeBulanOrderByCalculatedAtDesc(
            UUID businessEntityId, Integer periodeTahun, Integer periodeBulan);
}
