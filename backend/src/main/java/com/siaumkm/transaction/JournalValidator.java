package com.siaumkm.transaction;

import java.math.BigDecimal;

/**
 * SRS-B3-01 wajib: setiap jurnal harus balance (total debit = total kredit)
 * SEBELUM insert — lapisan pertama; trigger database (prevent_update_posted_journal
 * + trg_prevent_update_line_posted_journal) adalah lapisan kedua untuk
 * immutability setelah POSTED, bukan pengganti validasi ini.
 */
final class JournalValidator {

    private JournalValidator() {}

    static void validateBalance(JournalEntry je) {
        BigDecimal totalDebit = je.getLines().stream()
                .map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalKredit = je.getLines().stream()
                .map(JournalLine::getKredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalKredit) != 0) {
            throw new IllegalStateException(
                "Jurnal tidak balance: debit=" + totalDebit + " kredit=" + totalKredit);
        }
    }
}
