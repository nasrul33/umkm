package com.siaumkm.transaction;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SRS-B3-01: menerjemahkan wizard bahasa awam ("Jual Barang/Jasa", dst.) menjadi
 * jurnal double-entry. Setiap template punya JournalRuleMapper sendiri yang
 * ditemukan otomatis via Spring — menambah template baru = menambah @Component
 * baru, BUKAN menambah cabang kondisional di sini.
 *
 * Lihat CLAUDE.md Aturan Emas #1: BigDecimal + RoundingMode eksplisit, selalu.
 */
@Service
public class TransactionWizardService {

    private final Map<String, JournalRuleMapper> mappers;

    public TransactionWizardService(List<JournalRuleMapper> mapperList) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toUnmodifiableMap(JournalRuleMapper::kodeTemplate, m -> m));
    }

    public JournalEntry buatJurnal(TransactionRequest request, UUID createdBy) {
        JournalRuleMapper mapper = mappers.get(request.kodeTemplate());
        if (mapper == null) {
            throw new UnsupportedOperationException(
                "Template " + request.kodeTemplate() + " belum diimplementasikan — " +
                "tambahkan JournalRuleMapper baru, jangan hardcode di controller.");
        }
        JournalEntry je = mapper.map(request, createdBy);
        JournalValidator.validateBalance(je);
        return je;
    }
}
