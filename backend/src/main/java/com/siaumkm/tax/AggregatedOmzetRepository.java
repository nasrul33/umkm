package com.siaumkm.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AggregatedOmzetRepository extends JpaRepository<AggregatedOmzet, UUID> {
    Optional<AggregatedOmzet> findByBusinessEntityIdAndPeriodeTahun(UUID businessEntityId, Integer periodeTahun);
}
