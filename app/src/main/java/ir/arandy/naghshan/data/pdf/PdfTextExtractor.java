package ir.arandy.naghshan.data.pdf;

import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * استخراج متن و مختصات هر «سلول» از PDF.
 *
 * مشکل PDFBox در PDFهای فارسی جدولی:
 * یک writeString ممکن است کل ردیف (۹ سلول) را به‌صورت یک رشته‌ی
 * پیوسته تحویل بدهد. این باعث می‌شود TableDetector نتواند
 * ستون‌ها را تشخیص دهد.
 *
 * راه‌حل: در همین متد، کاراکترها را بر اساس فاصله‌ی افقی (X)
 * بین دو کاراکتر متوالی به «سلول» می‌شکنیم. ترتیب کاراکترها
 * دست‌نخورده می‌ماند تا متن منطقی (logical) که PDFBox می‌دهد
 * حفظ شود.
 */
public class PdfTextExtractor {

    private static final String TAG = "NAGHSHAN_PARSER";
    private static final boolean DEBUG_SPLIT = false;

    /**
     * آستانه‌ی گپ: اگر فاصله‌ی مرکز X دو کاراکتر متوالی از این مقدار
     * بیشتر باشد، سلول جدید آغاز می‌شود.
     * مقدار پایه: حداکثر بین ۱۰pt ثابت و ۱.۶ برابر عرض میانگین کاراکتر.
     *   - شکاف حروف یک کلمه:  ~۸pt   → شکسته نمی‌شود
     *   - شکاف بین کلمات:      ~۱۲pt  → ممکن است بشکند (مطلوب)
     *   - شکاف بین ستون‌ها:    ~۲۰pt+ → قطعاً می‌شکند
     */
    private static final float MIN_GAP = 10f;
    private static final float GAP_FACTOR = 1.6f;

    public static class TextElement {
        public final String text;
        public final float  x, y, width, height;
        public final int    pageIndex;
        public TextElement(String t, float x, float y, float w, float h, int p) {
            this.text = t; this.x = x; this.y = y;
            this.width = w; this.height = h; this.pageIndex = p;
        }
    }

    public List<TextElement> extract(File pdf) throws IOException {
        PDDocument doc = PDDocument.load(pdf);
        try {
            List<TextElement> out = new ArrayList<>();
            int pages = doc.getNumberOfPages();
            for (int p = 0; p < pages; p++) {
                PDPage page = doc.getPage(p);
                PageStripper stripper = new PageStripper(out, p);
                stripper.setStartPage(p + 1);
                stripper.setEndPage(p + 1);
                stripper.setSortByPosition(true);
                stripper.getText(doc);
            }
            return out;
        } finally {
            doc.close();
        }
    }

    private static class PageStripper extends PDFTextStripper {
        private final List<TextElement> sink;
        private final int pageIndex;

        PageStripper(List<TextElement> sink, int pageIndex) throws IOException {
            this.sink = sink;
            this.pageIndex = pageIndex;
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            if (positions == null || positions.isEmpty()) return;

            // ۱) میانگین عرض کاراکتر (برای آستانه‌ی تطبیقی)
            float totalW = 0f; int n = 0;
            for (TextPosition tp : positions) {
                totalW += tp.getWidthDirAdj(); n++;
            }
            if (n == 0) return;
            float avgW = totalW / n;
            float gapThreshold = Math.max(MIN_GAP, avgW * GAP_FACTOR);

            // ۲) شکستن به گروه‌ها: مقایسه‌ی فاصله‌ی مرکز X کاراکترهای متوالی
            //    (کار می‌کند هم برای RTL که X کاهشی است، هم LTR که افزایشی است)
            List<List<TextPosition>> groups = new ArrayList<>();
            List<TextPosition> cur = new ArrayList<>();
            TextPosition prev = null;
            for (TextPosition tp : positions) {
                if (prev != null) {
                    float prevC = prev.getXDirAdj() + prev.getWidthDirAdj() / 2f;
                    float curC  = tp.getXDirAdj()   + tp.getWidthDirAdj()   / 2f;
                    float dx = Math.abs(curC - prevC);
                    if (dx > gapThreshold) {
                        groups.add(cur);
                        cur = new ArrayList<>();
                    }
                }
                cur.add(tp);
                prev = tp;
            }
            if (!cur.isEmpty()) groups.add(cur);

            if (DEBUG_SPLIT) {
                Log.v(TAG, "writeString: text='" + text + "' positions=" + positions.size()
                        + " avgW=" + avgW + " gapThresh=" + gapThreshold
                        + " groups=" + groups.size());
            }

            // ۳) برای هر گروه یک TextElement با جعبه‌ی احاطه‌کننده
            for (List<TextPosition> g : groups) {
                StringBuilder sb = new StringBuilder();
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                for (TextPosition tp : g) {
                    String u = tp.getUnicode();
                    if (u != null) sb.append(u);
                    float x = tp.getXDirAdj();
                    float y = tp.getYDirAdj();
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x + tp.getWidthDirAdj() > maxX) maxX = x + tp.getWidthDirAdj();
                    if (y + tp.getHeightDir()  > maxY) maxY = y + tp.getHeightDir();
                }
                String cell = sb.toString().trim();
                if (cell.isEmpty()) continue;
                sink.add(new TextElement(cell, minX, minY,
                        maxX - minX, maxY - minY, pageIndex));
            }
        }
    }
}