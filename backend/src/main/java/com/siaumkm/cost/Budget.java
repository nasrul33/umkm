package com.siaumkm.cost;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** SRS-B6-03: anggaran per akun per periode, dibandingkan realisasi (varians). */
@Entity
@Table(name = "budget")
public class Budget {
    @Id @GeneratedValue private UUID id;
    @Column(name = "chart_of_account_id", nullable = false) private UUID chartOfAccountId;
    @Column(name = "periode_bulan", nullable = false) private Integer periodeBulan;
    @Column(name = "periode_tahun", nullable = false) private Integer periodeTahun;
    @Column(name = "nilai_anggaran", nullable = false, precision = 19, scale = 2) private BigDecimal nilaiAnggaran;

    public UUID getId() { return id; }
    public BigDecimal getNilaiAnggaran() { return nilaiAnggaran; }
    public void setNilaiAnggaran(BigDecimal v) { this.nilaiAnggaran = v; }
}
