package ir.arandy.naghshan.util;

/** تبدیل اعداد فارسی/عربی به لاتین و حذف جداکننده‌ها. */
public final class PersianNumberParser {

    private static final char[] PERSIAN  = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
    private static final char[] ARABIC   = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};

    private PersianNumberParser() { }

    public static String toLatinDigits(String in) {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            int p = indexOf(PERSIAN, c);
            int a = indexOf(ARABIC,  c);
            if (p >= 0) sb.append((char)('0'+p));
            else if (a >= 0) sb.append((char)('0'+a));
            else sb.append(c);
        }
        return sb.toString();
    }

    /** جداکننده‌های رایج: ، ٬ , . و فاصله را حذف می‌کند. */
    public static String stripSeparators(String in) {
        if (in == null) return "";
        return in.replace(",", "")
                .replace("،", "")
                .replace("٬", "")
                .replace(".", "")
                .replace("\u00A0", "")
                .replace(" ", "")
                .trim();
    }

    /** تلاش برای تبدیل یک رشته به long. 0 در صورت شکست. */
    public static long parseLong(String in) {
        try {
            String s = stripSeparators(toLatinDigits(in));
            if (s.isEmpty()) return 0;
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** تلاش برای تبدیل به double. */
    public static double parseDouble(String in) {
        try {
            String s = stripSeparators(toLatinDigits(in));
            if (s.isEmpty()) return 0;
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** تبدیل تعداد که ممکن است اعشاری باشد (مثل 1.5 یا ۰/۸). */
    public static double parseQuantity(String in) {
        if (in == null) return 0;
        String s = toLatinDigits(in).trim();
        // اگر جداکننده اعشار فارسی بود
        s = s.replace("٫", ".").replace("،", "").replace(",", "");
        // حذف جداکننده هزارگان انگلیسی به‌جز نقطه اعشار
        try {
            // اگر عدد صحیح بزرگ بود، نقطه را حذف کن
            if (s.contains(".") && s.length() > 3) {
                // احتمالاً جداکننده هزارگان است، نه اعشار
                String[] parts = s.split("\\.");
                if (parts.length == 2 && parts[1].length() == 3) {
                    s = parts[0] + parts[1];
                }
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int indexOf(char[] arr, char c) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == c) return i;
        return -1;
    }
}