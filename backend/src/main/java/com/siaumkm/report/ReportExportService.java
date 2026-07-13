package com.siaumkm.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.siaumkm.report.FinancialReportService.CashFlowCategoryRow;
import com.siaumkm.report.FinancialReportService.CashFlowReport;
import com.siaumkm.report.FinancialReportService.EquityChangeReport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * BR-B4-06: ekspor laporan keuangan ke Excel & PDF — format yang diterima
 * bank/lembaga pembiayaan (BR-B4-07). Lapisan PENYAJIAN murni: seluruh angka
 * datang dari FinancialReportService (BigDecimal); konversi ke double hanya
 * saat menulis sel Excel (aman — nilai UMKM jauh di bawah batas presisi
 * double 2^53; komputasi uang tidak pernah terjadi di kelas ini).
 */
@Service
public class ReportExportService {

    private static final Locale ID = Locale.of("id", "ID");

    private final FinancialReportService reportService;

    public ReportExportService(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    // ---------------------------------------------------------------- Excel

    public byte[] exportExcel(int tahun) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle uang = wb.createCellStyle();
            uang.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            sheetSaldo(wb, uang, "Neraca", reportService.getBalanceSheet());
            sheetSaldo(wb, uang, "Laba Rugi", reportService.getIncomeStatement());
            sheetArusKas(wb, uang, reportService.getCashFlow(tahun));
            sheetPerubahanModal(wb, uang, reportService.getEquityChange(tahun));

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal menulis workbook Excel", e);
        }
    }

    private void sheetSaldo(XSSFWorkbook wb, CellStyle uang, String nama,
                            List<AccountBalanceRow> rows) {
        Sheet sheet = wb.createSheet(nama);
        header(sheet, "Tipe", "Kode Akun", "Nama Akun", "Saldo");
        int r = 1;
        for (AccountBalanceRow row : rows) {
            Row baris = sheet.createRow(r++);
            baris.createCell(0).setCellValue(row.tipe());
            baris.createCell(1).setCellValue(row.kodeAkun());
            baris.createCell(2).setCellValue(row.namaAkun());
            selUang(baris, 3, uang, row.saldo());
        }
        for (int c = 0; c <= 3; c++) sheet.autoSizeColumn(c);
    }

    private void sheetArusKas(XSSFWorkbook wb, CellStyle uang, CashFlowReport laporan) {
        Sheet sheet = wb.createSheet("Arus Kas " + laporan.tahun());
        header(sheet, "Kategori", "Arus Masuk", "Arus Keluar", "Arus Bersih");
        int r = 1;
        Row awal = sheet.createRow(r++);
        awal.createCell(0).setCellValue("Saldo Kas Awal");
        selUang(awal, 3, uang, laporan.saldoKasAwal());
        for (CashFlowCategoryRow row : laporan.kategori()) {
            Row baris = sheet.createRow(r++);
            baris.createCell(0).setCellValue(row.kategori());
            selUang(baris, 1, uang, row.arusMasuk());
            selUang(baris, 2, uang, row.arusKeluar());
            selUang(baris, 3, uang, row.arusBersih());
        }
        Row akhir = sheet.createRow(r);
        akhir.createCell(0).setCellValue("Saldo Kas Akhir");
        selUang(akhir, 3, uang, laporan.saldoKasAkhir());
        for (int c = 0; c <= 3; c++) sheet.autoSizeColumn(c);
    }

    private void sheetPerubahanModal(XSSFWorkbook wb, CellStyle uang, EquityChangeReport laporan) {
        Sheet sheet = wb.createSheet("Perubahan Modal " + laporan.tahun());
        String[][] baris = {
            {"Modal Awal", null}, {"Setoran Modal", null}, {"Prive (Pengambilan)", null},
            {"Laba (Rugi) Periode", null}, {"Modal Akhir", null}};
        BigDecimal[] nilai = {laporan.modalAwal(), laporan.setoran(), laporan.prive(),
                laporan.labaRugi(), laporan.modalAkhir()};
        for (int i = 0; i < baris.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(baris[i][0]);
            selUang(row, 1, uang, nilai[i]);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void header(Sheet sheet, String... judul) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < judul.length; i++) row.createCell(i).setCellValue(judul[i]);
    }

    private void selUang(Row baris, int kolom, CellStyle uang, BigDecimal nilai) {
        Cell cell = baris.createCell(kolom);
        cell.setCellValue(nilai.doubleValue());
        cell.setCellStyle(uang);
    }

    // ------------------------------------------------------------------ PDF

    public byte[] exportPdf(int tahun) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font judulBesar = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        doc.add(new Paragraph("Laporan Keuangan Tahun " + tahun, judulBesar));
        doc.add(new Paragraph(" "));

        tabelSaldo(doc, "Neraca", reportService.getBalanceSheet());
        tabelSaldo(doc, "Laba Rugi", reportService.getIncomeStatement());
        tabelArusKas(doc, reportService.getCashFlow(tahun));
        tabelPerubahanModal(doc, reportService.getEquityChange(tahun));

        doc.close();
        return out.toByteArray();
    }

    private void tabelSaldo(Document doc, String judul, List<AccountBalanceRow> rows) {
        doc.add(subJudul(judul));
        PdfPTable tabel = tabel("Tipe", "Kode", "Nama Akun", "Saldo (Rp)");
        for (AccountBalanceRow row : rows) {
            tabel.addCell(sel(row.tipe()));
            tabel.addCell(sel(row.kodeAkun()));
            tabel.addCell(sel(row.namaAkun()));
            tabel.addCell(selAngka(row.saldo()));
        }
        doc.add(tabel);
    }

    private void tabelArusKas(Document doc, CashFlowReport laporan) {
        doc.add(subJudul("Arus Kas (Metode Langsung) " + laporan.tahun()));
        PdfPTable tabel = tabel("Kategori", "Masuk (Rp)", "Keluar (Rp)", "Bersih (Rp)");
        tabel.addCell(sel("Saldo Kas Awal"));
        tabel.addCell(sel(""));
        tabel.addCell(sel(""));
        tabel.addCell(selAngka(laporan.saldoKasAwal()));
        for (CashFlowCategoryRow row : laporan.kategori()) {
            tabel.addCell(sel(row.kategori()));
            tabel.addCell(selAngka(row.arusMasuk()));
            tabel.addCell(selAngka(row.arusKeluar()));
            tabel.addCell(selAngka(row.arusBersih()));
        }
        tabel.addCell(sel("Saldo Kas Akhir"));
        tabel.addCell(sel(""));
        tabel.addCell(sel(""));
        tabel.addCell(selAngka(laporan.saldoKasAkhir()));
        doc.add(tabel);
    }

    private void tabelPerubahanModal(Document doc, EquityChangeReport laporan) {
        doc.add(subJudul("Perubahan Modal (SAK EMKM) " + laporan.tahun()));
        PdfPTable tabel = tabel("Komponen", "Nilai (Rp)");
        String[] label = {"Modal Awal", "Setoran Modal", "Prive (Pengambilan)",
                "Laba (Rugi) Periode", "Modal Akhir"};
        BigDecimal[] nilai = {laporan.modalAwal(), laporan.setoran(), laporan.prive(),
                laporan.labaRugi(), laporan.modalAkhir()};
        for (int i = 0; i < label.length; i++) {
            tabel.addCell(sel(label[i]));
            tabel.addCell(selAngka(nilai[i]));
        }
        doc.add(tabel);
    }

    private Paragraph subJudul(String teks) {
        Paragraph p = new Paragraph(teks, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        return p;
    }

    private PdfPTable tabel(String... judul) {
        PdfPTable tabel = new PdfPTable(judul.length);
        tabel.setWidthPercentage(100);
        for (String j : judul) {
            PdfPCell cell = new PdfPCell(new Paragraph(j,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            tabel.addCell(cell);
        }
        return tabel;
    }

    private PdfPCell sel(String teks) {
        return new PdfPCell(new Paragraph(teks, FontFactory.getFont(FontFactory.HELVETICA, 9)));
    }

    private PdfPCell selAngka(BigDecimal nilai) {
        PdfPCell cell = new PdfPCell(new Paragraph(
                NumberFormat.getNumberInstance(ID).format(nilai),
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
