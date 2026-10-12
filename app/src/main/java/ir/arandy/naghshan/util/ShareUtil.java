package ir.arandy.naghshan.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * اشتراک‌گذاری فایل از طریق FileProvider.
 */
public final class ShareUtil {

    private static final String TAG = "NAGHSHAN_SHARE";

    private ShareUtil() { }

    public static void share(Context ctx, File file, String mime) {
        if (file == null || !file.exists()) {
            Log.w(TAG, "share: file missing -> " + (file == null ? "null" : file.getAbsolutePath()));
            Toast.makeText(ctx, "فایل یافت نشد", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(
                    ctx,
                    ctx.getPackageName() + ".fileprovider",
                    file);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(mime);
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, file.getName());
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.i(TAG, "share: uri=" + uri + " mime=" + mime);
            ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری فایل"));
        } catch (Exception e) {
            Log.e(TAG, "share failed: " + e.getMessage(), e);
            Toast.makeText(ctx, "خطا در اشتراک‌گذاری: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}