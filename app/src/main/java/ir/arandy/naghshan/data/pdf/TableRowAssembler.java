package ir.arandy.naghshan.data.pdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.PersianNumberParser;

/**
 * عناصر متنی داخل TableRegion را به ردیف‌های Kala تبدیل می‌کند.
 * منطق: هر «شماره ردیف» که در ستون اول ظاهر شود، یک رکورد جدید می‌سازد.
 */
public class TableRowAssembler {

    private final TableDetector.TableRegion region;

    public TableRowAssembler(TableDetector.TableRegion region) {
        this.region = region;
    }

    public List<Kala> assemble() {
        List<Kala> result = new ArrayList<>();
        if (region == null || region.rows.isEmpty()) return result;

        // مرتب‌سازی عناصر بر اساس (صفحه، y صعودی، x صعودی)
        List<TextElement> sorted = new ArrayList<>(region.rows);
        Collections.sort(sorted, Comparator
                .comparingInt((TextElement e) -> e.pageIndex)
                .thenComparingDouble(e -> e.y)
                .thenComparingDouble(e -> e.x));

        Kala current = null;
        StringBuilder nameBuf = null;

        for (TextElement e : sorted) {
            String t = e.text.trim();
            if (t.isEmpty()) continue;

            int col = columnOf(e.x);

            // ستون اول = ردیف؟ فقط اگر یک عدد صحیح کوچک باشد.
            if (col == 0 && isIntegerSmall(t)) {
                // شروع رکورد جدید
                if (current != null) {
                    if (nameBuf != null) current.setProductName(nameBuf.toString().trim());
                    result.add(current);
                }
                current = new Kala();
                current.setRowNumber((int) PersianNumberParser.parseLong(t));
                nameBuf = new StringBuilder();
                continue;
            }

            if (current == null) continue; // قبل از اولین ردیف، نادیده بگیر.

            switch (col) {
                case 1: // کد
                    current.setProductCode(t);
                    break;
                case 2: // نام کالا (احتمالاً چندخطی)
                    if (nameBuf.length() > 0) nameBuf.append(' ');
                    nameBuf.append(t);
                    break;
                case 3: // واحد
                    current.setUnit(t);
                    break;
                case 4: // نوع
                    current.setType(t);
                    break;
                case 5: // تعداد
                    current.setQuantity(PersianNumberParser.parseLong(t));
                    break;
                case 6: // مبلغ جنس
                    current.setMaterialPrice(PersianNumberParser.parseLong(t));
                    break;
                case 7: // مبلغ دستمزد
                    current.setLaborPrice(PersianNumberParser.parseLong(t));
                    break;
                default:
                    // ستون ناشناخته: بی‌خیال
            }
        }

        if (current != null) {
            if (nameBuf != null) current.setProductName(nameBuf.toString().trim());
            result.add(current);
        }
        return result;
    }

    private int columnOf(float x) {
        float[] b = region.columnBounds;
        if (b == null) return -1;
        for (int i = 0; i < b.length - 1; i++)
            if (x >= b[i] && x < b[i + 1]) return i;
        return b.length - 2;
    }

    private boolean isIntegerSmall(String s) {
        String t = PersianNumberParser.stripSeparators(PersianNumberParser.toLatinDigits(s));
        if (t.isEmpty() || t.length() > 4) return false;
        for (char c : t.toCharArray()) if (!Character.isDigit(c)) return false;
        return true;
    }
}