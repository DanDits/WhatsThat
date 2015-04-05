package dan.dit.whatsthat.preferences;

import android.text.TextUtils;

/**
 * Created by daniel on 24.03.15.
 */
public enum Tongue {
    GERMAN("Deutsch","de"), ENGLISH("English","en");

    private String mLocalizedName;
    private String mShortName; // by ISO 639-1

    private Tongue(String localizedName, String shortName) {
        mLocalizedName = localizedName;
        mShortName = shortName;
    }

    public String getShortcut() {
        return mShortName;
    }

    public static Tongue getByShortcut(String shortcut) {
        if (TextUtils.isEmpty(shortcut)) {
            return ENGLISH; // default
        }
        for (Tongue t : Tongue.values()) {
            if (t.mShortName.equalsIgnoreCase(shortcut)) {
                return t;
            }
        }
        return ENGLISH;
    }
}
