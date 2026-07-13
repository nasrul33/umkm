package com.siaumkm.tax;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-B5-05: permintaan kode billing ke PJAP mitra. Payload dipersist SUDAH
 * masked (NPWP tidak utuh — NFR-05); transisi status terekam audit hash-chain
 * (trg_audit_billing_request, V9). Satu request non-FAILED per baris kalkulasi
 * (uq_billing_request_active_per_log).
 */
@Entity
@Table(name = "billing_request")
public class BillingRequest {

    public enum Status { DRAFT, REQUESTED, ISSUED, PENDING_MANUAL, FAILED }

    @Id @GeneratedValue private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "tax_calculation_log_id")
    private UUID taxCalculationLogId;

    @Column(name = "pjap_provider", nullable = false, length = 50)
    private String pjapProvider;

    @Column(name = "kode_billing", length = 50)
    private String kodeBilling;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload")
    private String responsePayload;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public void setBusinessEntityId(UUID v) { this.businessEntityId = v; }
    public UUID getTaxCalculationLogId() { return taxCalculationLogId; }
    public void setTaxCalculationLogId(UUID v) { this.taxCalculationLogId = v; }
    public String getPjapProvider() { return pjapProvider; }
    public void setPjapProvider(String v) { this.pjapProvider = v; }
    public String getKodeBilling() { return kodeBilling; }
    public void setKodeBilling(String v) { this.kodeBilling = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; this.updatedAt = Instant.now(); }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String v) { this.requestPayload = v; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String v) { this.responsePayload = v; }
    public Instant getRequestedAt() { return requestedAt; }
}
