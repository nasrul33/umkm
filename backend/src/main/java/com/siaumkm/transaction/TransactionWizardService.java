package com.siaumkm.transaction;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SRS-B3-01: menerjemahkan wizard bahasa awam ("Jual Barang/Jasa", dst.) menjadi
 * jurnal double-entry. Setiap template punya strategi pemetaan akun sendiri —
 * jangan taruh if/else raksasa di sini; tambah TransactionTemplate baru = tambah
 * implementasi JournalRuleMapper baru, bukan menambah cabang kondisional.
 *
 * Lihat CLAUDE.md Aturan Emas #1: BigDecimal + RoundingMode eksplisit, selalu.
 */
@Service
public class TransactionWizardService {

    /**
     * Contoh implementasi untuk template "JUAL_BARANG_JASA".
     * Debit: Kas/Bank (sesuai metode pembayaran) — Kredit: Pendapatan Usaha.
     */
    public JournalEntry buatJurnalPenjualan(UUID kasAccountId, UUID pendapatanAccountId,
                                             BigDecimal jumlah, LocalDate tanggal, UUID createdBy) {
        BigDecimal jumlahBersih = jumlah.setScale(2, RoundingMode.HALF_UP);

        JournalEntry je = new JournalEntry();
        je.setNomorJurnal(generateNomorJurnal());
        je.setTanggalTransaksi(tanggal);
        je.setKeterangan("Jual Barang/Jasa");
        je.setCreatedBy(createdBy);

        je.addLine(new JournalLine(kasAccountId, jumlahBersih, BigDecimal.ZERO));
        je.addLine(new JournalLine(pendapatanAccountId, BigDecimal.ZERO, jumlahBersih));

        validateBalance(je);
        return je;
    }

    /**
     * SRS-B3-01 wajib: setiap jurnal harus balance (total debit = total kredit)
     * SEBELUM insert — validasi ini adalah lapisan pertama, trigger database
     * (prevent_update_posted_journal) adalah lapisan kedua untuk immutability
     * setelah POSTED, bukan pengganti validasi balance ini.
     */
    private void validateBalance(JournalEntry je) {
        BigDecimal totalDebit = je.getLines().stream()
                .map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalKredit = je.getLines().stream()
                .map(JournalLine::getKredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalKredit) != 0) {
            throw new IllegalStateException(
                "Jurnal tidak balance: debit=" + totalDebit + " kredit=" + totalKredit);
        }
    }

    private String generateNomorJurnal() {
        return "JU-" + System.currentTimeMillis();
    }
}
