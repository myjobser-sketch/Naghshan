package ir.arandy.naghshan.ui.invoice;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import ir.arandy.naghshan.R;

public class InvoiceActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_invoice);
        if (s == null) {
            Fragment f = new InvoiceFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f).commit();
        }
    }
}