package ir.arandy.naghshan.data.export;

import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.util.MoneyFormatter;
import ir.arandy.naghshan.util.MoneyToPersianWords;
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

    private static final String TAG = "NAGHSHAN_PDF";

    private static final float MARGIN  = 40f;
    private static final float PAGE_W  = PDRectangle.A4.getWidth();
    private static final float PAGE_H  = PDRectangle.A4.getHeight();
    private static final float USABLE_W = PAGE_W - 2 * MARGIN;

    // ارتفاع‌ها
    private static final float HEADER_H = 18f;
    private static final float DATA_H   = 16f;

    // پهنای ستون‌ها (راست → چپ)؛ مجموع = USABLE_W ≈ 515
    private static final float[] COL_W = {
            24f,   // ردیف
            60f,   // کد کالا
            130f,  // نام کالا
            36f,   // واحد
            30f,   // نوع
            36f,   // تعداد
            67f,   // مبلغ جنس
            67f,   // مبلغ دستمزد
            60f    // مبلغ کل
    };

    private static final String[] HEADERS = {
            "ردیف", "کد کالا", "نام کالا", "واحد", "نوع",
            "تعداد", "مبلغ جنس", "مبلغ دستمزد", "مبلغ کل"
    };

    // ----------------------------------------------------------------

    public File export(Invoice invoice, File outFile, File persianFontFile,
                       ProfileManager pm) throws IOException {
        Log.i(TAG, "export: start -> " + outFile.getName());

        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = PDType0Font.load(doc, persianFontFile);
            TableWriter tw = new TableWriter(doc, font);

            // ---- سرصفحه شرکت ----
            if (pm != null && pm.isComplete()) {
                tw.drawCompanyBanner(pm.getCompany(), pm.getFullName());
            }

            // ---- سرصفحه اطلاعات طرح ----
            tw.writeHeader("فاکتور طرح", 15);
            tw.writeHeader("شماره طرح: " + safe(invoice.getProjectInfo().getProjectNumber()), 10);
            tw.writeHeader("متقاضی: " + safe(invoice.getProjectInfo().getApplicantName()), 10);
            tw.writeHeader("شماره درخواست: " + safe(invoice.getProjectInfo().getRequestNumber()), 10);
            tw.blank(10);

            // ---- جداول ----
            for (KalaTable table : invoice.getTables()) {
                tw.ensure(HEADER_H + DATA_H * 5);
                tw.writeHeader("◄ " + safe(table.getName()), 12);
                tw.blank(4);
                tw.enableHeaderRedraw(HEADERS, COL_W);
                tw.drawHeaderRow(HEADERS, COL_W, HEADER_H);

                int idx = 0;
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
                    tw.drawDataRow(cells, COL_W, DATA_H, idx % 2 == 1);
                    idx++;
                }

                tw.drawFooterRow("جمع جنس این جدول",
                        MoneyFormatter.format(table.getMaterialTotal()));
                tw.drawFooterRow("جمع دستمزد این جدول",
                        MoneyFormatter.format(table.getLaborTotal()));
                tw.drawFooterRow("جمع کل این جدول",
                        MoneyFormatter.format(table.getGrandTotal()));

                tw.disableHeaderRedraw();
                tw.blank(14);
            }

            // ---- جمع کل + به حروف ----
            tw.blank(4);
            tw.writeHeader("جمع کل همه جدول‌ها: "
                    + MoneyFormatter.format(invoice.getGrandTotal()) + " ریال", 12);
            tw.blank(4);
            tw.writeHeader("به حروف: " + MoneyToPersianWords.formatWithRial(
                    invoice.getGrandTotal()), 10);

            tw.close();
            doc.save(outFile);
            Log.i(TAG, "export: done -> " + outFile.getAbsolutePath());
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
        private String[] pendingHeaderCells;
        private float[]  pendingHeaderWidths;

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
            // اگر وسط یک جدول هستیم، هدر ستون‌ها را دوباره بکش
            if (pendingHeaderCells != null && pendingHeaderWidths != null) {
                drawHeaderRowInternal(pendingHeaderCells, pendingHeaderWidths, HEADER_H);
            }
        }

        /** نوار بالای صفحه با نام شرکت و صادرکننده. */
        void drawCompanyBanner(String company, String person) throws IOException {
            float bannerH = 40f;
            cs.setNonStrokingColor(0x1E / 255f, 0x3A / 255f, 0x8A / 255f);
            cs.addRect(0, PAGE_H - bannerH, PAGE_W, bannerH);
            cs.fill();

            String c = sanitize(PersianTextShaper.shape(company == null ? "" : company));
            String p = sanitize(PersianTextShaper.shape(person == null ? "" : person));

            // نام شرکت (چپ‌چین داخل نوار، راست‌چین خوانا)
            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.beginText();
            cs.setFont(font, 12);
            float cw = textWidth(c, 12);
            cs.newLineAtOffset(PAGE_W - MARGIN - cw, PAGE_H - 18);
            cs.showText(c);
            cs.endText();

            // نام صادرکننده در سمت چپ نوار
            cs.beginText();
            cs.setFont(font, 9);
            float pw = textWidth(p, 9);
            cs.newLineAtOffset(MARGIN, PAGE_H - 18);
            cs.showText(p);
            cs.endText();

            y = PAGE_H - bannerH - 8;
            Log.d(TAG, "banner: " + company + " / " + person);
        }

        /** فعال‌سازی بازترسیم هدر در صفحه‌های بعدی. */
        void enableHeaderRedraw(String[] cells, float[] widths) {
            this.pendingHeaderCells  = cells;
            this.pendingHeaderWidths = widths;
        }

        /** غیرفعال‌سازی (وقتی جدول تمام شد). */
        void disableHeaderRedraw() {
            this.pendingHeaderCells  = null;
            this.pendingHeaderWidths = null;
        }

        /** نسخه‌ی داخلی drawHeaderRow که از newPage صدا زده می‌شود (بدون ensure). */
        private void drawHeaderRowInternal(String[] cells, float[] widths, float h) throws IOException {
            float top = y;
            float x = PAGE_W - MARGIN;
            for (int i = 0; i < cells.length; i++) {
                float w = widths[i];
                x -= w;
                cs.setNonStrokingColor(0xDB / 255f, 0xEA / 255f, 0xFE / 255f);
                cs.addRect(x, top - h, w, h);
                cs.fill();
                cs.setStrokingColor(0x64 / 255f, 0x74 / 255f, 0x8B / 255f);
                cs.setLineWidth(0.5f);
                cs.addRect(x, top - h, w, h);
                cs.stroke();
                drawCellText(cells[i], x, top - h, w, h, 9, false);
            }
            y = top - h;
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
                cs.setNonStrokingColor(0xDB / 255f, 0xEA / 255f, 0xFE / 255f);
                cs.addRect(x, top - h, w, h);
                cs.fill();
                cs.setStrokingColor(0x64 / 255f, 0x74 / 255f, 0x8B / 255f);
                cs.setLineWidth(0.5f);
                cs.addRect(x, top - h, w, h);
                cs.stroke();
                drawCellText(cells[i], x, top - h, w, h, 9, false);   // وسط‌چین
            }
            y = top - h;
        }

        /** یک ردیف داده معمولی. */
        void drawDataRow(String[] cells, float[] widths, float h, boolean alternate) throws IOException {
            ensure(h);
            float top = y;
            float x = PAGE_W - MARGIN;

            if (alternate) {
                cs.setNonStrokingColor(0xF1 / 255f, 0xF5 / 255f, 0xF9 / 255f);
                cs.addRect(MARGIN, top - h, USABLE_W, h);
                cs.fill();
            }

            for (int i = 0; i < cells.length; i++) {
                float w = widths[i];
                x -= w;
                cs.setStrokingColor(0xCB / 255f, 0xD5 / 255f, 0xE1 / 255f);
                cs.setLineWidth(0.3f);
                cs.addRect(x, top - h, w, h);
                cs.stroke();
                boolean rightAlign = (i == 2);
                drawCellText(cells[i], x, top - h, w, h, 8, rightAlign);
            }
            y = top - h;
        }

        /** ردیف جمع: برچسب سمت راست + مقدار در ناحیه عددی. */
        void drawFooterRow(String label, String value) throws IOException {
            float h = DATA_H;
            ensure(h);
            float top = y;

            cs.setNonStrokingColor(0xE8 / 255f, 0xF5 / 255f, 0xE9 / 255f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.fill();
            cs.setStrokingColor(0x64 / 255f, 0x74 / 255f, 0x8B / 255f);
            cs.setLineWidth(0.4f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.stroke();

            float labelW = 0f;
            for (int i = 0; i < 6; i++) labelW += COL_W[i];
            float valueW = USABLE_W - labelW;

            drawCellText(label, PAGE_W - MARGIN - labelW, top - h, labelW, h, 9, true);   // راست‌چین
            drawCellText(value, MARGIN, top - h, valueW, h, 9, false);                    // وسط‌چین

            y = top - h;
        }

        /** رسم متن در مرکز افقی و عمودی سلول، با ellipsis در صورت لزوم. */
        private void drawCellText(String text, float cellX, float cellBottom,
                                  float cellW, float cellH, int size,
                                  boolean alignRight) throws IOException {
            String shaped = PersianTextShaper.shape(text == null ? "" : text);
            shaped = sanitize(shaped);

            // اگر متن در اندازه‌ی فعلی جا نمی‌شود، فونت را پله‌پله کوچک کن
            int trySize = size;
            float w = textWidth(shaped, trySize);
            float maxW = cellW - 4f;
            while (w > maxW && trySize > 6) {
                trySize--;
                w = textWidth(shaped, trySize);
            }
            // اگر حتی در ۶pt هم جا نشد، ellipsize کن
            if (w > maxW) {
                shaped = ellipsize(shaped, maxW, trySize);
                w = textWidth(shaped, trySize);
            }

            float tx;
            if (alignRight) tx = cellX + cellW - w - 3f;
            else            tx = cellX + (cellW - w) / 2f;
            float ty = cellBottom + (cellH - trySize) / 2f + 1f;

            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.beginText();
            cs.setFont(font, trySize);
            cs.newLineAtOffset(tx, ty);
            cs.showText(shaped);
            cs.endText();
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