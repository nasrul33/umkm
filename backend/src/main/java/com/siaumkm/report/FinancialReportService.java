package com.siaumkm.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SRS-B4-01: laporan keuangan dihasilkan dari materialized view atas journal_entry
 * (single source of truth) — TIDAK ada tabel ringkasan laporan yang di-maintain manual.
 * vw_balance_sheet, vw_income_statement, vw_cash_flow, vw_equity_change (V1/V4/V10).
 *
 * Refresh materialized view dijadwalkan terpisah (Quartz job) — lihat NFR-07/scheduling,
 * bukan di-refresh sinkron setiap request (mahal untuk volume transaksi tinggi).
 *
 * Saldo kumulatif (kas awal periode, modal awal) dirakit DI SINI dari arus per
 * bulan (BigDecimal) — jangan simpan saldo berjalan di MV (refresh parsial merusaknya).
 */
@Service
public class FinancialReportService {

    public record CashFlowCategoryRow(String kategori, BigDecimal arusMasuk,
                                       BigDecimal arusKeluar, BigDecimal arusBersih) {}
    public record CashFlowReport(int tahun, BigDecimal saldoKasAwal,
                                  List<CashFlowCategoryRow> kategori,
                                  BigDecimal saldoKasAkhir) {}
    public record EquityChangeReport(int tahun, BigDecimal modalAwal, BigDecimal setoran,
                                      BigDecimal prive, BigDecimal labaRugi, BigDecimal modalAkhir) {}

    private final JdbcTemplate jdbcTemplate;

    public FinancialReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccountBalanceRow> getBalanceSheet() {
        List<AccountBalanceRow> rows = new ArrayList<>(jdbcTemplate.query(
            "SELECT tipe, kode_akun, nama_akun, saldo FROM vw_balance_sheet ORDER BY kode_akun",
            (rs, i) -> new AccountBalanceRow(
                rs.getString("tipe"), rs.getString("kode_akun"),
                rs.getString("nama_akun"), rs.getBigDecimal("saldo"))));

        // Sistem tanpa jurnal penutup (by design utk UMKM): tanpa baris derivatif
        // ini sisi MODAL kekurangan akumulasi laba dan neraca TIDAK balance.
        // Konvensi tanda mengikuti vw_balance_sheet (debit - kredit): saldo
        // kredit tampil negatif, dibalik di lapisan penyajian — sehingga
        // SUM(seluruh baris) == 0 saat neraca balance.
        BigDecimal saldoLaba = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(saldo), 0) FROM vw_income_statement", BigDecimal.class);
        rows.add(new AccountBalanceRow("MODAL", "3900", "Saldo Laba (akumulasi)", saldoLaba.negate()));
        return rows;
    }

    public List<AccountBalanceRow> getIncomeStatement() {
        return jdbcTemplate.query(
            "SELECT tipe, kode_akun, nama_akun, saldo FROM vw_income_statement ORDER BY kode_akun",
            (rs, i) -> new AccountBalanceRow(
                rs.getString("tipe"), rs.getString("kode_akun"),
                rs.getString("nama_akun"), rs.getBigDecimal("saldo")));
    }

    /** BR-B4-03: arus kas metode LANGSUNG per tahun (lihat V10 utk mekanikanya). */
    public CashFlowReport getCashFlow(int tahun) {
        LocalDate awal = LocalDate.of(tahun, 1, 1);
        LocalDate akhir = LocalDate.of(tahun + 1, 1, 1);

        BigDecimal saldoAwal = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(arus_bersih), 0) FROM vw_cash_flow WHERE periode < ?",
            BigDecimal.class, awal);

        List<CashFlowCategoryRow> kategori = jdbcTemplate.query("""
            SELECT kategori, SUM(arus_masuk) AS masuk, SUM(arus_keluar) AS keluar,
                   SUM(arus_bersih) AS bersih
            FROM vw_cash_flow WHERE periode >= ? AND periode < ?
            GROUP BY kategori ORDER BY kategori
            """,
            (rs, i) -> new CashFlowCategoryRow(rs.getString("kategori"),
                rs.getBigDecimal("masuk"), rs.getBigDecimal("keluar"), rs.getBigDecimal("bersih")),
            awal, akhir);

        BigDecimal bersihTahun = kategori.stream()
                .map(CashFlowCategoryRow::arusBersih)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashFlowReport(tahun, saldoAwal, kategori, saldoAwal.add(bersihTahun));
    }

    /** BR-B4-04 (SAK EMKM): modal akhir = modal awal + setoran - prive +/- laba. */
    public EquityChangeReport getEquityChange(int tahun) {
        LocalDate awal = LocalDate.of(tahun, 1, 1);
        LocalDate akhir = LocalDate.of(tahun + 1, 1, 1);

        // Tanpa jurnal penutup: modal awal = kumulatif (setoran - prive + laba)
        // seluruh periode sebelum tahun berjalan.
        BigDecimal modalAwal = jdbcTemplate.queryForObject("""
            SELECT COALESCE((SELECT SUM(setoran_bersih - prive_bersih)
                             FROM vw_equity_change WHERE periode < ?), 0)
                 + COALESCE((SELECT SUM(saldo)
                             FROM vw_income_statement WHERE periode < ?), 0)
            """, BigDecimal.class, awal, awal);

        Map<String, Object> mutasi = jdbcTemplate.queryForMap("""
            SELECT COALESCE(SUM(setoran_bersih), 0) AS setoran,
                   COALESCE(SUM(prive_bersih), 0) AS prive
            FROM vw_equity_change WHERE periode >= ? AND periode < ?
            """, awal, akhir);
        BigDecimal setoran = (BigDecimal) mutasi.get("setoran");
        BigDecimal prive = (BigDecimal) mutasi.get("prive");

        BigDecimal laba = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(saldo), 0) FROM vw_income_statement WHERE periode >= ? AND periode < ?",
            BigDecimal.class, awal, akhir);

        return new EquityChangeReport(tahun, modalAwal, setoran, prive, laba,
                modalAwal.add(setoran).subtract(prive).add(laba));
    }

    /** SRS-B4-08: ringkasan untuk dashboard (dikonsumsi Dashboard.vue). */
    public Map<String, Object> getDashboardSummary() {
        // Akun kas dari flag is_kas_setara_kas (V10) — bukan hardcode 1000/1100.
        BigDecimal saldoKas = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(vb.saldo), 0) FROM vw_balance_sheet vb
            JOIN chart_of_account c ON c.kode_akun = vb.kode_akun
            WHERE c.is_kas_setara_kas
            """, BigDecimal.class);

        // periode bertipe DATE (V4) — perbandingan bebas timezone sesi.
        // saldo view = kredit - debit: pendapatan positif, beban sudah NEGATIF —
        // laba = SUM(saldo) polos. (Rumus lama CASE ... ELSE -saldo salah tanda:
        // justru MENAMBAHKAN beban ke laba.)
        BigDecimal labaBulanIni = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(saldo), 0)
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
