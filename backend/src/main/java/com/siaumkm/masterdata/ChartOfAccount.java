package com.siaumkm.masterdata;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    // NAMED_ENUM: kolom PG bertipe enum bernama (tipe_akun/perilaku_biaya) —
    // tanpa ini Hibernate mengikat parameter sebagai varchar dan PG menolak.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TipeAkun tipe;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cost_behavior")
    private PerilakuBiaya costBehavior; // SRS-B6-02: klasifikasi FIXED/VARIABLE utk BEP

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // SRS-B5-02: hanya akun pendapatan ber-flag ini yang masuk peredaran bruto
    // agregasi omzet — pendapatan yang dikenai PPh final tersendiri (bunga,
    // sewa 4(2)) atau pekerjaan bebas wajib FALSE (PMK 168/2023).
    @Column(name = "is_omzet_usaha", nullable = false)
    private boolean isOmzetUsaha = true;

    // BR-B4-03: akun kas/setara kas (dasar laporan arus kas & saldo kas
    // dashboard) — single source of truth, bukan hardcode kode 1000/1100.
    @Column(name = "is_kas_setara_kas", nullable = false)
    private boolean isKasSetaraKas = false;

    // Klasifikasi arus kas akun NON-kas (CHECK V10: wajib salah satu).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cash_flow_category")
    private KategoriArusKas cashFlowCategory;

    public UUID getId() { return id; }
    public String getKodeAkun() { return kodeAkun; }
    public String getNamaAkun() { return namaAkun; }
    public TipeAkun getTipe() { return tipe; }
    public PerilakuBiaya getCostBehavior() { return costBehavior; }
    public void setCostBehavior(PerilakuBiaya v) { this.costBehavior = v; }
    public boolean isOmzetUsaha() { return isOmzetUsaha; }
    public void setOmzetUsaha(boolean v) { this.isOmzetUsaha = v; }
    public boolean isKasSetaraKas() { return isKasSetaraKas; }
    public void setKasSetaraKas(boolean v) { this.isKasSetaraKas = v; }
    public KategoriArusKas getCashFlowCategory() { return cashFlowCategory; }
    public void setCashFlowCategory(KategoriArusKas v) { this.cashFlowCategory = v; }

    public enum TipeAkun { ASET, KEWAJIBAN, MODAL, PENDAPATAN, BEBAN }
    public enum PerilakuBiaya { FIXED, VARIABLE }
    public enum KategoriArusKas { OPERASI, INVESTASI, PENDANAAN }
}
