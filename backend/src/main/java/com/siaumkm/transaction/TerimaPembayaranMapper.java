package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Terima Pembayaran" (pelunasan piutang pelanggan) — Debit: Kas/Bank,
 * Kredit: Piutang Usaha.
 */
@Component
class TerimaPembayaranMapper extends AbstractJournalRuleMapper {

    TerimaPembayaranMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "TERIMA_PEMBAYARAN";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Terima Pembayaran");

        je.addLine(new JournalLine(accounts.akunDana(request.metode()), jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.PIUTANG_USAHA), BigDecimal.ZERO, jumlah));
        return je;
    }
}
