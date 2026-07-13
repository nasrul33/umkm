package com.siaumkm.tax;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * BR-B5-07: kode billing PJAP. OWNER-only. Respons memuat payload MASKED —
 * NPWP utuh tidak pernah keluar lewat endpoint ini.
 */
@RestController
@RequestMapping("/app/tax/billing")
@PreAuthorize("hasRole('OWNER')")
public class BillingController {

    public record CatatManualRequest(String kodeBilling) {}

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/{taxCalculationLogId}")
    public ResponseEntity<BillingRequest> buat(@PathVariable UUID taxCalculationLogId) {
        return ResponseEntity.ok(billingService.buatKodeBilling(taxCalculationLogId));
    }

    /** SRS-B5-05: pencatatan kode billing yang dibuat manual di DJP Online. */
    @PutMapping("/{id}/manual")
    public ResponseEntity<BillingRequest> catatManual(@PathVariable UUID id,
                                                       @RequestBody CatatManualRequest request) {
        return ResponseEntity.ok(billingService.catatManual(id, request.kodeBilling()));
    }
}
