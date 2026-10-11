package ir.arandy.naghshan.data.export;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.util.MoneyFormatter;

/**
 * ساخت PDF نهایی.
 * ساختار:
 *   ۱. سرصفحه اطلاعات طرح
 *   ۲. برای هر جدول:
 *       - نام جدول (زیرعنوان)
 *       - سرصفحه ستون‌ها
 *       - ردیف‌های کالا
 *       - جمع‌های جدول
 *   ۳. جمع کل نهایی
 */
public class PdfExporter {

    private static final float MARGIN = 40f;
    private static final float LINE_H = 14f;
    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();

    public File export(Invoice invoice, File outFile, File persianFontFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = PDType0Font.load(doc, persianFontFile);

            // حالت سراسری صفحه‌بندی
            PageState state = new PageState(doc, font);

            // ---------- سرصفحه ----------
            state.write("فاکتور طرح", 16, true);
            state.write("شماره طرح: " + safe(invoice.getProjectInfo().getProjectNumber()), 11, false);
            state.write("متقاضی: " + safe(invoice.getProjectInfo().getApplicantName()), 11, false);
            state.write("شماره درخواست: " + safe(invoice.getProjectInfo().getRequestNumber()), 11, false);
            state.blank();

            // ---------- جدول‌ها ----------
            for (KalaTable table : invoice.getTables()) {
                state.blank();

                // نام جدول
                state.write("◄ " + safe(table.getName()), 13, true);

                // سرصفحه ستون‌ها
                state.write("ردیف | کد کالا | نام کالا | واحد | نوع | تعداد | جنس | دستمزد | کل",
                        10, true);

                // ردیف‌ها
                for (Kala k : table.getItems()) {
                    String row = k.getRowNumber() + " | "
                            + safe(k.getProductCode()) + " | "
                            + safe(k.getProductName()) + " | "
                            + safe(k.getUnit()) + " | "
                            + safe(k.getType()) + " | "
                            + formatQty(k.getQuantity()) + " | "
                            + MoneyFormatter.format(k.getMaterialPrice()) + " | "
                            + MoneyFormatter.format(k.getLaborPrice()) + " | "
                            + MoneyFormatter.format(k.getTotalPrice());
                    state.write(row, 9, false);
                }

                // جمع‌های جدول
                state.write("جمع جنس این جدول: "
                        + MoneyFormatter.format(table.getMaterialTotal()), 11, true);
                state.write("جمع دستمزد این جدول: "
                        + MoneyFormatter.format(table.getLaborTotal()), 11, true);
                state.write("جمع کل این جدول: "
                        + MoneyFormatter.format(table.getGrandTotal()), 11, true);
            }

            // ---------- جمع کل ----------
            state.blank();
            state.write("جمع کل (همه جدول‌ها): "
                    + MoneyFormatter.format(invoice.getGrandTotal()), 13, true);

            state.close();
            doc.save(outFile);
        }
        return outFile;
    }

    private static String formatQty(double q) {
        if (q == Math.floor(q)) return String.valueOf((long) q);
        return String.valueOf(q);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ------------------------------------------------------------------
    //  کلاس داخلی برای مدیریت صفحه‌بندی
    // ------------------------------------------------------------------

    private static class PageState {
        private final PDDocument doc;
        private final PDType0Font font;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        PageState(PDDocument doc, PDType0Font font) throws IOException {
            this.doc = doc;
            this.font = font;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;
        }

        void write(String text, int size, boolean bold) throws IOException {
            if (y < MARGIN + LINE_H * 2) newPage();

            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(text == null ? "" : text);
            cs.endText();
            y -= LINE_H + 2;
        }

        void blank() throws IOException {
            y -= LINE_H;
            if (y < MARGIN + LINE_H * 2) newPage();
        }

        void close() throws IOException {
            if (cs != null) cs.close();
        }
    }
}