package ir.arandy.naghshan.ui.invoice;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ir.arandy.naghshan.data.repository.InvoiceRepository;
import ir.arandy.naghshan.model.Invoice;

public class InvoiceViewModel extends AndroidViewModel {

    private final InvoiceRepository repo;
    private final MutableLiveData<Invoice> invoice = new MutableLiveData<>();

    public InvoiceViewModel(@NonNull Application app) {
        super(app);
        repo = new InvoiceRepository(app);
    }

    public LiveData<Invoice> getInvoice() { return invoice; }

    public void setInvoice(Invoice inv) { invoice.setValue(inv); }

    public Invoice applyAdjustment(double percent, boolean toMat, boolean toLab) {
        Invoice base = invoice.getValue();
        if (base == null) return null;
        Invoice updated = repo.applyAdjustment(base, percent, toMat, toLab);
        invoice.setValue(updated);
        return updated;
    }

    public InvoiceRepository getRepo() { return repo; }
    public Invoice getCurrent()        { return invoice.getValue(); }
}