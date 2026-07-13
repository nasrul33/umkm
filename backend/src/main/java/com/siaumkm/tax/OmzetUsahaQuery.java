package com.siaumkm.tax;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SATU-SATUNYA definisi "peredaran bruto usaha" dari pembukuan — dipakai
 * kalkulasi PPh masa bulanan (BR-B5-01) DAN agregasi tahunan Pasal 58
 * (SRS-B5-02) agar keduanya tidak pernah drift: SUM(kredit - debit) akun
 * PENDAPATAN ber-flag is_omzet_usaha, jurnal POSTED, per tanggal transaksi.
 * Jurnal pembalik otomatis tertangani (mendebit akun pendapatan).
 */
@Component
public class OmzetUsahaQuery {

    private final JdbcTemplate jdbcTemplate;

    public OmzetUsahaQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Omzet usaha pada rentang [dariInklusif, sampaiEksklusif). Bisa negatif. */
    public BigDecimal hitung(LocalDate dariInklusif, LocalDate sampaiEksklusif) {
        BigDecimal hasil = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(jl.kredit - jl.debit), 0)
            FROM journal_line jl
            JOIN chart_of_account coa ON coa.id = jl.chart_of_account_id
            JOIN journal_entry je ON je.id = jl.journal_entry_id
            WHERE coa.tipe = 'PENDAPATAN'
              AND coa.is_omzet_usaha
              AND je.status = 'POSTED'
              AND je.tanggal_transaksi >= ? AND je.tanggal_transaksi < ?
            """, BigDecimal.class, dariInklusif, sampaiEksklusif);
        return hasil.setScale(2, RoundingMode.HALF_UP);
    }
}
