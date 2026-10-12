package ir.arandy.naghshan.util;

import android.util.Log;

/**
 * تبدیل عدد صحیح به حروف فارسی.
 * مثال: 1727828770 → "یک میلیارد و هفتصد و بیست و هفت میلیون و هشتصد و بیست و هشت هزار و هفتصد و هفتاد"
 */
public final class MoneyToPersianWords {

    private static final String TAG = "NAGHSHAN_WORDS";

    private static final String[] ONES = {
            "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه",
            "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده",
            "هفده", "هجده", "نوزده"
    };
    private static final String[] TENS = {
            "", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"
    };
    private static final String[] HUNDREDS = {
            "", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد",
            "هشتصد", "نهصد"
    };
    private static final String[] SCALES = {
            "", "هزار", "میلیون", "میلیارد", "بیلیون"
    };

    private MoneyToPersianWords() { }

    /** خروجی: متن حروف فارسی بدون پسوند واحد پول. */
    public static String format(long n) {
        if (n == 0) return "صفر";
        if (n < 0)  return "منفی " + format(-n);

        StringBuilder sb = new StringBuilder();
        int scale = 0;
        while (n > 0) {
            int group = (int) (n % 1000);
            if (group > 0) {
                String g = threeDigits(group);
                if (scale > 0) g += " " + SCALES[scale];
                if (sb.length() > 0) sb.insert(0, " و ");
                sb.insert(0, g);
            }
            n /= 1000;
            scale++;
        }
        String out = sb.toString();
        Log.v(TAG, "format: -> " + out);
        return out;
    }

    /** خروجی با پسوند "ریال". */
    public static String formatWithRial(long n) {
        return format(n) + " ریال";
    }

    private static String threeDigits(int n) {
        StringBuilder sb = new StringBuilder();
        int h = n / 100, r = n % 100;
        if (h > 0) sb.append(HUNDREDS[h]);
        if (r > 0) {
            if (sb.length() > 0) sb.append(" و ");
            if (r < 20) {
                sb.append(ONES[r]);
            } else {
                sb.append(TENS[r / 10]);
                if (r % 10 > 0) sb.append(" و ").append(ONES[r % 10]);
            }
        }
        return sb.toString();
    }
}