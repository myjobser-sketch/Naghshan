package ir.arandy.naghshan.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import ir.arandy.naghshan.ui.history.HistoryActivity;
import ir.arandy.naghshan.R;
import ir.arandy.naghshan.databinding.ActivityHomeBinding;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.ui.invoice.InvoiceActivity;
import ir.arandy.naghshan.ui.invoice.InvoiceFragment;
import ir.arandy.naghshan.ui.profile.ProfileActivity;
import ir.arandy.naghshan.util.MoneyFormatter;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel vm;

    private final ActivityResultLauncher<String[]> openPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    vm.loadPdf(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.cardConvertPdf.setOnClickListener(v ->
                openPdf.launch(new String[]{"application/pdf"}));

        binding.cardInvoiceChanges.setOnClickListener(v -> {
            Invoice inv = vm.getInvoice().getValue();
            if (inv == null) {
                Toast.makeText(this, getString(R.string.toast_pick_pdf_first),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, InvoiceActivity.class)
                    .putExtra(InvoiceFragment.ARG_INVOICE, inv));
        });

        // ⬇⬇⬇ جدید: تاریخچه
        binding.cardHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_projects) {
                startActivity(new Intent(this, HistoryActivity.class));
                return false;
            } else if (id == R.id.nav_tools) {
                Toast.makeText(this, "ابزارها (به‌زودی)", Toast.LENGTH_SHORT).show();
                return false;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false;
            }
            return false;
        });

        vm.getState().observe(this, s -> {
            if (s == null) return;
            switch (s) {
                case LOADING:
                    binding.progress.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progress.setVisibility(View.GONE);
                    Invoice inv = vm.getInvoice().getValue();
                    if (inv != null) {
                        Toast.makeText(this,
                                getString(R.string.toast_extract_done, inv.getItemsCount()),
                                Toast.LENGTH_SHORT).show();
                        binding.txtLastTotal.setText(
                                "آخرین جمع کل: " + MoneyFormatter.formatWithRial(inv.getGrandTotal()));
                    }
                    break;
                case ERROR:
                    binding.progress.setVisibility(View.GONE);
                    Toast.makeText(this,
                            getString(R.string.toast_error, vm.getError().getValue()),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}