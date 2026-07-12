package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** SRS-B2-07: master rekening bank/kas/e-wallet — dipakai wizard transaksi (B3) sebagai sumber/tujuan dana. */
@Entity
@Table(name = "cash_account")
public class CashAccount {
    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, length = 100) private String nama; // 'Kas Tunai', 'BCA 123456', 'GoPay Merchant'
    @Column(nullable = false, length = 30) private String tipe;  // CASH / BANK / EWALLET

    @Column(name = "chart_of_account_id", nullable = false) private UUID chartOfAccountId;

    @Column(name = "saldo_awal", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoAwal = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false) private boolean isActive = true;

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public UUID getChartOfAccountId() { return chartOfAccountId; }
    public void setChartOfAccountId(UUID v) { this.chartOfAccountId = v; }
}
