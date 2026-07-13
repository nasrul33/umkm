package com.siaumkm.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * BR-B4-06: unduh laporan keuangan. OWNER-only (NFR-10) — laporan lengkap
 * adalah data sensitif; STAFF adalah peran input.
 */
@RestController
@RequestMapping("/app/reports/export")
@PreAuthorize("hasRole('OWNER')")
public class ReportExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportExportService exportService;

    public ReportExportController(ReportExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) Integer tahun) {
        int t = tahunEfektif(tahun);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"laporan-keuangan-" + t + ".xlsx\"")
                .body(exportService.exportExcel(t));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) Integer tahun) {
        int t = tahunEfektif(tahun);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"laporan-keuangan-" + t + ".pdf\"")
                .body(exportService.exportPdf(t));
    }

    private int tahunEfektif(Integer tahun) {
        return tahun != null ? tahun : LocalDate.now().getYear();
    }
}
