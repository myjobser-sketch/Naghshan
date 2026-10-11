package ir.arandy.naghshan.data.pdf;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.PersianNumberParser;

public class TableRowAssembler {

    private static final String TAG = "NAGHSHAN_PARSER";
    private static final float MAX_ROW_DY = 40f;

    private final TableDetector.DetectedTable table;

    public TableRowAssembler(TableDetector.DetectedTable table) {
        this.table = table;
    }

    public List<Kala> assemble() {
        Log.i(TAG, "assemble: table='" + (table == null ? "null" : table.name) + "'");
        List<Kala> result = new ArrayList<>();
        if (table == null || table.rows.isEmpty()) return result;

        float[] bounds = buildBounds();
        if (bounds == null) return result;

        List<TextElement> sorted = new ArrayList<>(table.rows);
        Collections.sort(sorted, Comparator
                .comparingInt((TextElement e) -> e.pageIndex)
                .thenComparingDouble(e -> e.y));

        // ---- لنگرها ----
        List<TextElement> anchors = new ArrayList<>();
        int prev = 0;
        for (TextElement e : sorted) {
            if (columnOf(e, bounds) != 0 || !isSmallInteger(e.text)) continue;
            int v = (int) PersianNumberParser.parseLong(e.text);
            boolean ok = (prev == 0) ? (v == 1) : (v > prev && v <= prev + 3);
            if (!ok) continue;
            anchors.add(e);
            prev = v;
        }
        int n = anchors.size();
        Log.i(TAG, "  anchors=" + n);
        if (n == 0) return result;

        // ---- Bucketها ----
        List<List<List<TextElement>>> buckets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<List<TextElement>> cols = new ArrayList<>();
            for (int c = 0; c < 9; c++) cols.add(new ArrayList<TextElement>());
            buckets.add(cols);
        }
        int dropped = 0;
        for (TextElement e : sorted) {
            int best = -1;
            float bestD = Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                TextElement a = anchors.get(i);
                if (a.pageIndex != e.pageIndex) continue;
                float d = Math.abs(a.y - e.y);
                if (d < bestD) { bestD = d; best = i; }
            }
            if (best < 0 || bestD > MAX_ROW_DY) { dropped++; continue; }
            buckets.get(best).get(columnOf(e, bounds)).add(e);
        }

        // ---- ساخت Kala ----
        long sumTotal = 0;
        for (int i = 0; i < n; i++) {
            List<List<TextElement>> cols = buckets.get(i);
            Kala k = new Kala();
            k.setTableName(table.name);
            k.setRowNumber((int) PersianNumberParser.parseLong(anchors.get(i).text));

            // ستون ۱: کد کالا — عدد خالص
            k.setProductCode(PersianNumberParser.toLatinDigits(
                    joinNumericCell(cols.get(1))));

            // ستون ۲: نام کالا — متن
            k.setProductName(clean(joinTextCell(cols.get(2), false)));

            // ستون ۳: واحد — متن کوتاه
            k.setUnit(clean(joinTextCell(cols.get(3), true)));

            // ستون ۴: نوع — متن کوتاه
            k.setType(clean(joinTextCell(cols.get(4), false)));

            // ستون ۵: تعداد — عدد
            k.setQuantity(PersianNumberParser.parseQuantity(joinNumericCell(cols.get(5))));

            // ستون ۶، ۷، ۸: مبالغ — عدد
            k.setMaterialPrice(PersianNumberParser.parseLong(joinNumericCell(cols.get(6))));
            k.setLaborPrice(PersianNumberParser.parseLong(joinNumericCell(cols.get(7))));
            k.setTotalPrice(PersianNumberParser.parseLong(joinNumericCell(cols.get(8))));

            sumTotal += k.getTotalPrice();
            result.add(k);
            if (i < 3) {
                Log.d(TAG, "  row " + k.getRowNumber() + " code=" + k.getProductCode()
                        + " name=" + k.getProductName() + " unit=" + k.getUnit()
                        + " qty=" + k.getQuantity() + " total=" + k.getTotalPrice());
            }
        }

        Log.i(TAG, "  Table '" + table.name + "': anchors=" + n + " dropped=" + dropped
                + " items=" + result.size() + " sumTotal=" + sumTotal);
        return result;
    }

    // ----------------------------------------------------------------

    private float[] buildBounds() {
        float[] c = table.colCenters;
        if (c == null || c.length != 9) {
            if (table.rightX <= table.leftX) return null;
            c = new float[9];
            float w = (table.rightX - table.leftX) / 9f;
            for (int i = 0; i < 9; i++) c[i] = table.rightX - w * (i + 0.5f);
        }
        float[] b = new float[8];
        for (int i = 0; i < 8; i++) b[i] = (c[i] + c[i + 1]) / 2f;
        b[1] = 2f * c[1] - b[0];
        b[2] = 2f * c[3] - b[3];
        return b;
    }

    private int columnOf(TextElement e, float[] b) {
        float cx = e.x + e.width / 2f;
        for (int i = 0; i < 8; i++) if (cx > b[i]) return i;
        return 8;
    }

    // ----------------------------------------------------------------
    //  سلول‌های عددی: مرتب‌سازی صعودی X (چپ به راست)، بدون هیچ تغییر دیگر
    // ----------------------------------------------------------------

    private String joinNumericCell(List<TextElement> els) {
        if (els == null || els.isEmpty()) return "";

        List<TextElement> s = new ArrayList<>(els);
        // برای سلول عددی: از چپ به راست (X صعودی)
        Collections.sort(s, (a, b) -> Float.compare(a.x, b.x));

        StringBuilder out = new StringBuilder();
        for (TextElement e : s) {
            if (out.length() > 0) out.append(' ');
            String t = e.text == null ? "" : e.text.trim();
            out.append(PersianNumberParser.toLatinDigits(t));
        }
        return out.toString().trim();
    }

    // ----------------------------------------------------------------
    //  سلول‌های متنی: مرتب‌سازی نزولی X (راست به چپ) + اصلاح ارقام جاسازی‌شده
    // ----------------------------------------------------------------

    private String joinTextCell(List<TextElement> els, boolean glueLines) {
        if (els == null || els.isEmpty()) return "";

        List<TextElement> s = new ArrayList<>(els);
        Collections.sort(s, (a, b) -> Float.compare(a.y, b.y));

        StringBuilder out = new StringBuilder();
        List<TextElement> line = new ArrayList<>();
        float lineY = s.get(0).y;
        for (TextElement e : s) {
            if (Math.abs(e.y - lineY) > 3f) {
                appendTextLine(out, line, glueLines);
                line.clear();
                lineY = e.y;
            }
            line.add(e);
        }
        appendTextLine(out, line, glueLines);
        return out.toString().trim();
    }

    private void appendTextLine(StringBuilder out, List<TextElement> line, boolean glue) {
        if (line.isEmpty()) return;
        // ستون متنی: از راست به چپ
        Collections.sort(line, (a, b) -> Float.compare(b.x, a.x));

        StringBuilder ln = new StringBuilder();
        for (TextElement e : line) {
            if (ln.length() > 0) ln.append(' ');
            ln.append(TableDetector.fixWord(e.text, table.visualOrder));
        }
        // اصلاح ارقام وارونه درون متن فارسی (اگر وجود دارد)
        String fixed = reverseEmbeddedDigits(ln.toString());
        if (out.length() > 0 && !glue) out.append(' ');
        out.append(fixed);
    }

    /**
     * در متن‌های فارسی که PDFsharp آن‌ها را RTL ذخیره کرده، توالی‌های عددی
     * جاسازی‌شده برعکس شده‌اند. این متد ONLY توالی‌های عددیِ درون رشته‌ای
     * که حاوی حروف فارسی است را برمی‌گرداند. رشته‌های کاملاً عددی یا لاتین
     * بدون تغییر باقی می‌مانند.
     */
    private static String reverseEmbeddedDigits(String s) {
        if (s == null || s.isEmpty()) return "";

        // شرط ۱: رشته باید حاوی حروف فارسی/عربی باشد
        boolean hasArabicLetter = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // حروف (نه ارقام) فارسی/عربی
            if ((c >= 0x0620 && c <= 0x064A) ||
                    (c >= 0x066E && c <= 0x06D3) ||
                    (c >= 0x06FA && c <= 0x06FF) ||
                    (c >= 0xFB50 && c <= 0xFDFF) ||
                    (c >= 0xFE70 && c <= 0xFEFF)) {
                hasArabicLetter = true;
                break;
            }
        }
        if (!hasArabicLetter) return s;   // رشته‌ی خالص عددی/لاتین: دست نزن

        // شرط ۲: رشته نباید کاملاً عدد باشد (باید محتوای غیرعددی داشته باشد)
        boolean hasNonDigit = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c) && c != '.' && c != '/' && c != ' ' && c != ',') {
                hasNonDigit = true;
                break;
            }
        }
        if (!hasNonDigit) return s;       // کاملاً عددی: دست نزن

        // اصلاح: هر توالی عددی جداگانه را برگردان
        StringBuilder out = new StringBuilder(s.length());
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '/') {
                buf.append(c);
            } else {
                if (buf.length() > 0) {
                    out.append(buf.reverse());
                    buf.setLength(0);
                }
                out.append(c);
            }
        }
        if (buf.length() > 0) out.append(buf.reverse());
        return out.toString();
    }

    private boolean isSmallInteger(String s) {
        if (s == null) return false;
        String t = PersianNumberParser.toLatinDigits(s)
                .replace(",", "").replace(".", "")
                .replace("٬", "").replace("،", "").trim();
        if (t.isEmpty() || t.length() > 4) return false;
        for (char c : t.toCharArray())
            if (!Character.isDigit(c)) return false;
        return true;
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("|", "").trim();
    }
}