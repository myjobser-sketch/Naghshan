package ir.arandy.naghshan.data.export;

import android.util.Log;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.util.MoneyToPersianWords;

/**
 * تولید Excel با سربرگ رنگی + چیدمان راست‌به‌چپ + توضیحات.
 */
public class ExcelExporter {

    private static final String TAG = "NAGHSHAN_EXCEL";

    public File export(Invoice invoice, File outFile,
                       ProfileManager pm, ExportOptions opts) throws IOException {

        if (opts == null) opts = new ExportOptions();
        Log.i(TAG, "export: start -> " + outFile.getName());
        opts.log();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            Workbook wb = new Workbook(fos, "Naghshan", "1.0");
            Worksheet ws = wb.newWorksheet("فاکتور");

            // ⬇ راست‌به‌چپ کردن کل شیت
            try {
                ws.rightToLeft();
                Log.d(TAG, "sheet rightToLeft applied");
            } catch (Throwable t) {
                Log.w(TAG, "rightToLeft not supported in this fastexcel version: "
                        + t.getMessage());
            }

            // پهنای ستون‌ها
            ws.width(0, 8);
            ws.width(1, 14);
            ws.width(2, 40);
            ws.width(3, 10);
            ws.width(4, 12);
            ws.width(5, 10);
            ws.width(6, 18);
            ws.width(7, 18);
            ws.width(8, 20);

            int row = 0;
            String bgHex = (opts.themeColor == null || opts.themeColor.isEmpty())
                    ? "F1F5F9"
                    : opts.themeColor.replace("#", "");

            // ---------- ردیف ۰: نام شرکت ----------
            String company = (pm == null || pm.getCompany().isEmpty())
                    ? "—" : pm.getCompany();
            ws.value(row, 0, company);
            for (int c = 0; c < 9; c++) {
                ws.style(row, c).fillColor(bgHex).borderStyle(BorderStyle.THIN)
                        .borderColor("94A3B8").set();
            }
            ws.style(row, 0).bold().fontSize(14).set();
            row++;

            // ---------- ردیف ۱: نام صادرکننده ----------
            String issuer = (pm == null || pm.getFullName().isEmpty())
                    ? "" : "صادرکننده: " + pm.getFullName();
            ws.value(row, 0, issuer);
            for (int c = 0; c < 9; c++) {
                ws.style(row, c).fillColor(bgHex).borderStyle(BorderStyle.THIN)
                        .borderColor("94A3B8").set();
            }
            ws.style(row, 0).fontSize(10).set();
            row++;

            // ---------- ردیف ۲: خالی ----------
            row++;

            // ---------- ردیف ۳: تاریخ و شماره فاکتور ----------
            ws.value(row, 0, "تاریخ فاکتور: " + safe(opts.issueDate));
            ws.style(row, 0).bold().fontSize(10).set();

            ws.value(row, 5, "شماره فاکتور: " + safe(opts.invoiceNumber));
            ws.style(row, 5).bold().fontSize(10).set();
            row++;

            // ---------- ردیف ۴: شماره طرح ----------
            ws.value(row, 0, "شماره طرح: " +
                    safe(invoice.getProjectInfo().getProjectNumber()));
            ws.style(row, 0).fontSize(10).set();
            row++;

            // ---------- ردیف ۵: نام طرح ----------
            ws.value(row, 0, "نام طرح: " +
                    safe(invoice.getProjectInfo().getProjectName()));
            ws.style(row, 0).fontSize(10).set();
            row += 2;

            // ---------- جداول ----------
            for (KalaTable table : invoice.getTables()) {

                // زیرعنوان
                ws.value(row, 0, safe(table.getName()));
                for (int c = 0; c < 9; c++) {
                    ws.style(row, c)
                            .borderStyle(BorderStyle.THIN)
                            .borderColor("64748B")
                            .fillColor("F1F5F9")
                            .set();
                }
                ws.style(row, 0).bold().fontSize(13).set();
                row++;

                // سرستون‌ها
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

                // داده‌ها
                for (Kala k : table.getItems()) {
                    ws.value(row, 0, k.getRowNumber());
                    ws.value(row, 1, safe(k.getProductCode()));
                    ws.value(row, 2, safe(k.getProductName()));
                    ws.value(row, 3, safe(k.getUnit()));
                    ws.value(row, 4, safe(k.getType()));
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

                // جمع‌ها
                ws.value(row, 0, "جمع جنس");
                ws.value(row, 1, table.getMaterialTotal());
                applyFooterStyle(ws, row, "E8F5E9");
                row++;

                ws.value(row, 0, "جمع دستمزد");
                ws.value(row, 1, table.getLaborTotal());
                applyFooterStyle(ws, row, "E8F5E9");
                row++;

                ws.value(row, 0, "جمع کل");
                ws.value(row, 1, table.getGrandTotal());
                applyFooterStyle(ws, row, "A7F3D0");
                row++;

                row += 2;
            }

            // ---------- جمع کل نهایی ----------
            ws.value(row, 0, "جمع کل (همه جدول‌ها)");
            ws.value(row, 1, invoice.getGrandTotal());
            ws.style(row, 0).bold().fillColor("FEF3C7").set();
            ws.style(row, 1).bold().fillColor("FEF3C7").set();
            row++;

            // ---------- به حروف ----------
            ws.value(row, 0, "به حروف:");
            ws.style(row, 0).bold().set();
            ws.value(row, 1, MoneyToPersianWords.formatWithRial(invoice.getGrandTotal()));
            ws.style(row, 1).italic().set();
            row += 2;

            // ---------- توضیحات ----------
            if (opts.invoiceNote != null && !opts.invoiceNote.trim().isEmpty()) {
                ws.value(row, 0, "توضیحات:");
                ws.style(row, 0).bold().fontSize(10).set();
                row++;

                ws.value(row, 0, opts.invoiceNote.trim());
                ws.style(row, 0).fontSize(9).set();
                // تلاش برای wrap؛ در fastexcel 0.15 پشتیبانی نمی‌شود → try/catch
                try {
                    ws.style(row, 0).wrapText(true).set();
                } catch (Throwable ignored) { /* ignore */ }
                ws.style(row, 0).horizontalAlignment("right").set();
                row++;
            }

            wb.finish();
            Log.i(TAG, "export: done -> " + outFile.getAbsolutePath());
        }
        return outFile;
    }

    private static void applyFooterStyle(Worksheet ws, int row, String color) {
        ws.style(row, 0).bold().fillColor(color).set();
        ws.style(row, 1).bold().fillColor(color).set();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}