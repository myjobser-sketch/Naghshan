package ir.arandy.naghshan.data.export;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.util.MoneyFormatter;
import ir.arandy.naghshan.util.PersianNumberParser;
import ir.arandy.naghshan.util.PersianTextShaper;

/**
 * تولید PDF با ساختار جدولی مشابه Excel:
 *   - سرصفحه اطلاعات طرح
 *   - برای هر KalaTable: عنوان + ردیف هدر + ردیف‌های کالا + جمع‌ها
 *   - جمع کل نهایی
 *
 * ستون‌ها (از راست به چپ):
 *   ردیف | کد کالا | نام کالا | واحد | نوع | تعداد | مبلغ جنس | مبلغ دستمزد | مبلغ کل
 */
public class PdfExporter {

    private static final float MARGIN  = 40f;
    private static final float PAGE_W  = PDRectangle.A4.getWidth();
    private static final float PAGE_H  = PDRectangle.A4.getHeight();
    private static final float USABLE_W = PAGE_W - 2 * MARGIN;

    // ارتفاع‌ها
    private static final float HEADER_H = 18f;
    private static final float DATA_H   = 16f;

    // پهنای ستون‌ها (راست → چپ)؛ مجموع = USABLE_W ≈ 515
    private static final float[] COL_W = {
            30f,   // ردیف
            65f,   // کد کالا
            150f,  // نام کالا
            40f,   // واحد
            35f,   // نوع
            40f,   // تعداد
            55f,   // مبلغ جنس
            55f,   // مبلغ دستمزد
            45f    // مبلغ کل
    };

    private static final String[] HEADERS = {
            "ردیف", "کد کالا", "نام کالا", "واحد", "نوع",
            "تعداد", "مبلغ جنس", "مبلغ دستمزد", "مبلغ کل"
    };

    // ----------------------------------------------------------------

