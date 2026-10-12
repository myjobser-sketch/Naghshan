package ir.arandy.naghshan.data.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * مدیریت پروفایل کاربر (نام، نام خانوادگی، شرکت) روی SharedPreferences.
 */
public class ProfileManager {

    private static final String TAG = "NAGHSHAN_PROFILE";
    private static final String PREF     = "naghshan_profile";
    private static final String K_FIRST  = "first_name";
    private static final String K_LAST   = "last_name";
    private static final String K_COMPANY= "company";

    private final SharedPreferences prefs;

    public ProfileManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void save(String first, String last, String company) {
        Log.i(TAG, "save: first=" + first + " last=" + last + " company=" + company);
        prefs.edit()
                .putString(K_FIRST,   first   == null ? "" : first.trim())
                .putString(K_LAST,    last    == null ? "" : last.trim())
                .putString(K_COMPANY, company == null ? "" : company.trim())
                .apply();
    }

    public String getFirstName() { return prefs.getString(K_FIRST, ""); }
    public String getLastName()  { return prefs.getString(K_LAST,  ""); }
    public String getCompany()   { return prefs.getString(K_COMPANY, ""); }

    public String getFullName() {
        String f = getFirstName().trim();
        String l = getLastName().trim();
        if (f.isEmpty() && l.isEmpty()) return "";
        return (f + " " + l).trim();
    }

    public boolean isComplete() {
        boolean ok = !getFullName().isEmpty() && !getCompany().isEmpty();
        Log.v(TAG, "isComplete: " + ok);
        return ok;
    }
}