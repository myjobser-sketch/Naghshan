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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.util.MoneyFormatter;
import ir.arandy.naghshan.util.MoneyToPersianWords;
import ir.arandy.naghshan.util.PersianNumberParser;
import ir.arandy.naghshan.util.PersianTextShaper;

/**
 * تولید PDF با ساختار جدولی + سربرگ رنگی + تکرار سربرگ در هر صفحه
 * + شماره صفحه + توضیحات پایانی.
 */
public class PdfExporter {

    private static final String TAG = "NAGHSHAN_PDF";

    private static final float MARGIN   = 40f;
    private static final float PAGE_W   = PDRectangle.A4.getWidth();
    private static final float PAGE_H   = PDRectangle.A4.getHeight();
    private static final float USABLE_W = PAGE_W - 2 * MARGIN;

    private static final float BANNER_H = 46f;
    private static final float INFO_H   = 30f;
    private static final float HEADER_H = 18f;
    private static final float DATA_H   = 16f;

    private static final float[] COL_W = {
            24f, 60f, 130f, 36f, 30f, 36f, 67f, 67f, 60f
    };

    private static final String[] HEADERS = {
            "ردیف", "کد کالا", "نام کالا", "واحد", "نوع",
            "تعداد", "مبلغ جنس", "مبلغ دستمزد", "مبلغ کل"
    };

    // ----------------------------------------------------------------

    public File export(Invoice invoice, File outFile, File persianFontFile,
                       ProfileManager pm, ExportOptions opts) throws IOException {

        if (opts == null) opts = new ExportOptions();
        Log.i(TAG, "export: start -> " + outFile.getName());
        opts.log();

        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = PDType0Font.load(doc, persianFontFile);
            TableWriter tw = new TableWriter(doc, font);

            // ---- تنظیم سربرگ ----
            tw.setBannerData(
                    pm == null ? "" : pm.getCompany(),
                    pm == null ? "" : pm.getFullName(),
                    opts.issueDate,
                    opts.invoiceNumber,
                    opts.themeColor
            );
            tw.setProjectData(
                    invoice.getProjectInfo().getProjectNumber(),
                    invoice.getProjectInfo().getProjectName()
            );

            // صفحه‌ی اول: بنر + اطلاعات طرح
            tw.drawBanner();
            tw.drawProjectInfo();
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

            // ---- جمع کل نهایی ----
            tw.blank(4);
            tw.writeHeader("جمع کل همه جدول‌ها: "
                    + MoneyFormatter.format(invoice.getGrandTotal()) + " ریال", 12);
            tw.blank(4);
            tw.writeHeader("به حروف: " + MoneyToPersianWords.formatWithRial(
                    invoice.getGrandTotal()), 10);

            // ---- توضیحات ----
            if (opts.invoiceNote != null && !opts.invoiceNote.trim().isEmpty()) {
                tw.blank(8);
                tw.drawSeparatorLine();
                tw.writeHeader("توضیحات:", 10);
                tw.drawWrappedText(opts.invoiceNote.trim(), 9, 4);
            }

            tw.close();
            tw.addPageNumbers();
            doc.save(outFile);
            Log.i(TAG, "export: done -> " + outFile.getAbsolutePath());
        }
        return outFile;
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private static String formatQty(double q) {
        if (q == Math.floor(q)) return String.valueOf((long) q);
        return PersianNumberParser.toLatinDigits(String.valueOf(q));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static float[] hexToRgb(String hex) {
        if (hex == null || hex.length() < 7 || hex.charAt(0) != '#') {
            return new float[]{0.95f, 0.96f, 0.98f};
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return new float[]{r / 255f, g / 255f, b / 255f};
        } catch (Exception e) {
            return new float[]{0.95f, 0.96f, 0.98f};
        }
    }

    private static float[] getContrastTextColor(float[] bg) {
        float lum = 0.299f * bg[0] + 0.587f * bg[1] + 0.114f * bg[2];
        if (lum > 0.6f) return new float[]{0.06f, 0.09f, 0.16f};
        return new float[]{1f, 1f, 1f};
    }

    private static String toPersianDigits(int n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') sb.append((char) ('۰' + (c - '0')));
            else sb.append(c);
        }
        return sb.toString();
    }

    // =================================================================
    //  TableWriter — کشیدن جدول + سربرگ + شماره صفحه
    // =================================================================

    private static class TableWriter {

        private final PDDocument doc;
        private final PDFont     font;

        // Banner / project data
        private String bannerCompany   = "";
        private String bannerIssuer    = "";
        private String bannerDate      = "";
        private String bannerNumber    = "";
        private String bannerColorHex  = "#F1F5F9";
        private boolean bannerEnabled  = false;

        private String projNumber = "";
        private String projName   = "";

        // Column header for redraw on new pages
        private String[] pendingHeaderCells;
        private float[]  pendingHeaderWidths;

        // Current page state
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        private final Map<Character, Character> glyphCache = new HashMap<>();

