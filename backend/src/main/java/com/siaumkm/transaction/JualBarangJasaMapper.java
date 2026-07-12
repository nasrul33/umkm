package com.siaumkm.transaction;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Jual Barang/Jasa" — Debit: Kas/Bank (atau Piutang Usaha bila penjualan kredit),
 * Kredit: Pendapatan Usaha.
 */
@Component
class JualBarangJasaMapper extends AbstractJournalRuleMapper {

    JualBarangJasaMapper(AccountResolver accounts) {
        super(accounts);
    }

    @Override
    public String kodeTemplate() {
        return "JUAL_BARANG_JASA";
    }

    @Override
    public JournalEntry map(TransactionRequest request, UUID createdBy) {
        BigDecimal jumlah = jumlahBersih(request);
        JournalEntry je = entriBaru(request, createdBy, "Jual Barang/Jasa");

        UUID debitAccount = request.metode() == MetodePembayaran.RECEIVABLE
                ? accounts.idByKode(AccountResolver.PIUTANG_USAHA)
                : accounts.akunDana(request.metode());

        je.addLine(new JournalLine(debitAccount, jumlah, BigDecimal.ZERO));
        je.addLine(new JournalLine(accounts.idByKode(AccountResolver.PENDAPATAN_USAHA), BigDecimal.ZERO, jumlah));
        return je;
    }
}
