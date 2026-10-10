package ir.arandy.naghshan.ui.home;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import ir.arandy.naghshan.R;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ir.arandy.naghshan.databinding.ActivityHomeBinding;
import ir.arandy.naghshan.ui.invoice.InvoiceFragment;
import ir.arandy.naghshan.ui.invoice.InvoiceActivity;
import ir.arandy.naghshan.model.Invoice;
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
                Toast.makeText(this, "ابتدا یک فایل PDF انتخاب کنید", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, InvoiceActivity.class)
                    .putExtra(InvoiceFragment.ARG_INVOICE, inv));
        });

        vm.getState().observe(this, s -> {
            if (s == null) return;
            switch (s) {
                case LOADING:
                    binding.progress.setVisibility(android.view.View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progress.setVisibility(android.view.View.GONE);
                    Invoice inv = vm.getInvoice().getValue();
                    if (inv != null) {
                        Toast.makeText(this,
                                "استخراج انجام شد. اقلام: " + inv.getItems().size(),
                                Toast.LENGTH_SHORT).show();
                        binding.txtLastTotal.setText(
                                "آخرین جمع کل: " + MoneyFormatter.formatWithRial(inv.getGrandTotal()));
                    }
                    break;
                case ERROR:
                    binding.progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "خطا: " + vm.getError().getValue(),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}