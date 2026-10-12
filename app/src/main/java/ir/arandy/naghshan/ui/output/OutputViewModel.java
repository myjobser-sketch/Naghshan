package ir.arandy.naghshan.ui.output;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import ir.arandy.naghshan.data.history.ExportRecord;
import ir.arandy.naghshan.data.history.HistoryManager;
import ir.arandy.naghshan.data.repository.InvoiceRepository;
import ir.arandy.naghshan.model.Invoice;

public class OutputViewModel extends AndroidViewModel {

    private static final String TAG = "NAGHSHAN_OUTPUT";

    public enum State { IDLE, WORKING, DONE, ERROR }

    private final InvoiceRepository repo;
    private final MutableLiveData<State>  state   = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Invoice> invoice = new MutableLiveData<>();
    private final MutableLiveData<File>   lastExportFile = new MutableLiveData<>();
    private final MutableLiveData<String> lastExportMime = new MutableLiveData<>();

    public OutputViewModel(@NonNull Application app) {
        super(app);
        repo = new InvoiceRepository(app);
    }

    public LiveData<State>   getState()   { return state; }
    public LiveData<String>  getMessage() { return message; }
    public LiveData<Invoice> getInvoice() { return invoice; }
    public LiveData<File>   getLastExportFile() { return lastExportFile; }
    public LiveData<String> getLastExportMime() { return lastExportMime; }

    public void setInvoice(Invoice inv) { invoice.setValue(inv); }

    public void exportExcel() {
        Invoice inv = invoice.getValue();
        if (inv == null) return;
        state.setValue(State.WORKING);
        new Thread(() -> {
            try {
                File f = repo.exportExcel(inv);
                Log.i(TAG, "excel exported: " + f.getAbsolutePath());

                recordHistory(inv, f.getAbsolutePath(), null);

                lastExportFile.postValue(f);
                lastExportMime.postValue(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

                message.postValue("Excel ساخته شد: " + f.getAbsolutePath());
                state.postValue(State.DONE);
            } catch (Exception e) {
                Log.e(TAG, "excel export failed: " + e.getMessage(), e);
                message.postValue("خطا: " + e.getMessage());
                state.postValue(State.ERROR);
            }
        }).start();
    }

    public void exportPdf() {
        Invoice inv = invoice.getValue();
        if (inv == null) return;
        state.setValue(State.WORKING);
        new Thread(() -> {
            try {
                File font = copyFontFromAssets();
                File f = repo.exportPdf(inv, font);
                Log.i(TAG, "pdf exported: " + f.getAbsolutePath());

                recordHistory(inv, null, f.getAbsolutePath());

                lastExportFile.postValue(f);
                lastExportMime.postValue("application/pdf");

                message.postValue("PDF ساخته شد: " + f.getAbsolutePath());
                state.postValue(State.DONE);
            } catch (Exception e) {
                Log.e(TAG, "pdf export failed: " + e.getMessage(), e);
                message.postValue("خطا: " + e.getMessage());
                state.postValue(State.ERROR);
            }
        }).start();
    }

    private File copyFontFromAssets() throws Exception {
        File out = new File(getApplication().getCacheDir(), "Vazirmatn-Regular.ttf");
        if (out.exists() && out.length() > 0) return out;
        try (InputStream in = getApplication().getAssets().open("fonts/Vazirmatn-Regular.ttf");
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        }
        return out;
    }

    /**
     * ثبت یک رکورد تاریخچه برای خروجی ساخته‌شده.
     * اگر رکوردی با همان شماره طرح در بازه‌ی اخیر وجود داشته باشد،
     * مسیر فایل جدید به آن اضافه می‌شود (به‌جای ساخت رکورد تکراری).
     */
    private void recordHistory(Invoice inv, String excelPath, String pdfPath) {
        try {
            HistoryManager hm = new HistoryManager(getApplication());
            ExportRecord rec = new ExportRecord();
            rec.projectNumber  = inv.getProjectInfo().getProjectNumber();
            rec.applicant      = inv.getProjectInfo().getApplicantName();
            rec.requestNumber  = inv.getProjectInfo().getRequestNumber();
            rec.grandTotal     = inv.getGrandTotal();
            rec.appliedPercent = inv.getAppliedPercent();
            rec.excelPath      = excelPath;
            rec.pdfPath        = pdfPath;
            rec.createdAt      = System.currentTimeMillis();
            hm.add(rec);
            Log.i(TAG, "history recorded: " + rec);
        } catch (Exception e) {
            Log.e(TAG, "recordHistory failed: " + e.getMessage(), e);
        }
    }
}