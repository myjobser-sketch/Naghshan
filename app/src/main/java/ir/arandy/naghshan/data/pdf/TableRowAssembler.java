package ir.arandy.naghshan.data.pdf;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.PersianNumberParser;

/**
 * تبدیل ردیف‌ها به Kala.
 * روش: شماره ردیف به‌عنوان «لنگر» و اختصاص هر المان به نزدیک‌ترین لنگر (Y).
 * مرزهای ستون از مرکز واقعی سرستون‌ها ساخته می‌شود.
 */
public class TableRowAssembler {

    private static final String TAG = "NAGHSHAN_PARSER";

    /** بیشینه فاصله Y یک المان تا لنگر ردیف (ردیف‌های چندخطی وسط‌چین‌اند). */
    private static final float MAX_ROW_DY = 40f;

    private final TableDetector.DetectedTable table;

    public TableRowAssembler(TableDetector.DetectedTable table) {
        this.table = table;
    }

    public List<Kala> assemble() {
        Log.i(TAG, "assemble: table='" + (table == null ? "null" : table.name) + "'");
        List<Kala> result = new ArrayList<>();
        if (table == null || table.rows.isEmpty()) {
            Log.w(TAG, "assemble: table is empty");
            return result;
        }
        float[] bounds = buildBounds();
        if (bounds == null) return result;

        List<TextElement> sorted = new ArrayList<>(table.rows);
        Collections.sort(sorted, Comparator
                .comparingInt((TextElement e) -> e.pageIndex)
                .thenComparingDouble(e -> e.y));

        // ---- ۱) لنگرها: عدد ردیف در ستون ۰ با شمارش متوالی (۱، ۲، ۳، ...) ----
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

        // ---- ۲) هر المان به نزدیک‌ترین لنگر (همان صفحه) نسبت داده می‌شود ----
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

        // ---- ۳) ساخت Kala ----
        long sumTotal = 0;
        for (int i = 0; i < n; i++) {
            List<List<TextElement>> cols = buckets.get(i);
            Kala k = new Kala();
            k.setTableName(table.name);
            k.setRowNumber((int) PersianNumberParser.parseLong(anchors.get(i).text));
            k.setProductCode(PersianNumberParser.toLatinDigits(joinCell(cols.get(1), true)));
            k.setProductName(clean(joinCell(cols.get(2), false)));
            k.setUnit(clean(joinCell(cols.get(3), true)));
            k.setType(clean(joinCell(cols.get(4), false)));
            k.setQuantity(PersianNumberParser.parseQuantity(joinCell(cols.get(5), true)));
            k.setMaterialPrice(PersianNumberParser.parseLong(joinCell(cols.get(6), true)));
            k.setLaborPrice(PersianNumberParser.parseLong(joinCell(cols.get(7), true)));
            k.setTotalPrice(PersianNumberParser.parseLong(joinCell(cols.get(8), true)));
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

    /** مرز بین ۹ ستون (۸ مرز، راست → چپ) از مرکز سرستون‌ها. */
    private float[] buildBounds() {
        float[] c = table.colCenters;
        if (c == null || c.length != 9) {
            if (table.rightX <= table.leftX) {
                Log.w(TAG, "buildBounds: invalid X range rightX=" + table.rightX
                        + " leftX=" + table.leftX);
                return null;
            }
            c = new float[9];
            float w = (table.rightX - table.leftX) / 9f;
            for (int i = 0; i < 9; i++) c[i] = table.rightX - w * (i + 0.5f);
            Log.w(TAG, "buildBounds: header groups != 9, using equal-width fallback");
        }
        float[] b = new float[8];
        for (int i = 0; i < 8; i++) b[i] = (c[i] + c[i + 1]) / 2f;
        // دو ستون پهن (کد|نام و نام|واحد): مرز را با آینه‌کردن نیمه‌پهنای ستون باریک کناری بساز
        b[1] = 2f * c[1] - b[0];
        b[2] = 2f * c[3] - b[3];
        StringBuilder sb = new StringBuilder();
        for (float v : b) sb.append(Math.round(v)).append(' ');
        Log.i(TAG, "buildBounds: " + sb);
        return b;
    }

    /** ایندکس ستون (۰ = ردیف ... ۸ = مبلغ کل) بر اساس مرکز X المان. */
    private int columnOf(TextElement e, float[] b) {
        float cx = e.x + e.width / 2f;
        for (int i = 0; i < 8; i++) {
            if (cx > b[i]) return i;
        }
        return 8;
    }

    /** متن یک سلول: خطوط از بالا به پایین، کلمات هر خط از راست به چپ. */
    private String joinCell(List<TextElement> els, boolean glueLines) {
        if (els == null || els.isEmpty()) return "";
        List<TextElement> s = new ArrayList<>(els);
        Collections.sort(s, (a, b) -> Float.compare(a.y, b.y));
        StringBuilder out = new StringBuilder();
        List<TextElement> line = new ArrayList<>();
        float lineY = s.get(0).y;
        for (TextElement e : s) {
            if (Math.abs(e.y - lineY) > 3f) {
                appendLine(out, line, glueLines);
                line.clear();
                lineY = e.y;
            }
            line.add(e);
        }
        appendLine(out, line, glueLines);
        return out.toString().trim();
    }

    private void appendLine(StringBuilder out, List<TextElement> line, boolean glue) {
        if (line.isEmpty()) return;
        Collections.sort(line, (a, b) -> Float.compare(b.x, a.x));   // راست → چپ
        StringBuilder ln = new StringBuilder();
        for (TextElement e : line) {
            if (ln.length() > 0) ln.append(' ');
            ln.append(TableDetector.fixWord(e.text, table.visualOrder));
        }
        if (out.length() > 0 && !glue) out.append(' ');
        out.append(ln);
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