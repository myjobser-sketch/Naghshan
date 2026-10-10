package ir.arandy.naghshan.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** قالب‌بندی مبالغ ریالی با جداکننده هزارگان. */
public final class MoneyFormatter {

    private static final DecimalFormat FMT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(',');
        FMT = new DecimalFormat("#,###", sym);
    }

    private MoneyFormatter() { }

    public static String format(long amount) {
        return FMT.format(amount);
    }

    public static String formatWithRial(long amount) {
        return FMT.format(amount) + " ریال";
    }
}