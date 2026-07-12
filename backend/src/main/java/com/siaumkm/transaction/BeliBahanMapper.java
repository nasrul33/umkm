package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Beli Bahan/Barang" — Debit: Persediaan, Kredit: Kas/Bank
 * (atau Hutang Usaha bila pembelian kredit).
 */
@Component
class BeliBahanMapper extends AbstractJournalRuleMapper {

    BeliBahanMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "BELI_BAHAN";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Beli Bahan/Barang");

        UUID kreditAccount = request.metode() == MetodePembayaran.PAYABLE
                ? accounts.idByKode(AccountResolver.HUTANG_USAHA)
                : accounts.akunDana(request.metode());

        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.PERSEDIAAN), jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(kreditAccount, BigDecimal.ZERO, jumlah));
        return je;
    }
}
