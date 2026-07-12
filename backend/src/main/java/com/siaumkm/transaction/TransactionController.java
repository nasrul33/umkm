package com.siaumkm.transaction;

import com.siaumkm.masterdata.ChartOfAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/**
 * SRS-B3-01: endpoint yang dipanggil TransactionWizard.vue. Pemetaan kodeTemplate
 * ke akun debit/kredit di sini masih contoh sederhana untuk template
 * JUAL_BARANG_JASA — untuk template lain (TERIMA_PEMBAYARAN, BELI_BAHAN, BAYAR_BIAYA),
 * tambahkan strategi baru di TransactionWizardService, JANGAN tambah if/else di controller.
 */
@RestController
@RequestMapping("/app/transaksi")
public class TransactionController {

    private final TransactionWizardService wizardService;
    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    public TransactionController(TransactionWizardService wizardService,
                                  JournalEntryRepository journalEntryRepository,
                                  ChartOfAccountRepository chartOfAccountRepository) {
        this.wizardService = wizardService;
        this.journalEntryRepository = journalEntryRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
    }

    @PostMapping
    public ResponseEntity<JournalEntry> catatTransaksi(@RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal UUID currentUserId) {
        UUID kasAccountId = chartOfAccountRepository.findByKodeAkun("1000")
                .orElseThrow(() -> new IllegalStateException("Akun Kas (1000) belum ter-seed")).getId();
        UUID pendapatanAccountId = chartOfAccountRepository.findByKodeAkun("4000")
                .orElseThrow(() -> new IllegalStateException("Akun Pendapatan Usaha (4000) belum ter-seed")).getId();

        JournalEntry je = switch (request.kodeTemplate()) {
            case "JUAL_BARANG_JASA" -> wizardService.buatJurnalPenjualan(
                    kasAccountId, pendapatanAccountId, request.jumlah(), request.tanggal(), currentUserId);
            default -> throw new UnsupportedOperationException(
                    "Template " + request.kodeTemplate() + " belum diimplementasikan — " +
                    "tambahkan method baru di TransactionWizardService, jangan hardcode di sini.");
        };

        return ResponseEntity.ok(journalEntryRepository.save(je));
    }
}