        TableWriter(PDDocument doc, PDFont font) throws IOException {
            this.doc = doc;
            this.font = font;
            newPage();
        }

        // ---------- banner / project data ----------

        void setBannerData(String company, String issuer,
                           String date, String number, String colorHex) {
            this.bannerCompany  = company  == null ? "" : company;
            this.bannerIssuer   = issuer   == null ? "" : issuer;
            this.bannerDate     = date     == null ? "" : date;
            this.bannerNumber   = number   == null ? "" : number;
            this.bannerColorHex = (colorHex == null || colorHex.isEmpty())
                    ? "#F1F5F9" : colorHex;
            this.bannerEnabled  = true;
            Log.d(TAG, "banner set: company='" + company + "' date='" + date
                    + "' number='" + number + "' color=" + colorHex);
        }

        void setProjectData(String number, String name) {
            this.projNumber = number == null ? "" : number;
            this.projName   = name   == null ? "" : name;
        }

        // ---------- page management ----------

        void newPage() throws IOException {
            if (cs != null) cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;

            if (bannerEnabled) {
                drawBannerInternal();
                drawProjectInfoInternal();
                if (pendingHeaderCells != null && pendingHeaderWidths != null) {
                    drawHeaderRowInternal(pendingHeaderCells, pendingHeaderWidths, HEADER_H);
                }
            }
        }

        // ---------- banner ----------

        void drawBanner() throws IOException { drawBannerInternal(); }

        private void drawBannerInternal() throws IOException {
            float[] bg = hexToRgb(bannerColorHex);
            float top = y;

            cs.setNonStrokingColor(bg[0], bg[1], bg[2]);
            cs.addRect(0, top - BANNER_H, PAGE_W, BANNER_H);
            cs.fill();

            float[] fg = getContrastTextColor(bg);
            cs.setNonStrokingColor(fg[0], fg[1], fg[2]);

            // راست: نام شرکت + نام صادرکننده
            if (!bannerCompany.isEmpty()) {
                String c = sanitize(PersianTextShaper.shape(bannerCompany));
                cs.beginText();
                cs.setFont(font, 12);
                float w = textWidth(c, 12);
                cs.newLineAtOffset(PAGE_W - MARGIN - w, top - 16);
                cs.showText(c);
                cs.endText();
            }
            if (!bannerIssuer.isEmpty()) {
                String p = sanitize(PersianTextShaper.shape(bannerIssuer));
                cs.beginText();
                cs.setFont(font, 9);
                float w = textWidth(p, 9);
                cs.newLineAtOffset(PAGE_W - MARGIN - w, top - 32);
                cs.showText(p);
                cs.endText();
            }

            // چپ: تاریخ + شماره فاکتور
            String d = "تاریخ: " + (bannerDate.isEmpty() ? "-" : bannerDate);
            String n = "شماره: " + (bannerNumber.isEmpty() ? "-" : bannerNumber);
            String dShaped = sanitize(PersianTextShaper.shape(d));
            String nShaped = sanitize(PersianTextShaper.shape(n));

            cs.beginText();
            cs.setFont(font, 9);
            cs.newLineAtOffset(MARGIN, top - 16);
            cs.showText(dShaped);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 9);
            cs.newLineAtOffset(MARGIN, top - 32);
            cs.showText(nShaped);
            cs.endText();

            y = top - BANNER_H - 4;
        }

        // ---------- project info ----------

        void drawProjectInfo() throws IOException { drawProjectInfoInternal(); }

        private void drawProjectInfoInternal() throws IOException {
            float top = y;
            float h = INFO_H;

            cs.setNonStrokingColor(0.97f, 0.98f, 0.99f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.fill();

            cs.setStrokingColor(0.60f, 0.65f, 0.72f);
            cs.setLineWidth(0.5f);
            cs.addRect(MARGIN, top - h, USABLE_W, h);
            cs.stroke();

            cs.setNonStrokingColor(0f, 0f, 0f);

            String line1 = sanitize(PersianTextShaper.shape(
                    "شماره طرح: " + (projNumber.isEmpty() ? "-" : projNumber)));
            String line2 = sanitize(PersianTextShaper.shape(
                    "نام طرح: " + (projName.isEmpty() ? "-" : projName)));

            cs.beginText();
            cs.setFont(font, 10);
            float w1 = textWidth(line1, 10);
            cs.newLineAtOffset(PAGE_W - MARGIN - 6 - w1, top - 12);
            cs.showText(line1);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 9);
            float w2 = textWidth(line2, 9);
            cs.newLineAtOffset(PAGE_W - MARGIN - 6 - w2, top - 25);
            cs.showText(line2);
            cs.endText();

            y = top - h - 4;
        }

        // ---------- header redraw ----------

        void enableHeaderRedraw(String[] cells, float[] widths) {
            this.pendingHeaderCells  = cells;
            this.pendingHeaderWidths = widths;
        }

        void disableHeaderRedraw() {
            this.pendingHeaderCells  = null;
            this.pendingHeaderWidths = null;
        }

