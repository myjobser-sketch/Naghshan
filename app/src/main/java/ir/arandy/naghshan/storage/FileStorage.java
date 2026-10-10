package ir.arandy.naghshan.storage;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/** مدیریت نام و مسیر فایل‌های خروجی. */
public class FileStorage {

    private final Context ctx;
    public FileStorage(Context ctx) { this.ctx = ctx; }

    public File getOutputDir() {
        File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = ctx.getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public File newExcel(String projectNumber) {
        return new File(getOutputDir(), "nagh-" + safe(projectNumber) + ".xlsx");
    }

    public File newPdf(String projectNumber) {
        return new File(getOutputDir(), "nagh-" + safe(projectNumber) + ".pdf");
    }

    private static String safe(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        return s.replaceAll("[^A-Za-z0-9_-]", "");
    }
}