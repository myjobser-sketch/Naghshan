package ir.arandy.naghshan.data.pdf;

import android.util.Log;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;

/** تشخیص جدول‌های بخش «لیست کالا». */
public class TableDetector {

    private static final String TAG = "NAGHSHAN_PARSER";

    public static class DetectedTable {
        public String name;
        public int pageStart, pageEnd;
        public float topY;
        public float rightX;   // X لبه‌ی راست جدول
        public float leftX;    // X لبه‌ی چپ جدول
        public int nCols = 9;
        public float[] colCenters;      // مرکز X نه سرستون، از راست به چپ
        public boolean visualOrder;     // true = حروف کلمات وارونه (ترتیب بصری) استخراج شده
        public List<TextElement> rows = new ArrayList<>();
    }

    private static final String[] TABLE_TITLE_PATTERNS = {
            "پست هوایی", "پست زمینی",
            "شبکه فشارمتوسط", "شبکه فشار متوسط",
            "شبکه فشارضعیف", "شبکه فشار ضعیف",
            "روشنایی معابر"
    };

    private static final boolean DEBUG_WORDS = false;

    public List<DetectedTable> detectAll(List<TextElement> elements) {
        Log.i(TAG, "detectAll: elements=" + (elements == null ? 0 : elements.size()));
        List<DetectedTable> result = new ArrayList<>();
        if (elements == null || elements.isEmpty()) return result;

        List<Line> lines = groupIntoLines(elements);
        Log.i(TAG, "Total lines: " + lines.size());

        List<Integer> headerIdx = new ArrayList<>();
        List<Boolean> headerVisual = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int mode = headerMode(lines.get(i));
            if (mode != 0) {
                headerIdx.add(i);
                headerVisual.add(mode == 2);
                Log.i(TAG, "Header found at line " + i + " (page " + lines.get(i).pageIndex
                        + ", visualOrder=" + (mode == 2) + ")");
            }
        }

        if (headerIdx.isEmpty()) {
            Log.w(TAG, "detectAll: NO header found. Sample lines of page 12:");
            int shown = 0;
            for (Line ln : lines) {
                if (ln.pageIndex != 11 || shown >= 25) continue;
                StringBuilder sb = new StringBuilder();
                for (TextElement e : ln.elements) sb.append(e.text).append(" | ");
                Log.w(TAG, "  L" + shown + ": " + sb);
                shown++;
            }
            return result;
        }

