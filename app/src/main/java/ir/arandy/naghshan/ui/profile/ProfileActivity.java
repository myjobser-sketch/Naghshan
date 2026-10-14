package ir.arandy.naghshan.ui.profile;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "NAGHSHAN_PROFILE";

    /** پالت رنگ از پیش تعریف‌شده. */
    private static final String[] COLOR_HEX = {
            "#F1F5F9", // خاکستری روشن
            "#FEF3C7", // کرم
            "#DBEAFE", // آبی روشن
            "#DCFCE7", // سبز روشن
            "#FCE7F3", // صورتی روشن
            "#FFFFFF"  // بدون رنگ
    };

    private ActivityProfileBinding b;
    private ProfileManager pm;
    private String selectedColor = COLOR_HEX[0];

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        pm = new ProfileManager(this);
        Log.i(TAG, "onCreate: complete=" + pm.isComplete());

        b.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // ----- بارگذاری داده‌های قبلی -----
        b.editFirstName.setText(pm.getFirstName());
        b.editLastName.setText(pm.getLastName());
        b.editCompany.setText(pm.getCompany());
        b.editInvoiceNote.setText(pm.getInvoiceNote());

        selectedColor = pm.getThemeColor();
        setupColorPalette();
        updateColorPreview();

        // ----- ذخیره -----
        b.btnSave.setOnClickListener(v -> {
            String first = text(b.editFirstName.getText());
            String last  = text(b.editLastName.getText());
            String comp  = text(b.editCompany.getText());
            String note  = text(b.editInvoiceNote.getText());

            if (first.isEmpty() || last.isEmpty() || comp.isEmpty()) {
                Toast.makeText(this, "لطفاً نام، نام خانوادگی و شرکت را پر کنید",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (note.length() > 300) {
                note = note.substring(0, 300);
            }

            pm.save(first, last, comp);
            pm.setInvoiceNote(note);
            pm.setThemeColor(selectedColor);

            Log.i(TAG, "saved: color=" + selectedColor + " noteLen=" + note.length());
            Toast.makeText(this, "پروفایل ذخیره شد", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // ----------------------------------------------------------------

    /** اتصال ۶ RadioButton رنگ به listener. */
    private void setupColorPalette() {
        int[] ids = {
                b.color0.getId(), b.color1.getId(), b.color2.getId(),
                b.color3.getId(), b.color4.getId(), b.color5.getId()
        };
        for (int i = 0; i < ids.length; i++) {
            final String hex = COLOR_HEX[i];
            RadioButton rb = findViewById(ids[i]);
            // رنگ پس‌زمینه‌ی دکمه
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(hex));
            gd.setStroke(2, Color.parseColor("#94A3B8"));
            rb.setButtonDrawable(null);
            rb.setBackground(gd);

            if (hex.equalsIgnoreCase(selectedColor)) rb.setChecked(true);

            rb.setOnClickListener(v -> {
                selectedColor = hex;
                Log.d(TAG, "color selected: " + hex);
                updateColorPreview();
            });
        }
    }

    /** به‌روزرسانی پیش‌نمایش رنگی سربرگ. */
    private void updateColorPreview() {
        try {
            b.previewBanner.setBackgroundColor(Color.parseColor(selectedColor));
            b.previewBannerText.setText(
                    pm.getCompany().isEmpty() ? "نام شرکت" : pm.getCompany());
        } catch (Exception e) {
            Log.e(TAG, "color preview failed: " + e.getMessage());
        }
    }

    private static String text(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}