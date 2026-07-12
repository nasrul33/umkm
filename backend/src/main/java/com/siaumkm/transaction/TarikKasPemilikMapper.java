package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Tarik Kas Pemilik" / prive (BR-B3-01) — pemilik mengambil uang usaha
 * untuk keperluan pribadi. Debit: Modal Pemilik, Kredit: Kas/Bank.
 */
@Component
class TarikKasPemilikMapper extends AbstractJournalRuleMapper {

    TarikKasPemilikMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "TARIK_KAS_PEMILIK";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Tarik Kas Pemilik");

        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.MODAL_PEMILIK), jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(accounts.akunDana(request.metode()), BigDecimal.ZERO, jumlah));
        return je;
    }
}
