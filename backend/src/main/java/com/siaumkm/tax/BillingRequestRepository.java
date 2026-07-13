package com.siaumkm.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BillingRequestRepository extends JpaRepository<BillingRequest, UUID> {
    Optional<BillingRequest> findFirstByTaxCalculationLogIdAndStatusNotOrderByRequestedAtDesc(
            UUID taxCalculationLogId, BillingRequest.Status statusBukan);
}
