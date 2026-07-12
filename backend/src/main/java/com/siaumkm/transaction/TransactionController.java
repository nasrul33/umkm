package com.siaumkm.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final JournalPostingService postingService;
    private final JournalEntryRepository journalEntryRepository;

    public TransactionController(TransactionWizardService wizardService,
                                  JournalPostingService postingService,
                                  JournalEntryRepository journalEntryRepository) {
        this.wizardService = wizardService;
        this.postingService = postingService;
        this.journalEntryRepository = journalEntryRepository;
    }

    @PostMapping
    public ResponseEntity<JournalEntry> catatTransaksi(@RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal UUID currentUserId) {
        JournalEntry je = wizardService.buatJurnal(request, currentUserId);
        return ResponseEntity.ok(journalEntryRepository.save(je));
    }

    /** Posting = penyelesaian alur input harian — STAFF boleh (NFR-10). */
    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public ResponseEntity<JournalEntry> post(@PathVariable UUID id) {
        return ResponseEntity.ok(postingService.post(id));
    }

    /**
     * BR-B3-07: koreksi jurnal POSTED. Hanya OWNER — pembalikan mengubah angka
     * buku final dan reversal penjualan adalah pola klasik fraud oleh staf.
     */
    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<JournalEntry> reverse(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UUID currentUserId) {
        return ResponseEntity.ok(postingService.balikkan(id, currentUserId));
    }
}
