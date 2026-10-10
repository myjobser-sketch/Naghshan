package ir.arandy.naghshan.data.pdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;

/**
 * بر اساس محتوا و مختصات تصمیم می‌گیرد که جدول از کجا شروع می‌شود،
 * هدر کدام است و ستون‌ها کدام محدوده‌ها را دارند.
 */
public class TableDetector {

    public static class TableRegion {
        public int    startPageIndex;
        public int    endPageIndex;
        public float  topY;       // بالای هدر
        public float  bottomY;    // پایین آخرین ردیف
        public float[] columnBounds;   // مرزهای عمودی ستون‌ها
        public String[] columnTitles;
        public List<TextElement> rows = new ArrayList<>();
    }

    private static final String[] HEADER_KEYWORDS = {
            "ردیف", "شرح", "کالا", "واحد", "تعداد",
            "مبلغ", "جنس", "دستمزد", "کد"
    };

    public TableRegion detect(List<TextElement> elements) {
        if (elements == null || elements.isEmpty()) return null;

        // ۱. پیدا کردن هدر جدول
        int headerIndex = findHeaderLine(elements);
        if (headerIndex < 0) return null;

        TextElement headerAnchor = elements.get(headerIndex);

        // ۲. جمع‌آوری همان خط به عنوان هدر ستون‌ها
        List<TextElement> headerLine = collectSameLine(elements, headerAnchor);

        TableRegion region = new TableRegion();
        region.startPageIndex = headerAnchor.pageIndex;
        region.topY = minY(headerLine);
        region.columnBounds = computeColumnBounds(headerLine);
        region.columnTitles = new String[headerLine.size()];
        for (int i = 0; i < headerLine.size(); i++)
            region.columnTitles[i] = headerLine.get(i).text;

        // ۳. همه عناصر پایین‌تر از هدر (در همین صفحه و صفحات بعدی) را جمع کن
        int lastPage = elements.get(elements.size() - 1).pageIndex;
        region.endPageIndex = lastPage;

        for (TextElement e : elements) {
            if (e.pageIndex < region.startPageIndex) continue;
            if (e.pageIndex == region.startPageIndex && e.y <= region.topY + 2) continue;
            region.rows.add(e);
        }

        // ۴. پایین‌ترین y را برای مرز پایین نگه می‌داریم
        float bottom = 0f;
        for (TextElement e : region.rows)
            if (e.y + e.height > bottom) bottom = e.y + e.height;
        region.bottomY = bottom;

        return region;
    }

    // ---------------------------------------------------------

    private int findHeaderLine(List<TextElement> elements) {
        for (int i = 0; i < elements.size(); i++) {
            TextElement e = elements.get(i);
            int matches = 0;
            for (String kw : HEADER_KEYWORDS)
                if (e.text.contains(kw)) matches++;
            if (matches >= 2) {
                // تأیید با بررسی هم‌خط‌هایش
                List<TextElement> line = collectSameLine(elements, e);
                int lineMatches = 0;
                for (TextElement le : line)
                    for (String kw : HEADER_KEYWORDS)
                        if (le.text.contains(kw)) { lineMatches++; break; }
                if (lineMatches >= 3) return i;
            }
        }
        return -1;
    }

    private List<TextElement> collectSameLine(List<TextElement> elements, TextElement anchor) {
        List<TextElement> line = new ArrayList<>();
        float tol = Math.max(anchor.height, 4f) * 0.6f;
        for (TextElement e : elements) {
            if (e.pageIndex != anchor.pageIndex) continue;
            if (Math.abs(e.y - anchor.y) <= tol) line.add(e);
        }
        Collections.sort(line, Comparator.comparingDouble(a -> a.x));
        return line;
    }

    private float[] computeColumnBounds(List<TextElement> headerLine) {
        // برای هر قطعه هدر، وسط افقی را مرز می‌گیریم.
        Collections.sort(headerLine, Comparator.comparingDouble(e -> e.x));
        float[] bounds = new float[headerLine.size() + 1];
        bounds[0] = 0f;
        for (int i = 0; i < headerLine.size(); i++) {
            TextElement e = headerLine.get(i);
            bounds[i + 1] = e.x + e.width / 2f;
        }
        // آخرین مرز: بزرگ‌تر از آخرین ستون
        TextElement last = headerLine.get(headerLine.size() - 1);
        bounds[bounds.length - 1] = last.x + last.width + 20f;
        return bounds;
    }

    private float minY(List<TextElement> list) {
        float v = Float.MAX_VALUE;
        for (TextElement e : list) if (e.y < v) v = e.y;
        return v;
    }
}