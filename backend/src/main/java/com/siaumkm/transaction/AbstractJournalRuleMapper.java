package com.siaumkm.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Kerangka bersama para mapper: header jurnal + pembulatan uang (Aturan Emas #1). */
abstract class AbstractJournalRuleMapper implements JournalRuleMapper {

    protected final AccountResolver accounts;

    protected AbstractJournalRuleMapper(AccountResolver accounts) {
        this.accounts = accounts;
    }

    protected JournalEntry entriBaru(TransactionRequest request, UUID createdBy, String keterangan) {
        JournalEntry je = new JournalEntry();
        je.setNomorJurnal(generateNomorJurnal());
        je.setTanggalTransaksi(request.tanggal());
        je.setKeterangan(keterangan);
        je.setMetodePembayaran(request.metode());
        je.setCreatedBy(createdBy);
        return je;
    }

    protected BigDecimal jumlahBersih(TransactionRequest request) {
        if (request.jumlah() == null || request.jumlah().signum() <= 0) {
            throw new IllegalArgumentException("Jumlah transaksi harus lebih besar dari 0");
        }
        return request.jumlah().setScale(2, RoundingMode.HALF_UP);
    }

    private String generateNomorJurnal() {
        // 21 karakter, muat di kolom VARCHAR(30); suffix acak mencegah tabrakan
        // unique constraint saat dua transaksi tercatat pada milidetik yang sama.
        return "JU-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