        DetectedTable last = null;
        for (int h = 0; h < headerIdx.size(); h++) {
            int hi = headerIdx.get(h);
            Line hl = lines.get(hi);
            boolean visual = headerVisual.get(h);

            int titleIdx = findTitleLine(lines, hi, visual);
            boolean isNew = (titleIdx >= 0 || last == null);
            DetectedTable t;
            if (isNew) {
                t = new DetectedTable();
                t.name = (titleIdx >= 0) ? extractTitle(lines.get(titleIdx), visual) : "لیست کالا";
                t.pageStart = hl.pageIndex;
                t.topY = hl.y;
                t.visualOrder = visual;
                t.colCenters = buildColCenters(hl.elements);
                float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                for (TextElement e : hl.elements) {
                    if (e.x < minX) minX = e.x;
                    if (e.x + e.width > maxX) maxX = e.x + e.width;
                }
                t.leftX = minX;
                t.rightX = maxX;
                Log.i(TAG, "New table '" + t.name + "' page=" + t.pageStart
                        + " rightX=" + t.rightX + " leftX=" + t.leftX
                        + " colCenters=" + (t.colCenters == null ? "null(fallback)" : "ok"));
            } else {
                t = last;
                Log.i(TAG, "Header on page " + hl.pageIndex + " = continuation of '" + t.name + "'");
            }

            int endLine = lines.size();
            if (h + 1 < headerIdx.size()) {
                endLine = headerIdx.get(h + 1);
                int nextTitle = findTitleLine(lines, endLine, headerVisual.get(h + 1));
                if (nextTitle > hi) endLine = nextTitle;
            }

            int before = t.rows.size();
            for (int i = hi + 1; i < endLine; i++) {
                Line ln = lines.get(i);
                if (ln.pageIndex != hl.pageIndex) continue;   // فقط ردیف‌های همان صفحه هدر
                t.rows.addAll(ln.elements);
            }
            Log.i(TAG, "  rows collected on page " + hl.pageIndex + ": " + (t.rows.size() - before));

            if (t.rows.size() > before) {
                t.pageEnd = hl.pageIndex;
                if (isNew) {
                    result.add(t);
                    last = t;
                }
            }
        }
        Log.i(TAG, "detectAll: tables=" + result.size());
        return result;
    }

    // ----------------------------------------------------------------

    private List<Line> groupIntoLines(List<TextElement> elements) {
        List<TextElement> sorted = new ArrayList<>(elements);
        Collections.sort(sorted, Comparator
                .comparingInt((TextElement e) -> e.pageIndex)
                .thenComparingDouble(e -> e.y));

        List<Line> lines = new ArrayList<>();
        Line cur = null;
        for (TextElement e : sorted) {
            if (cur == null
                    || e.pageIndex != cur.pageIndex
                    || Math.abs(e.y - cur.y) > 4f) {
                cur = new Line();
                cur.pageIndex = e.pageIndex;
                cur.y = e.y;
                lines.add(cur);
            }
            cur.elements.add(e);
        }
        return lines;
    }

    /** 0 = هدر نیست، 1 = متن منطقی، 2 = متن بصری (حروف وارونه). */
    private int headerMode(Line line) {
        StringBuilder sb = new StringBuilder();
        for (TextElement e : line.elements) sb.append(norm(e.text)).append(' ');
        String text = sb.toString();

        boolean logical = text.contains("ردیف") && text.contains("تعداد") && text.contains("واحد");
        boolean visual  = text.contains("فیدر") && text.contains("دادعت") && text.contains("دحاو");
        int mode = logical ? 1 : (visual ? 2 : 0);
        if (mode != 0) Log.d(TAG, "headerMode: mode=" + mode + " text=" + text);
        return mode;
    }

    /** اندیس خط عنوان جدول (حداکثر ۶ خط بالای هدر، فقط در همان صفحه) یا -1. */
    private int findTitleLine(List<Line> lines, int headerLineIdx, boolean visual) {
        if (headerLineIdx <= 0 || headerLineIdx >= lines.size()) return -1;
        int page = lines.get(headerLineIdx).pageIndex;
        int from = Math.max(0, headerLineIdx - 6);
        for (int i = headerLineIdx - 1; i >= from; i--) {
            Line ln = lines.get(i);
            if (ln.pageIndex != page) break;
            StringBuilder sb = new StringBuilder();
            for (TextElement e : ln.elements) sb.append(fixWord(e.text, visual)).append(' ');
            String key = norm(sb.toString());
            for (String pat : TABLE_TITLE_PATTERNS) {
                if (key.contains(norm(pat))) {
                    Log.i(TAG, "findTitleLine: header@" + headerLineIdx + " -> title@" + i);
                    return i;
                }
            }
        }
        Log.i(TAG, "findTitleLine: header@" + headerLineIdx + " -> no title on this page");
        return -1;
    }

    /** فقط بخش عنوان خط را برمی‌دارد (کلمات راست‌ترین تا قبل از «مبلغ ...»). */
    private String extractTitle(Line ln, boolean visual) {
        List<TextElement> es = new ArrayList<>(ln.elements);
        Collections.sort(es, (a, b) -> Float.compare(b.x + b.width / 2f, a.x + a.width / 2f));
        StringBuilder sb = new StringBuilder();
        for (TextElement e : es) {
            String w = fixWord(e.text, visual);
            if (norm(w).contains("مبلغ")) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(w);
        }
        String title = sb.toString().replaceAll("\\s+", " ").trim();
        Log.i(TAG, "extractTitle: '" + title + "'");
        return title.isEmpty() ? "لیست کالا" : title;
    }

    /** مرکز X نه سرستون از راست به چپ؛ اگر تعداد گروه‌ها ۹ نبود null. */
    private float[] buildColCenters(List<TextElement> headerElems) {
        List<TextElement> hs = new ArrayList<>(headerElems);
        Collections.sort(hs, (a, b) -> Float.compare(b.x + b.width / 2f, a.x + a.width / 2f));
        List<float[]> groups = new ArrayList<>();
        for (TextElement e : hs) {
            float x0 = e.x, x1 = e.x + e.width;
            if (!groups.isEmpty()) {
                float[] g = groups.get(groups.size() - 1);
                if (g[0] - x1 < 10f) {              // ← ۶ به ۱۰ تغییر کرد
                    g[0] = Math.min(g[0], x0);
                    g[1] = Math.max(g[1], x1);
                    continue;
                }
            }
            groups.add(new float[]{x0, x1});
        }
        Log.i(TAG, "buildColCenters: header groups=" + groups.size());
        if (groups.size() != 9) {
            for (int i = 0; i < groups.size(); i++) {
                Log.w(TAG, "  group[" + i + "] x0=" + groups.get(i)[0]
                        + " x1=" + groups.get(i)[1]
                        + " cx=" + ((groups.get(i)[0] + groups.get(i)[1]) / 2f));
            }
            return null;
        }
        float[] c = new float[9];
        for (int i = 0; i < 9; i++) c[i] = (groups.get(i)[0] + groups.get(i)[1]) / 2f;
        return c;
    }

    /** NFKC (اشکال نمایشی عربی → حروف پایه) و حذف ZWNJ/فاصله، برای مقایسه. */
    static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replace("\u200c", "").replace("\u200f", "").replace("\u200e", "")
                .replace(" ", "").trim();
        if (DEBUG_WORDS) Log.v(TAG, "norm: '" + s + "' -> '" + n + "'");
        return n;
    }

    /** نرمال‌سازی یک کلمه؛ اگر متن بصری باشد حروف کلمه را برعکس می‌کند (ارقام دست نمی‌خورند). */
    public static String fixWord(String raw, boolean visual) {
        if (raw == null) return "";
        String n = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
        if (!visual) return n;
        boolean letters = false;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.isLetter(c) && c >= 0x0600 && c <= 0x06FF) { letters = true; break; }
        }
        if (!letters) return n;
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = n.length() - 1; i >= 0; i--) {
            char c = n.charAt(i);
            if (c == '(') c = ')'; else if (c == ')') c = '(';
            sb.append(c);
        }
        if (DEBUG_WORDS) Log.v(TAG, "fixWord: '" + raw + "' -> '" + sb + "'");
        return sb.toString();
    }

    private static class Line {
        int pageIndex;
        float y;
        List<TextElement> elements = new ArrayList<>();
    }
}