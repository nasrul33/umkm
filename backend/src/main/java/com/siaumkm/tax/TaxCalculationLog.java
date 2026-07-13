package com.siaumkm.tax;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-B5-01: jejak kalkulasi PPh Final per masa. APPEND-ONLY (trigger V8):
 * kalkulasi ulang = baris baru; billing_request menunjuk baris historis via FK.
 * Baris yang berlaku = calculated_at terbaru per (entity, tahun, bulan).
 */
@Entity
@Table(name = "tax_calculation_log")
public class TaxCalculationLog {

    @Id @GeneratedValue private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "periode_bulan", nullable = false)
    private Integer periodeBulan;

    @Column(name = "periode_tahun", nullable = false)
    private Integer periodeTahun;

    /** Omzet bruto masa apa adanya (bisa negatif bila ada koreksi lintas masa). */
    @Column(name = "omzet_bruto", nullable = false, precision = 19, scale = 2)
    private BigDecimal omzetBruto;

    /** DPP: bagian omzet yang dikenai tarif (>= 0, setelah pengecualian Rp500jt OP). */
    @Column(name = "omzet_kena_pajak", nullable = false, precision = 19, scale = 2)
    private BigDecimal omzetKenaPajak;

    @Column(name = "tax_rule_id", nullable = false)
    private UUID taxRuleId;

    @Column(name = "pajak_terhitung", nullable = false, precision = 19, scale = 2)
    private BigDecimal pajakTerhitung;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    protected TaxCalculationLog() {}

    TaxCalculationLog(UUID businessEntityId, int periodeBulan, int periodeTahun,
                      BigDecimal omzetBruto, BigDecimal omzetKenaPajak,
                      UUID taxRuleId, BigDecimal pajakTerhitung) {
        this.businessEntityId = businessEntityId;
        this.periodeBulan = periodeBulan;
        this.periodeTahun = periodeTahun;
        this.omzetBruto = omzetBruto;
        this.omzetKenaPajak = omzetKenaPajak;
        this.taxRuleId = taxRuleId;
        this.pajakTerhitung = pajakTerhitung;
    }

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public Integer getPeriodeBulan() { return periodeBulan; }
    public Integer getPeriodeTahun() { return periodeTahun; }
    public BigDecimal getOmzetBruto() { return omzetBruto; }
    public BigDecimal getOmzetKenaPajak() { return omzetKenaPajak; }
    public UUID getTaxRuleId() { return taxRuleId; }
    public BigDecimal getPajakTerhitung() { return pajakTerhitung; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