        private void drawHeaderRowInternal(String[] cells, float[] widths, float h)
                throws IOException {
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

        // ---------- layout helpers ----------

        void ensure(float need) throws IOException {
            if (y - need < MARGIN + 14) newPage();
        }

        void blank(float h) throws IOException {
            y -= h;
            ensure(0);
        }

        void writeHeader(String text, int size) throws IOException {
            ensure(size + 6);
            String shaped = sanitize(PersianTextShaper.shape(text == null ? "" : text));
            float w = textWidth(shaped, size);
            float x = PAGE_W - MARGIN - w;
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y - size);
            cs.showText(shaped);
            cs.endText();
            y -= size + 6;
        }

        void drawHeaderRow(String[] cells, float[] widths, float h) throws IOException {
            ensure(h);
            drawHeaderRowInternal(cells, widths, h);
        }

        void drawDataRow(String[] cells, float[] widths, float h,
                         boolean alternate) throws IOException {
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

            drawCellText(label, PAGE_W - MARGIN - labelW, top - h, labelW, h, 9, true);
            drawCellText(value, MARGIN, top - h, valueW, h, 9, false);
            y = top - h;
        }

        void drawSeparatorLine() throws IOException {
            ensure(8);
            cs.setStrokingColor(0.60f, 0.65f, 0.72f);
            cs.setLineWidth(0.4f);
            cs.moveTo(MARGIN, y - 2);
            cs.lineTo(PAGE_W - MARGIN, y - 2);
            cs.stroke();
            y -= 8;
        }

        /** متن چندخطی با word wrap و محدودیت تعداد خطوط. */
        void drawWrappedText(String text, int size, int maxLines) throws IOException {
            if (text == null || text.isEmpty()) return;
            List<String> lines = wrapText(text, USABLE_W - 10, size);
            int n = Math.min(lines.size(), maxLines);
            for (int i = 0; i < n; i++) {
                String line = lines.get(i);
                if (i == maxLines - 1 && lines.size() > maxLines) {
                    line = line + "...";
                }
                ensure(size + 6);
                String shaped = sanitize(PersianTextShaper.shape(line));
                float w = textWidth(shaped, size);
                cs.setNonStrokingColor(0.10f, 0.15f, 0.20f);
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(PAGE_W - MARGIN - w, y - size);
                cs.showText(shaped);
                cs.endText();
                y -= size + 5;
            }
        }

        // ---------- cell text ----------

        private void drawCellText(String text, float cellX, float cellBottom,
                                  float cellW, float cellH, int size,
                                  boolean alignRight) throws IOException {
            String shaped = sanitize(PersianTextShaper.shape(text == null ? "" : text));

            int trySize = size;
            float w = textWidth(shaped, trySize);
            float maxW = cellW - 4f;
            while (w > maxW && trySize > 6) {
                trySize--;
                w = textWidth(shaped, trySize);
            }
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
            try { return font.getStringWidth(s) / 1000f * size; }
            catch (Exception e) { return 0f; }
        }

        private boolean isGlyphAvailable(char c) {
            Character cached = glyphCache.get(c);
            if (cached != null) return cached != '?';
            boolean ok;
            try {
                font.encode(String.valueOf(c));
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            glyphCache.put(c, ok ? c : '?');
            return ok;
        }

        private String sanitize(String s) {
            if (s == null || s.isEmpty()) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < 0x20 && c != '\t') continue;
                char forced = forceSubstitute(c);
                if (forced != 0) { sb.append(forced); continue; }
                sb.append(isGlyphAvailable(c) ? c : '?');
            }
            return sb.toString();
        }

        private static char forceSubstitute(char c) {
            switch (c) {
                case '-': case '\u2010': case '\u2011':
                case '\u2012': case '\u2013': case '\u2014':
                    return '_';
                case '\u25C4': case '\u25BA': case '\u25B6':
                    return '*';
                default:
                    return 0;
            }
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

        private List<String> wrapText(String text, float maxWidth, int size) {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isEmpty()) return lines;
            String[] words = text.split("\\s+");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.length() == 0 ? w : cur.toString() + " " + w;
                String shaped = PersianTextShaper.shape(test);
                float width = textWidth(shaped, size);
                if (width > maxWidth && cur.length() > 0) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (cur.length() > 0) lines.add(cur.toString());
            return lines;
        }

        // ---------- page numbers ----------

        void addPageNumbers() throws IOException {
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream pcs = new PDPageContentStream(
                        doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    String txt = "صفحه " + toPersianDigits(i + 1)
                            + " از " + toPersianDigits(total);
                    String shaped = sanitize(PersianTextShaper.shape(txt));
                    pcs.setNonStrokingColor(0.40f, 0.45f, 0.50f);
                    pcs.beginText();
                    pcs.setFont(font, 9);
                    pcs.newLineAtOffset(MARGIN, 22);
                    pcs.showText(shaped);
                    pcs.endText();
                }
            }
            Log.d(TAG, "page numbers added: total=" + total);
        }

        void close() throws IOException {
            if (cs != null) cs.close();
        }
    }
}