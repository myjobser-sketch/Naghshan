package ir.arandy.naghshan.data.history;

/**
 * رکورد خروجی ذخیره‌شده در تاریخچه.
 * توجه: برای ذخیره در JSON استفاده می‌شود؛ نه Serializable.
 */
public class ExportRecord {

    public String projectNumber;
    public String applicant;
    public String requestNumber;
    public long   grandTotal;
    public double appliedPercent;
    public String excelPath;   // ممکن است null باشد
    public String pdfPath;     // ممکن است null باشد
    public long   createdAt;   // System.currentTimeMillis()

    @Override
    public String toString() {
        return "ExportRecord{" + projectNumber + ", total=" + grandTotal + "}";
    }
}