package dan.dit.whatsthat.preferences;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Locale;

import dan.dit.whatsthat.R;

/**
 * Singleton class that needs to be instantiated once by user settings or preferences and used wherever needed.
 * Created by daniel on 24.03.15.
 */
public class Language {
    private static final String PREFERRED_TONGUE_KEY = "tongue_preferred";
    private Tongue mTongue;
    private static Language INSTANCE = makeInstance(Tongue.getTongueOfLocale(Locale.getDefault()));

    /**
     * Initializes the singleton to use the given tongue.
     * @param tongue The tongue. Must not be null.
     * @return The singleton.
     */
    public static Language makeInstance(@NonNull Tongue tongue) {
        INSTANCE = new Language(tongue);
        return INSTANCE;
    }

    /**
     * Returns the singleton instance of the language.
     * @return The language singleton.
     */
    public static Language getInstance() {
        return INSTANCE;
    }

    private Language(Tongue tongue) {
        mTongue = tongue;
        if (tongue == null) {
            throw new IllegalArgumentException("Null tongue.");
        }
    }

    /**
     * Returns the language's tongue.
     * @return The tongue.
     */
    public Tongue getTongue() {
        return mTongue;
    }

    /**
     * Returns a valid icon that represents the tongue of
     * this language. Usually a flag of the country the tongue
     * belongs too. If not specified by the tongue an en icon is used.
     * @return An icon representing this language's tongue.
     */
    public int getTongueIcon() {
        int iconId = mTongue.getIconResId();
        if (iconId == 0) {
            return R.drawable.flag_en;
        }
        return iconId;
    }

    public void saveAsPreference(SharedPreferences prefs) {
        prefs.edit().putString(PREFERRED_TONGUE_KEY, mTongue.getShortcut()).apply();
    }

    public static Tongue getTonguePreference(SharedPreferences prefs) {
        String shortCut = prefs.getString(PREFERRED_TONGUE_KEY, null);
        if (TextUtils.isEmpty(shortCut)) {
            return null;
        }
        return Tongue.getByShortcut(shortCut);
    }
}
