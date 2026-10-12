package ir.arandy.naghshan.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "NAGHSHAN_PROFILE";

    private ActivityProfileBinding b;
    private ProfileManager pm;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        pm = new ProfileManager(this);
        Log.i(TAG, "ProfileActivity onCreate: complete=" + pm.isComplete());

        b.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        b.editFirstName.setText(pm.getFirstName());
        b.editLastName.setText(pm.getLastName());
        b.editCompany.setText(pm.getCompany());

        b.btnSave.setOnClickListener(v -> {
            String first = b.editFirstName.getText() == null
                    ? "" : b.editFirstName.getText().toString();
            String last  = b.editLastName.getText()  == null
                    ? "" : b.editLastName.getText().toString();
            String comp  = b.editCompany.getText()   == null
                    ? "" : b.editCompany.getText().toString();

            if (first.trim().isEmpty() || last.trim().isEmpty() || comp.trim().isEmpty()) {
                Toast.makeText(this,
                        "لطفاً همه فیلدها را پر کنید",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            pm.save(first, last, comp);
            Log.i(TAG, "ProfileActivity saved");
            Toast.makeText(this, "پروفایل ذخیره شد", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}