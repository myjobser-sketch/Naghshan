package ir.arandy.naghshan.ui.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import ir.arandy.naghshan.data.history.ExportRecord;
import ir.arandy.naghshan.data.history.HistoryManager;
import ir.arandy.naghshan.databinding.ActivityHistoryBinding;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "NAGHSHAN_HISTORY";

    private ActivityHistoryBinding b;
    private HistoryManager hm;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        hm = new HistoryManager(this);
        hm.validateFiles();

        b.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new HistoryAdapter(this, hm);
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        adapter.submit(hm.getAll());
        Log.i(TAG, "HistoryActivity onCreate: count=" + adapter.getItemCount());

        b.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence cs, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence cs, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable e) {
                String q = e.toString();
                List<ExportRecord> res = hm.search(q);
                adapter.submit(res);
                Log.d(TAG, "search: q='" + q + "' -> " + res.size());
                b.txtEmpty.setVisibility(res.isEmpty() ? android.view.View.VISIBLE
                        : android.view.View.GONE);
            }
        });

        b.txtEmpty.setVisibility(adapter.getItemCount() == 0
                ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // بازخوانی در صورت تغییرات
        hm.validateFiles();
        adapter.submit(hm.getAll());
    }
}