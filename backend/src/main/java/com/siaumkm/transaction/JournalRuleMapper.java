package com.siaumkm.transaction;

import java.util.UUID;

/**
 * SRS-B3-01: satu implementasi per kode template wizard bahasa awam.
 * Menambah template baru = menambah @Component baru yang mengimplementasi
 * interface ini — TransactionWizardService menemukannya otomatis via Spring,
 * TIDAK perlu (dan tidak boleh) menambah cabang if/else di service/controller.
 */
public interface JournalRuleMapper {

    /** Kode template yang dikirim TransactionWizard.vue, mis. "JUAL_BARANG_JASA". */
    String kodeTemplate();

    /** Terjemahkan input awam menjadi jurnal double-entry (belum tersimpan). */
    JournalEntry map(TransactionRequest request, UUID createdBy);
}
