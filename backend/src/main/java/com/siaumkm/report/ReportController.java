package com.siaumkm.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/** SRS-B4: endpoint privat laporan keuangan (di bawah /app/**). */
@RestController
@RequestMapping("/app/reports")
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
