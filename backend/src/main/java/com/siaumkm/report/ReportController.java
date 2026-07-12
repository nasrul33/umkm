package com.siaumkm.report;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * SRS-B4: endpoint privat laporan keuangan (di bawah /app/**).
 * NFR-10: laporan keuangan adalah data sensitif — hanya OWNER; STAFF adalah
 * peran input transaksi, bukan pembaca laporan.
 */
@RestController
@RequestMapping("/app/reports")
@PreAuthorize("hasRole('OWNER')")
public class ReportController {

    private final FinancialReportService reportService;

    public ReportController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard-summary")
    public Map<String, Object> dashboardSummary() {
        return reportService.getDashboardSummary();
    }

    @GetMapping("/balance-sheet")
    public List<AccountBalanceRow> balanceSheet() {
        return reportService.getBalanceSheet();
    }

    @GetMapping("/income-statement")
    public List<AccountBalanceRow> incomeStatement() {
        return reportService.getIncomeStatement();
    }
}
