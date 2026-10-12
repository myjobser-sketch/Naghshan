package ir.arandy.naghshan.data.history;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * مدیریت تاریخچه‌ی خروجی‌ها به‌صورت یک فایل JSON ایندکس.
 * (جایگزین سبک Room).
 */
public class HistoryManager {

    private static final String TAG  = "NAGHSHAN_HISTORY";
    private static final String FILE = "exports_index.json";

    private final File indexFile;

    public HistoryManager(Context ctx) {
        indexFile = new File(ctx.getFilesDir(), FILE);
    }

    public synchronized List<ExportRecord> getAll() {
        List<ExportRecord> list = new ArrayList<>();
        if (!indexFile.exists()) {
            Log.v(TAG, "getAll: index file not exists");
            return list;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(indexFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            if (sb.length() == 0) return list;
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ExportRecord e = new ExportRecord();
                e.projectNumber  = o.optString("projectNumber", "");
                e.applicant      = o.optString("applicant", "");
                e.requestNumber  = o.optString("requestNumber", "");
                e.grandTotal     = o.optLong("grandTotal", 0);
                e.appliedPercent = o.optDouble("appliedPercent", 0);
                e.excelPath      = o.isNull("excelPath") ? null : o.optString("excelPath");
                e.pdfPath        = o.isNull("pdfPath")   ? null : o.optString("pdfPath");
                e.createdAt      = o.optLong("createdAt", 0);
                list.add(e);
            }
        } catch (Exception ex) {
            Log.e(TAG, "getAll failed: " + ex.getMessage(), ex);
        }
        Collections.sort(list, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        Log.i(TAG, "getAll: size=" + list.size());
        return list;
    }

    public synchronized void add(ExportRecord rec) {
        List<ExportRecord> list = getAll();
        list.add(0, rec);
        save(list);
        Log.i(TAG, "add: " + rec);
    }

    public synchronized void remove(ExportRecord rec) {
        List<ExportRecord> list = getAll();
        list.remove(rec);
        save(list);
        Log.i(TAG, "remove: " + rec);
    }

    /** اگر فایلی حذف شده، فیلدهای مسیر را null می‌کند. */
    public synchronized void validateFiles() {
        List<ExportRecord> list = getAll();
        boolean changed = false;
        for (ExportRecord e : list) {
            if (e.excelPath != null && !new File(e.excelPath).exists()) {
                e.excelPath = null; changed = true;
            }
            if (e.pdfPath != null && !new File(e.pdfPath).exists()) {
                e.pdfPath = null; changed = true;
            }
        }
        if (changed) save(list);
    }

    public List<ExportRecord> search(String query) {
        if (query == null || query.trim().isEmpty()) return getAll();
        String q = query.trim().toLowerCase();
        List<ExportRecord> result = new ArrayList<>();
        for (ExportRecord e : getAll()) {
            if (contains(e.projectNumber, q)
                    || contains(e.applicant, q)
                    || contains(e.requestNumber, q)) {
                result.add(e);
            }
        }
        Log.i(TAG, "search: q='" + q + "' -> " + result.size());
        return result;
    }

    private static boolean contains(String src, String q) {
        return src != null && src.toLowerCase().contains(q);
    }

    private void save(List<ExportRecord> list) {
        try {
            JSONArray arr = new JSONArray();
            for (ExportRecord e : list) {
                JSONObject o = new JSONObject();
                o.put("projectNumber",  e.projectNumber == null ? "" : e.projectNumber);
                o.put("applicant",      e.applicant == null ? "" : e.applicant);
                o.put("requestNumber",  e.requestNumber == null ? "" : e.requestNumber);
                o.put("grandTotal",     e.grandTotal);
                o.put("appliedPercent", e.appliedPercent);
                o.put("excelPath",      e.excelPath == null ? JSONObject.NULL : e.excelPath);
                o.put("pdfPath",        e.pdfPath   == null ? JSONObject.NULL : e.pdfPath);
                o.put("createdAt",      e.createdAt);
                arr.put(o);
            }
            try (FileWriter w = new FileWriter(indexFile, false)) {
                w.write(arr.toString());
            }
        } catch (Exception ex) {
            Log.e(TAG, "save failed: " + ex.getMessage(), ex);
        }
    }
}