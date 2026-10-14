package ir.arandy.naghshan.data.export;

import android.util.Log;

import java.io.Serializable;

/**
 * گزینه‌های ورودی کاربر برای هر خروجی (PDF یا Excel).
 * این کلاس داده‌ی موقتِ هر فاکتور را حمل می‌کند و در Invoice ذخیره نمی‌شود.
 */
public class ExportOptions implements Serializable {

    private static final String TAG = "NAGHSHAN_EXPORT_OPTS";

    public static final String DEFAULT_COLOR = "#F1F5F9";

    public String issueDate;       // تاریخ فاکتور (ورودی دستی کاربر)
    public String invoiceNumber;   // شماره فاکتور (ورودی دستی کاربر)
    public String invoiceNote;     // توضیحات فاکتور (پیش‌فرض از پروفایل)
    public String themeColor;      // رنگ نوار بالا (#RRGGBB)

    public ExportOptions() {
        this.issueDate     = "";
        this.invoiceNumber = "";
        this.invoiceNote   = "";
        this.themeColor    = DEFAULT_COLOR;
    }

    public ExportOptions(String issueDate, String invoiceNumber,
                         String invoiceNote, String themeColor) {
        this.issueDate     = safe(issueDate);
        this.invoiceNumber = safe(invoiceNumber);
        this.invoiceNote   = safe(invoiceNote);
        this.themeColor    = (themeColor == null || themeColor.isEmpty())
                ? DEFAULT_COLOR : themeColor;
    }

    /** لاگ‌گیری برای دیباگ. */
    public void log() {
        Log.i(TAG, "ExportOptions: date='" + issueDate
                + "' number='" + invoiceNumber
                + "' color=" + themeColor
                + " noteLen=" + (invoiceNote == null ? 0 : invoiceNote.length()));
    }

    private static String safe(String s) { return s == null ? "" : s; }
}