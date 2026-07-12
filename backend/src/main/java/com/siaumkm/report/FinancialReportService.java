package com.siaumkm.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * SRS-B4-01: laporan keuangan dihasilkan dari materialized view atas journal_entry
 * (single source of truth) — TIDAK ada tabel ringkasan laporan yang di-maintain manual.
 * Lihat schema.sql untuk definisi vw_balance_sheet, vw_income_statement, vw_product_margin
 * (sudah tervalidasi jalan di PostgreSQL 18 asli).
 *
 * Refresh materialized view dijadwalkan terpisah (Quartz job) — lihat NFR-07/scheduling,
 * bukan di-refresh sinkron setiap request (mahal untuk volume transaksi tinggi).
 */
@Service
public class FinancialReportService {

    private final JdbcTemplate jdbcTemplate;

    public FinancialReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccountBalanceRow> getBalanceSheet() {
        return jdbcTemplate.query(
            "SELECT tipe, kode_akun, nama_akun, saldo FROM vw_balance_sheet ORDER BY kode_akun",
            (rs, i) -> new AccountBalanceRow(
                rs.getString("tipe"), rs.getString("kode_akun"),
                rs.getString("nama_akun"), rs.getBigDecimal("saldo")));
    }

    public List<AccountBalanceRow> getIncomeStatement() {
        return jdbcTemplate.query(
            "SELECT tipe, kode_akun, nama_akun, saldo FROM vw_income_statement ORDER BY kode_akun",
            (rs, i) -> new AccountBalanceRow(
                rs.getString("tipe"), rs.getString("kode_akun"),
                rs.getString("nama_akun"), rs.getBigDecimal("saldo")));
    }

    /** SRS-B4-08: ringkasan untuk dashboard (dikonsumsi Dashboard.vue). */
    public Map<String, Object> getDashboardSummary() {
        BigDecimal saldoKas = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(saldo), 0) FROM vw_balance_sheet WHERE kode_akun IN ('1000','1100')",
            BigDecimal.class);

        // periode bertipe DATE (V4) — perbandingan bebas timezone sesi.
        BigDecimal labaBulanIni = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(CASE WHEN tipe = 'PENDAPATAN' THEN saldo ELSE -saldo END), 0)
            FROM vw_income_statement WHERE periode = date_trunc('month', CURRENT_DATE)::date
            """, BigDecimal.class);

        BigDecimal piutangJatuhTempo = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(saldo), 0) FROM vw_balance_sheet WHERE kode_akun = '1200'",
            BigDecimal.class);

        return Map.of(
            "saldoKas", saldoKas,
            "labaBulanIni", labaBulanIni,
            "piutangJatuhTempo", piutangJatuhTempo
        );
    }
}
