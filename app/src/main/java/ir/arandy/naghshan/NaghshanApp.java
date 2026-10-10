package ir.arandy.naghshan;

import android.app.Application;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class NaghshanApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
    }
}