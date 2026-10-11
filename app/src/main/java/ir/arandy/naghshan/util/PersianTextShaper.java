package ir.arandy.naghshan.util;

import java.text.Bidi;
import java.util.HashMap;
import java.util.Map;

/**
 * تبدیل متن فارسی/عربی به شکل نمایشی برای PDFBox.
 * PDFBox خودش Arabic shaping و BiDi انجام نمی‌دهد (GSUB را پشتیبانی نمی‌کند)،
 * پس حروف را به فرم‌های Presentation Forms تبدیل و ترتیب را بصری می‌کنیم.
 */
public final class PersianTextShaper {

    private PersianTextShaper() { }

    private static final char ZWNJ = '\u200C';

    /** هر حرف → [isolated, final, initial, medial] — 0 یعنی ندارد. */
    private static final Map<Character, char[]> FORMS = new HashMap<>();
    static {
        FORMS.put('\u0621', new char[]{'\uFE80', 0,        0,        0});        // ء
        FORMS.put('\u0622', new char[]{'\uFE81', '\uFE82', 0,        0});        // آ
        FORMS.put('\u0623', new char[]{'\uFE83', '\uFE84', 0,        0});        // أ
        FORMS.put('\u0625', new char[]{'\uFE87', '\uFE88', 0,        0});        // إ
        FORMS.put('\u0627', new char[]{'\uFE8D', '\uFE8E', 0,        0});        // ا
        FORMS.put('\u0628', new char[]{'\uFE8F', '\uFE90', '\uFE91', '\uFE92'}); // ب
        FORMS.put('\u062A', new char[]{'\uFE95', '\uFE96', '\uFE97', '\uFE98'}); // ت
        FORMS.put('\u062B', new char[]{'\uFE99', '\uFE9A', '\uFE9B', '\uFE9C'}); // ث
        FORMS.put('\u062C', new char[]{'\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0'}); // ج
        FORMS.put('\u062D', new char[]{'\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4'}); // ح
        FORMS.put('\u062E', new char[]{'\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8'}); // خ
        FORMS.put('\u062F', new char[]{'\uFEA9', '\uFEAA', 0,        0});        // د
        FORMS.put('\u0630', new char[]{'\uFEAB', '\uFEAC', 0,        0});        // ذ
        FORMS.put('\u0631', new char[]{'\uFEAD', '\uFEAE', 0,        0});        // ر
        FORMS.put('\u0632', new char[]{'\uFEAF', '\uFEB0', 0,        0});        // ز
        FORMS.put('\u0633', new char[]{'\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4'}); // س
        FORMS.put('\u0634', new char[]{'\uFEB5', '\uFEB6', '\uFEB7', '\uFEB8'}); // ش
        FORMS.put('\u0635', new char[]{'\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC'}); // ص
        FORMS.put('\u0636', new char[]{'\uFEBD', '\uFEBE', '\uFEBF', '\uFEC0'}); // ض
        FORMS.put('\u0637', new char[]{'\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4'}); // ط
        FORMS.put('\u0638', new char[]{'\uFEC5', '\uFEC6', '\uFEC7', '\uFEC8'}); // ظ
        FORMS.put('\u0639', new char[]{'\uFEC9', '\uFECA', '\uFECB', '\uFECC'}); // ع
        FORMS.put('\u063A', new char[]{'\uFECD', '\uFECE', '\uFECF', '\uFED0'}); // غ
        FORMS.put('\u0641', new char[]{'\uFED1', '\uFED2', '\uFED3', '\uFED4'}); // ف
        FORMS.put('\u0642', new char[]{'\uFED5', '\uFED6', '\uFED7', '\uFED8'}); // ق
        FORMS.put('\u0643', new char[]{'\uFED9', '\uFEDA', '\uFEDB', '\uFEDC'}); // ك عربی
        FORMS.put('\u0644', new char[]{'\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0'}); // ل
        FORMS.put('\u0645', new char[]{'\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4'}); // م
        FORMS.put('\u0646', new char[]{'\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8'}); // ن
        FORMS.put('\u0647', new char[]{'\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC'}); // ه ← اصلاح شد
        FORMS.put('\u0648', new char[]{'\uFEED', '\uFEEE', 0,        0});        // و ← اصلاح شد
        FORMS.put('\u0649', new char[]{'\uFEEF', '\uFEF0', 0,        0});        // ى
        FORMS.put('\u064A', new char[]{'\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4'}); // ي عربی
        FORMS.put('\u0629', new char[]{'\uFE93', '\uFE94', 0,        0});        // ة
        FORMS.put('\u067E', new char[]{'\uFB56', '\uFB57', '\uFB58', '\uFB59'}); // پ
        FORMS.put('\u0686', new char[]{'\uFB7A', '\uFB7B', '\uFB7C', '\uFB7D'}); // چ
        FORMS.put('\u0698', new char[]{'\uFB8A', '\uFB8B', 0,        0});        // ژ
        FORMS.put('\u06A9', new char[]{'\uFB8E', '\uFB8F', '\uFB90', '\uFB91'}); // ک
        FORMS.put('\u06AF', new char[]{'\uFB92', '\uFB93', '\uFB94', '\uFB95'}); // گ
        FORMS.put('\u06CC', new char[]{'\uFBFC', '\uFBFD', '\uFBFE', '\uFBFF'}); // ی
    }

    public static String shape(String input) {
        if (input == null || input.isEmpty()) return "";
        return bidi(shapeLetters(input));
    }

    private static String shapeLetters(String input) {
        int len = input.length();
        StringBuilder out = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            char[] forms = FORMS.get(c);
            if (forms == null) { out.append(c); continue; }

            boolean joinPrev = false;
            if (i > 0) {
                char prev = input.charAt(i - 1);
                if (prev != ZWNJ) {
                    char[] pf = FORMS.get(prev);
                    joinPrev = (pf != null && (pf[2] != 0 || pf[3] != 0));
                }
            }
            boolean joinNext = false;
            if (i < len - 1) {
                char next = input.charAt(i + 1);
                if (next != ZWNJ) {
                    char[] nf = FORMS.get(next);
                    joinNext = (nf != null && (nf[1] != 0 || nf[3] != 0));
                }
            }

            char chosen;
            if (joinPrev && joinNext && forms[3] != 0)      chosen = forms[3];
            else if (joinPrev && forms[1] != 0)             chosen = forms[1];
            else if (joinNext && forms[2] != 0)             chosen = forms[2];
            else                                            chosen = forms[0];
            out.append(chosen);
        }
        return combineLamAlef(out.toString());
    }

    private static String combineLamAlef(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '\uFEDF' || c == '\uFEE0') && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == '\uFE8D' || next == '\uFE8E') {
                    sb.append(c == '\uFEDF' ? '\uFEFB' : '\uFEFC');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String bidi(String input) {
        Bidi bidi = new Bidi(input, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        if (bidi.isLeftToRight()) return input;
        StringBuilder out = new StringBuilder(input.length());
        int n = bidi.getRunCount();
        for (int i = 0; i < n; i++) {
            int start = bidi.getRunStart(i);
            int limit = bidi.getRunLimit(i);
            int level = bidi.getRunLevel(i);
            String run = input.substring(start, limit);
            if ((level & 1) == 1) out.append(new StringBuilder(run).reverse());
            else                   out.append(run);
        }
        return out.toString();
    }
}