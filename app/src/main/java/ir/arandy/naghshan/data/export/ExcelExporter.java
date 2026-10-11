package ir.arandy.naghshan.data.export;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;

/**
 * تولید Excel از Invoice.
 * هر جدول (KalaTable) با زیرعنوان خودش + سرصفحه + ردیف‌ها + جمع‌ها چاپ می‌شود.
 *
 * نکته: FastExcel نسخه 0.15.x متد mergeCells ندارد.
 * برای جایگزینی، از یک سلول بزرگ استفاده می‌کنیم و بقیه را خالی می‌گذاریم.
 */
public class ExcelExporter {

    public File export(Invoice invoice, File outFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            Workbook wb = new Workbook(fos, "Naghshan", "1.0");
            Worksheet ws = wb.newWorksheet("فاکتور");

            int row = 0;

            // ---------- سرصفحه کلی ----------
            ws.value(row, 0, "شماره طرح:");
            ws.value(row, 1, nz(invoice.getProjectInfo().getProjectNumber()));
            row++;

            ws.value(row, 0, "متقاضی:");
            ws.value(row, 1, nz(invoice.getProjectInfo().getApplicantName()));
            row++;

            ws.value(row, 0, "شماره درخواست:");
            ws.value(row, 1, nz(invoice.getProjectInfo().getRequestNumber()));
            row++;

            row++; // خط خالی

            // ---------- برای هر جدول ----------
            for (KalaTable table : invoice.getTables()) {

                // زیرعنوان جدول — در سلول A می‌نویسیم و استایل می‌دهیم.
                ws.value(row, 0, nz(table.getName()));
                // به‌جای mergeCells، سلول را bold و پررنگ می‌کنیم
                for (int c = 0; c < 9; c++) {
                    ws.style(row, c)
                            .borderStyle(BorderStyle.THIN)
                            .borderColor("64748B")
                            .fillColor("F1F5F9")
                            .set();
                }
                ws.style(row, 0).bold().fontSize(13).set();
                row++;

                // سرصفحه ستون‌ها
                String[] headers = {
                        "ردیف", "کد کالا", "نام کالا", "واحد", "نوع",
                        "تعداد", "مبلغ جنس", "مبلغ دستمزد", "مبلغ کل"
                };
                for (int c = 0; c < headers.length; c++) {
                    ws.value(row, c, headers[c]);
                    ws.style(row, c)
                            .bold()
                            .fillColor("DBEAFE")
                            .horizontalAlignment("center")
                            .borderStyle(BorderStyle.THIN)
                            .borderColor("64748B")
                            .set();
                }
                row++;

                // ردیف‌های کالا
                for (Kala k : table.getItems()) {
                    ws.value(row, 0, k.getRowNumber());
                    ws.value(row, 1, nz(k.getProductCode()));
                    ws.value(row, 2, nz(k.getProductName()));
                    ws.value(row, 3, nz(k.getUnit()));
                    ws.value(row, 4, nz(k.getType()));
                    ws.value(row, 5, k.getQuantity());
                    ws.value(row, 6, k.getMaterialPrice());
                    ws.value(row, 7, k.getLaborPrice());
                    ws.value(row, 8, k.getTotalPrice());

                    for (int c = 0; c < 9; c++) {
                        ws.style(row, c)
                                .borderStyle(BorderStyle.THIN)
                                .borderColor("CBD5E1")
                                .set();
                    }
                    row++;
                }

                // جمع‌های جدول
                ws.value(row, 0, "جمع جنس");
                ws.value(row, 1, table.getMaterialTotal());
                applyFooterStyle(ws, row, 0, "E8F5E9");
                row++;

                ws.value(row, 0, "جمع دستمزد");
                ws.value(row, 1, table.getLaborTotal());
                applyFooterStyle(ws, row, 0, "E8F5E9");
                row++;

                ws.value(row, 0, "جمع کل");
                ws.value(row, 1, table.getGrandTotal());
                applyFooterStyle(ws, row, 0, "A7F3D0");
                row++;

                row += 2; // دو خط خالی بین جدول‌ها
            }

            // ---------- جمع کل نهایی ----------
            ws.value(row, 0, "جمع کل (همه جدول‌ها)");
            ws.value(row, 1, invoice.getGrandTotal());
            ws.style(row, 0).bold().fillColor("FEF3C7").set();
            ws.style(row, 1).bold().fillColor("FEF3C7").set();

            wb.finish();
        }
        return outFile;
    }

    /** استایل یکسان برای ردیف‌های جمع هر جدول. */
    private void applyFooterStyle(Worksheet ws, int row, int col, String color) {
        ws.style(row, col).bold().fillColor(color).set();
        ws.style(row, col + 1).bold().fillColor(color).set();
    }

    private static String nz(String s) { return s == null ? "" : s; }
}