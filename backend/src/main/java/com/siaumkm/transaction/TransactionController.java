package com.siaumkm.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/**
 * SRS-B3-01: endpoint yang dipanggil TransactionWizard.vue. Controller ini
 * sengaja tipis — seluruh pemetaan template ke akun debit/kredit ada di
 * JournalRuleMapper masing-masing (lihat TransactionWizardService).
 */
@RestController
@RequestMapping("/app/transaksi")
public class TransactionController {

    private final TransactionWizardService wizardService;
    private final JournalEntryRepository journalEntryRepository;

    public TransactionController(TransactionWizardService wizardService,
                                  JournalEntryRepository journalEntryRepository) {
        this.wizardService = wizardService;
        this.journalEntryRepository = journalEntryRepository;
    }

    @PostMapping
    public ResponseEntity<JournalEntry> catatTransaksi(@RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal UUID currentUserId) {
        JournalEntry je = wizardService.buatJurnal(request, currentUserId);
        return ResponseEntity.ok(journalEntryRepository.save(je));
    }
}
