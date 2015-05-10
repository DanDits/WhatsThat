package dan.dit.whatsthat.preferences;

import android.support.annotation.NonNull;

import java.util.Locale;

/**
 * Singleton class that needs to be instantiated once by user settings or preferences and used wherever needed.
 * Created by daniel on 24.03.15.
 */
public class Language {
    private Tongue mTongue;
    private static Language INSTANCE = makeInstance(Tongue.getByShortcut(Locale.getDefault().getLanguage()));

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
}
