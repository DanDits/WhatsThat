package dan.dit.whatsthat.preferences;

import java.util.Locale;

/**
 * Singleton class that needs to be instantiated once by user settings or preferences and used wherever needed.
 * Created by daniel on 24.03.15.
 */
public class Language {
    private Tongue mTongue;
    private static Language INSTANCE = makeInstance(Tongue.getByShortcut(Locale.getDefault().getLanguage()));

    public static Language makeInstance(Tongue tongue) {
        INSTANCE = new Language(tongue);
        return INSTANCE;
    }

    public static Language getInstance() {
        return INSTANCE;
    }

    private Language(Tongue tongue) {
        mTongue = tongue;
        if (tongue == null) {
            throw new IllegalArgumentException("Null tongue.");
        }
    }

    public Tongue getTongue() {
        return mTongue;
    }
}
