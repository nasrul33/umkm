package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Bayar Biaya Operasional" — Debit: Biaya Operasional, Kredit: Kas/Bank.
 */
@Component
class BayarBiayaMapper extends AbstractJournalRuleMapper {

    BayarBiayaMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "BAYAR_BIAYA";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Bayar Biaya Operasional");

        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.BIAYA_OPERASIONAL), jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(accounts.akunDana(request.metode()), BigDecimal.ZERO, jumlah));
        return je;
    }
}
