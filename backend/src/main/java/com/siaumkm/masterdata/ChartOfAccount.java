package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.util.UUID;

/** SRS-B2-01: seed standar UMKM via Flyway (lihat V1__init_schema.sql). */
@Entity
@Table(name = "chart_of_account")
public class ChartOfAccount {

    @Id @GeneratedValue private UUID id;

    @Column(name = "kode_akun", nullable = false, unique = true, length = 20)
    private String kodeAkun;

    @Column(name = "nama_akun", nullable = false)
    private String namaAkun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipeAkun tipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_behavior")
    private PerilakuBiaya costBehavior; // SRS-B6-02: klasifikasi FIXED/VARIABLE utk BEP

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UUID getId() { return id; }
    public String getKodeAkun() { return kodeAkun; }
    public String getNamaAkun() { return namaAkun; }
    public TipeAkun getTipe() { return tipe; }
    public PerilakuBiaya getCostBehavior() { return costBehavior; }
    public void setCostBehavior(PerilakuBiaya v) { this.costBehavior = v; }

    public enum TipeAkun { ASET, KEWAJIBAN, MODAL, PENDAPATAN, BEBAN }
    public enum PerilakuBiaya { FIXED, VARIABLE }
}
