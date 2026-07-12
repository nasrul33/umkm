package com.siaumkm.tax;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SRS-B5-02: hasil OmzetAggregationJob (Pasal 58 PP 20/2026). Baris di-upsert
 * per (business_entity, tahun); riwayat rekalkulasi terekam di audit hash-chain
 * (trg_audit_aggregated_omzet, V6), bukan tabel riwayat terpisah.
 */
@Entity
@Table(name = "aggregated_omzet")
public class AggregatedOmzet {

    @Id @GeneratedValue private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "periode_tahun", nullable = false)
    private Integer periodeTahun;

    @Column(name = "omzet_entitas_utama", nullable = false, precision = 19, scale = 2)
    private BigDecimal omzetEntitasUtama = BigDecimal.ZERO;

    @Column(name = "omzet_entitas_terkait", nullable = false, precision = 19, scale = 2)
    private BigDecimal omzetEntitasTerkait = BigDecimal.ZERO;

    @Column(name = "omzet_gabungan", nullable = false, precision = 19, scale = 2)
    private BigDecimal omzetGabungan = BigDecimal.ZERO;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public Integer getPeriodeTahun() { return periodeTahun; }
    public BigDecimal getOmzetEntitasUtama() { return omzetEntitasUtama; }
    public BigDecimal getOmzetEntitasTerkait() { return omzetEntitasTerkait; }
    public BigDecimal getOmzetGabungan() { return omzetGabungan; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
