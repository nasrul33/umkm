package com.siaumkm.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * BR-B3-07 / Aturan Emas #2: jurnal POSTED immutable — koreksi HANYA lewat
 * jurnal pembalik. Keputusan desain (konsultasi accounting-engine-architect):
 * - Pembalik lahir langsung POSTED (turunan mekanis 1:1, tidak ada yang perlu
 *   diedit manusia; fase DRAFT justru membuka jendela manipulasi).
 * - Tanggal pembalik = HARI INI, bukan tanggal sumber — backdating menulis
 *   ulang omzet periode yang mungkin sudah dilaporkan (dasar PPh Final).
 * - Pembalik tidak bisa dibalik lagi; kalau pembaliknya yang keliru, catat
 *   ulang transaksi via wizard sebagai jurnal normal baru.
 * - Pembalikan ganda ditolak di service (pesan ramah) DAN oleh partial unique
 *   index uq_journal_entry_reversal_of (penegakan race-safe di database).
 */
@Service
public class JournalPostingService {

    private final JournalEntryRepository journalEntryRepository;

    public JournalPostingService(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional
    public JournalEntry post(UUID id) {
        JournalEntry je = cari(id);
        if (je.getStatus() == JournalEntry.Status.POSTED) {
            throw new IllegalStateException(
                "Jurnal " + je.getNomorJurnal() + " sudah POSTED.");
        }
        je.setStatus(JournalEntry.Status.POSTED);
        je.setPostedAt(Instant.now());
        JournalEntry saved = journalEntryRepository.save(je);
        // open-in-view=false: inisialisasi lines di dalam transaksi agar
        // respons JSON controller tidak gagal LazyInitialization.
        saved.getLines().size();
        return saved;
    }

    @Transactional
    public JournalEntry balikkan(UUID id, UUID createdBy) {
        JournalEntry sumber = cari(id);

        if (sumber.getStatus() != JournalEntry.Status.POSTED) {
            throw new IllegalStateException(
                "Hanya jurnal POSTED yang dapat dibalik — jurnal DRAFT cukup diubah/dihapus langsung.");
        }
        if (sumber.getReversalOfId() != null) {
            throw new IllegalStateException(
                "Jurnal pembalik tidak dapat dibalik lagi — catat ulang transaksi via wizard sebagai jurnal baru.");
        }
        if (journalEntryRepository.existsByReversalOfId(id)) {
            throw new IllegalStateException(
                "Jurnal " + sumber.getNomorJurnal() + " sudah pernah dibalik.");
        }

        JournalEntry pembalik = new JournalEntry();
        pembalik.setNomorJurnal(NomorJurnal.generate("JR"));
        pembalik.setTanggalTransaksi(LocalDate.now());
        pembalik.setKeterangan("Pembalik: " + sumber.getNomorJurnal());
        pembalik.setMetodePembayaran(sumber.getMetodePembayaran());
        pembalik.setCreatedBy(createdBy);
        pembalik.setReversalOfId(sumber.getId());
        pembalik.setStatus(JournalEntry.Status.POSTED); // INSERT ber-status POSTED valid; trigger hanya menjaga UPDATE/DELETE
        pembalik.setPostedAt(Instant.now());

        for (JournalLine line : sumber.getLines()) {
            // Debit/kredit ditukar apa adanya (sudah scale 2 — tanpa rounding ulang);
            // product_id WAJIB ikut agar laporan margin per produk (B6) terkoreksi.
            JournalLine cermin = new JournalLine(line.getChartOfAccountId(), line.getKredit(), line.getDebit());
            cermin.setProductId(line.getProductId());
            pembalik.addLine(cermin);
        }

        JournalValidator.validateBalance(pembalik);
        return journalEntryRepository.save(pembalik);
    }

    private JournalEntry cari(UUID id) {
        return journalEntryRepository.findById(id).orElseThrow(
            () -> new NoSuchElementException("Jurnal " + id + " tidak ditemukan."));
    }
}
