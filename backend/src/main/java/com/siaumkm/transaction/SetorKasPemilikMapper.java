package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Setor Kas Pemilik" (BR-B3-01) — pemilik menyetor uang pribadi ke usaha.
 * Debit: Kas/Bank, Kredit: Modal Pemilik.
 */
@Component
class SetorKasPemilikMapper extends AbstractJournalRuleMapper {

    SetorKasPemilikMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "SETOR_KAS_PEMILIK";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Setor Kas Pemilik");

        je.addLine(new JournalLine(accounts.akunDana(request.metode()), jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.MODAL_PEMILIK), BigDecimal.ZERO, jumlah));
        return je;
    }
}
