package ir.arandy.naghshan.ui.output;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import ir.arandy.naghshan.data.repository.InvoiceRepository;
import ir.arandy.naghshan.model.Invoice;

public class OutputViewModel extends AndroidViewModel {

    public enum State { IDLE, WORKING, DONE, ERROR }

    private final InvoiceRepository repo;
    private final MutableLiveData<State>  state   = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Invoice> invoice = new MutableLiveData<>();

    public OutputViewModel(@NonNull Application app) {
        super(app);
        repo = new InvoiceRepository(app);
    }

    public LiveData<State>   getState()   { return state; }
    public LiveData<String>  getMessage() { return message; }
    public LiveData<Invoice> getInvoice() { return invoice; }

    public void setInvoice(Invoice inv) { invoice.setValue(inv); }

    public void exportExcel() {
        Invoice inv = invoice.getValue();
        if (inv == null) return;
        state.setValue(State.WORKING);
        new Thread(() -> {
            try {
                File f = repo.exportExcel(inv);
                message.postValue("Excel ساخته شد: " + f.getAbsolutePath());
                state.postValue(State.DONE);
            } catch (Exception e) {
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
                message.postValue("PDF ساخته شد: " + f.getAbsolutePath());
                state.postValue(State.DONE);
            } catch (Exception e) {
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
}