    public File export(Invoice invoice, File outFile, File persianFontFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = PDType0Font.load(doc, persianFontFile);

            TableWriter tw = new TableWriter(doc, font);

            // ---- سرصفحه اطلاعات طرح ----
            tw.writeHeader("فاکتور طرح", 15);
            tw.writeHeader("شماره طرح: " + safe(invoice.getProjectInfo().getProjectNumber()), 10);
            tw.writeHeader("متقاضی: " + safe(invoice.getProjectInfo().getApplicantName()), 10);
            tw.writeHeader("شماره درخواست: " + safe(invoice.getProjectInfo().getRequestNumber()), 10);
            tw.blank(10);

            // ---- جداول ----
            for (KalaTable table : invoice.getTables()) {
                tw.writeHeader("◄ " + safe(table.getName()), 12);
                tw.blank(4);

                tw.drawHeaderRow(HEADERS, COL_W, HEADER_H);

                for (Kala k : table.getItems()) {
                    String[] cells = {
                            String.valueOf(k.getRowNumber()),
                            safe(k.getProductCode()),
                            safe(k.getProductName()),
                            safe(k.getUnit()),
                            safe(k.getType()),
                            formatQty(k.getQuantity()),
                            MoneyFormatter.format(k.getMaterialPrice()),
                            MoneyFormatter.format(k.getLaborPrice()),
                            MoneyFormatter.format(k.getTotalPrice())
                    };
                    tw.drawDataRow(cells, COL_W, DATA_H);
                }

                tw.drawFooterRow("جمع جنس این جدول",
                        MoneyFormatter.format(table.getMaterialTotal()));
                tw.drawFooterRow("جمع دستمزد این جدول",
                        MoneyFormatter.format(table.getLaborTotal()));
                tw.drawFooterRow("جمع کل این جدول",
                        MoneyFormatter.format(table.getGrandTotal()));

                tw.blank(14);
            }

            // ---- جمع کل ----
            tw.blank(4);
            tw.writeHeader("جمع کل (همه جدول‌ها): "
                    + MoneyFormatter.formatWithRial(invoice.getGrandTotal()), 12);

            tw.close();
            doc.save(outFile);
        }
        return outFile;
    }

    private static String formatQty(double q) {
        if (q == Math.floor(q)) return String.valueOf((long) q);
        return PersianNumberParser.toLatinDigits(String.valueOf(q));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // =================================================================
    //  TableWriter — کشیدن جدول + متن‌های سرصفحه
    // =================================================================

    private static class TableWriter {

        private final PDDocument doc;
        private final PDFont     font;

        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        /** برای هر کد کاراکتر، خودش اگر در فونت هست، وگرنه '?'. */
        private final java.util.Map<Character, Character> glyphCache = new java.util.HashMap<>();

        TableWriter(PDDocument doc, PDFont font) throws IOException {
            this.doc = doc;
            this.font = font;
            newPage();
        }

        void newPage() throws IOException {
            if (cs != null) cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;
        }

        void ensure(float need) throws IOException {
            if (y - need < MARGIN) newPage();
        }

        void blank(float h) throws IOException {
            y -= h;
            ensure(0);
        }

        /** یک خط متن راست‌چین در کل عرض صفحه. */
        void writeHeader(String text, int size) throws IOException {
            ensure(size + 6);
            String shaped = PersianTextShaper.shape(text == null ? "" : text);
            shaped = sanitize(shaped);
            float w = textWidth(shaped, size);
            float x = PAGE_W - MARGIN - w;
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y - size);
            cs.showText(shaped);
            cs.endText();
            y -= size + 6;
        }

        /** ردیف سرصفحه ستون‌ها با پس‌زمینه و مرز. */
        void drawHeaderRow(String[] cells, float[] widths, float h) throws IOException {
            ensure(h);
            float top = y;
            float x = PAGE_W - MARGIN;
            for (int i = 0; i < cells.length; i++) {
                float w = widths[i];
                x -= w;
                // پس‌زمینه آبی روشن
                cs.setNonStrokingColor(0xDB / 255f, 0xEA / 255f, 0xFE / 255f);
                cs.addRect(x, top - h, w, h);
                cs.fill();
                // مرز
                cs.setStrokingColor(0x64 / 255f, 0x74 / 255f, 0x8B / 255f);
                cs.setLineWidth(0.5f);
                cs.addRect(x, top - h, w, h);
                cs.stroke();
                // متن وسط‌چین
                drawCellText(cells[i], x, top - h, w, h, 9);
            }
            y = top - h;
        }

        /** یک ردیف داده معمولی. */
        void drawDataRow(String[] cells, float[] widths, float h) throws IOException {
            ensure(h);
            float top = y;
            float x = PAGE_W - MARGIN;
            for (int i = 0; i < cells.length; i++) {
                float w = widths[i];
                x -= w;
                cs.setStrokingColor(0xCB / 255f, 0xD5 / 255f, 0xE1 / 255f);
                cs.setLineWidth(0.3f);
                cs.addRect(x, top - h, w, h);
                cs.stroke();
                drawCellText(cells[i], x, top - h, w, h, 9);
            }
            y = top - h;
        }

        /** ردیف جمع: برچسب سمت راست + مقدار در ناحیه عددی. */
        void drawFooterRow(String label, String value) throws IOException {
            float h = DATA_H;
            ensure(h);
            float top = y;

            // پس‌زمینه سبز روشن
            cs.setNonStrokingColor(0xE8 / 255f, 0xF5 / 255f, 0xE9 / 255f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.fill();
            cs.setStrokingColor(0x64 / 255f, 0x74 / 255f, 0x8B / 255f);
            cs.setLineWidth(0.4f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.stroke();

            // پهنای برچسب = ۶ ستون اول از راست؛ پهنای مقدار = ۳ ستون آخر
            float labelW = 0f;
            for (int i = 0; i < 6; i++) labelW += COL_W[i];
            float valueW = USABLE_W - labelW;

            // برچسب، راست‌چین در سمت راست نوار
            drawCellText(label, PAGE_W - MARGIN - labelW, top - h, labelW, h, 10);
            // مقدار، وسط‌چین در سمت چپ نوار
            drawCellText(value, MARGIN, top - h, valueW, h, 10);

            y = top - h;
        }

        /** رسم متن در مرکز افقی و عمودی سلول، با ellipsis در صورت لزوم. */
        private void drawCellText(String text, float cellX, float cellBottom,
                                  float cellW, float cellH, int size) throws IOException {
            String shaped = PersianTextShaper.shape(text == null ? "" : text);
            shaped = sanitize(shaped);

            float w = textWidth(shaped, size);
            if (w > cellW - 4f) {
                shaped = ellipsize(shaped, cellW - 4f, size);
                w = textWidth(shaped, size);
            }
            float tx = cellX + (cellW - w) / 2f;
            float ty = cellBottom + (cellH - size) / 2f + 1f;

            // اگر هنوز خطا داد، تک‌تک کاراکترها را تست کن
            try {
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(tx, ty);
                cs.showText(shaped);
                cs.endText();
            } catch (IllegalArgumentException e) {
                cs.endText();                      // بستن بلوک متن نیمه‌کاره
                String safe = sanitizeStrict(shaped);
                float sw = textWidth(safe, size);
                float sx = cellX + (cellW - sw) / 2f;
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(sx, ty);
                cs.showText(safe);
                cs.endText();
            }
        }

        private float textWidth(String s, int size) {
            try {
                return font.getStringWidth(s) / 1000f * size;
            } catch (Exception e) {
                return 0f;
            }
        }

        /** آیا فونت گلیف این کاراکتر را دارد؟ نتیجه کَش می‌شود. */
        private boolean isGlyphAvailable(char c) {
            Character cached = glyphCache.get(c);
            if (cached != null) return cached != '?';
            boolean ok;
            try {
                // encode() همان چیزی را چک می‌کند که showText در پشت صحنه صدا می‌زند
                font.encode(String.valueOf(c));
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            glyphCache.put(c, ok ? c : '?');
            return ok;
        }

        /** حذف نویسه‌های ناموجود در فونت با '?' تا PDFBox خطا ندهد. */
        private String sanitize(String s) {
            if (s == null || s.isEmpty()) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < 0x20 && c != '\t') continue;

                // ۱) جایگزینی اجباری برای کاراکترهایی که فونت Vazir-Medium ندارد
                char forced = forceSubstitute(c);
                if (forced != 0) { sb.append(forced); continue; }

                // ۲) بررسی عادی بقیه‌ی کاراکترها
                sb.append(isGlyphAvailable(c) ? c : '?');
            }
            return sb.toString();
        }
        /**
         * جایگزین اجباری برای کاراکترهایی که فونت Vazir-Medium ادعا می‌کند
         * دارد ولی در عمل گلیف واقعی ندارد (font.encode استثنا نمی‌دهد).
         * @return جایگزین یا 0 اگر نیازی به جایگزینی نیست.
         */
        private static char forceSubstitute(char c) {
            switch (c) {
                // انواع خط تیره
                case '-':      // HYPHEN-MINUS
                case '\u2010': // HYPHEN
                case '\u2011': // NON-BREAKING HYPHEN
                case '\u2012': // FIGURE DASH
                case '\u2013': // EN DASH
                case '\u2014': // EM DASH
                    return '_';

                // فلش‌ها (احتمالاً در فونت نیستند)
                case '\u25C4': // BLACK LEFT-POINTING TRIANGLE
                case '\u25BA': // BLACK RIGHT-POINTING TRIANGLE
                case '\u25B6': // BLACK RIGHT-POINTING TRIANGLE (alt)
                    return '*';

                default:
                    return 0;
            }
        }



        /** sanitize قوی‌تر: هر کاراکتر را با encode تست می‌کند. */
        private String sanitizeStrict(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < 0x20 && c != '\t') continue;
                try {
                    font.encode(String.valueOf(c));
                    sb.append(c);
                } catch (Exception e) {
                    sb.append('?');
                }
            }
            return sb.toString();
        }

        private String ellipsize(String s, float maxW, int size) {
            String ell = "...";
            float ew = textWidth(ell, size);
            if (maxW <= ew) return "";
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() > 1) {
                sb.deleteCharAt(sb.length() - 1);
                if (textWidth(sb.toString(), size) + ew <= maxW) return sb + ell;
            }
            return ell;
        }

        void close() throws IOException {
            if (cs != null) cs.close();
        }
    }
}