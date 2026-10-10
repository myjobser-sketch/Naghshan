package ir.arandy.naghshan.ui.home;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executors;

import ir.arandy.naghshan.data.repository.InvoiceRepository;
import ir.arandy.naghshan.model.Invoice;

public class HomeViewModel extends AndroidViewModel {

    public enum State { IDLE, LOADING, SUCCESS, ERROR }

    private final InvoiceRepository repo;
    private final MutableLiveData<State>   state   = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<Invoice> invoice = new MutableLiveData<>();
    private final MutableLiveData<String>  error   = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application app) {
        super(app);
        repo = new InvoiceRepository(app);
    }

    public LiveData<State>   getState()   { return state; }
    public LiveData<Invoice> getInvoice() { return invoice; }
    public LiveData<String>  getError()   { return error; }

    public void loadPdf(Uri uri) {
        state.setValue(State.LOADING);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Invoice inv = repo.parsePdfFromUri(uri);
                invoice.postValue(inv);
                state.postValue(State.SUCCESS);
            } catch (Exception e) {
                error.postValue(e.getMessage());
                state.postValue(State.ERROR);
            }
        });
    }

    public InvoiceRepository getRepo() { return repo; }
}