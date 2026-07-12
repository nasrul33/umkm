package com.siaumkm.report;

import java.math.BigDecimal;

/** SRS-B4-01: baris hasil query vw_balance_sheet / vw_income_statement. */
public record AccountBalanceRow(String tipe, String kodeAkun, String namaAkun, BigDecimal saldo) {}
