package com.siaumkm.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload dari TransactionWizard.vue — SRS-B3-01. */
public record TransactionRequest(String kodeTemplate, BigDecimal jumlah, String metodePembayaran, LocalDate tanggal) {}
