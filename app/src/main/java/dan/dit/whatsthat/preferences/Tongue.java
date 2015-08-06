package dan.dit.whatsthat.preferences;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Locale;
import java.util.Random;

import dan.dit.whatsthat.R;

/**
 * A tongue is something a language uses to express words. A tongue consists
 * of an alphabet which is a set of characters. Each character has a given probabilty
 * to appear in a random text of that language.
 * Created by daniel on 24.03.15.
 */
public enum Tongue {
    // source https://de.wikipedia.org/wiki/Buchstabenh%C3%A4ufigkeit#Buchstabenh.C3.A4ufigkeiten_in_deutschsprachigen_Texten
    // de (sums up to 99.668%; some modifications for Umlaute)
    // en (without ï; sums up to 99.999%):
    GERMAN("Deutsch","de", R.drawable.flag_de, "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ", new double[] {5.58,1.96,3.16,4.98,17.29,1.49,3.02,4.98,8.02,0.24,1.32,3.6,2.55,10.53,2.24,0.67,0.02,7.99,7.19,5.99,3.83,0.84,0.178,0.05,0.05,1.21,0.24,0.20,0.35}),

    ENGLISH("English","en", R.drawable.flag_en, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", new double[] {8.167,1.492,2.782,4.253,12.702,2.228,2.015,6.094,6.966,0.153,0.772,4.025,2.406,6.749,7.507,1.929,0.095,5.987,6.327,9.056,2.758,0.978,2.360,0.150,1.974,0.074}),

    ENGLISH_US("English", "en-us", R.drawable.flag_en_us, ENGLISH);


    private String mLocalizedName;
    private String mShortName; // "languagecode-countrycode" or "languagecode". The language codes:  http://www.loc.gov/standards/iso639-2/php/English_list.php
    private String mAlphabet;
    private final double[] mLetterDistribution; // frequency distribution of letters of the alphabet
    private Random mRandom;
    private int mIconResId;
    private Tongue mParentTongue;

    Tongue(String localizedName, String shortName, int iconResId, Tongue parent) {
        mLocalizedName = localizedName;
        mShortName = shortName;
        mIconResId = iconResId;
        if (parent == null) {
            throw new IllegalArgumentException("Null parent given.");
        }
        mParentTongue = parent;
        mAlphabet = parent.mAlphabet;
        mRandom = new Random();
        mLetterDistribution = parent.mLetterDistribution;
    }

    Tongue(String localizedName, String shortName, int iconResId, String alphabet, double[] frequencies) {
        mLocalizedName = localizedName;
        mShortName = shortName;
        mIconResId = iconResId;
        mAlphabet = alphabet;
        mRandom = new Random();
        mLetterDistribution = new double[frequencies.length];
        for (int i = 0; i < alphabet.length(); i++) {
            mLetterDistribution[i] = frequencies[i] / 100.;
            if (i > 0) {
                mLetterDistribution[i] += mLetterDistribution[i - 1];
            }
        }
    }

    @Override
    public String toString() {
        return mLocalizedName + " (" + mShortName + ")";
    }

    /**
     * Returns a random letter of this tongue distributed by the average character
     * distribution of this tongue.
     * @return A random letter of this tongue's alphabet.
     */
    public char getRandomLetter() {
        // inversion method to map uniform distribution to alphabet distribution
        double rolledValue = mRandom.nextDouble();
        for (int index = mLetterDistribution.length - 2; index >= 0; index--) {
            // start at -2 since the last one is (>=) 1.0
            if (mLetterDistribution[index] < rolledValue) {
                return mAlphabet.charAt(index + 1);
            }
        }
        return mAlphabet.charAt(0);
    }

    /**
     * Returns the shortcut of this tongue.
     * @return The tongue's shortcut.
     */
    public String getShortcut() {
        return mShortName;
    }

    /**
     * Returns the localized name describing the tongue in its own tongue.
     * @return The localized name.
     */
    public String getLocalizedName() {
        return mLocalizedName;
    }

    /**
     * Returns the parent tongue of this tongue if any.
     * @return The parent tongue.
     */
    @Nullable
    public Tongue getParentTongue() {
        return mParentTongue;
    }

    /**
     * Searches a tongue with the given shortcut. Not case sensitive.
     * @param shortcut The tongue shortcut.
     * @return The tongue whose shortcut equals the given shortcut
     * or ENGLISH if the given shortcut was empty or not found.
     */
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

    /**
     * Returns the icon resource id that describes this tongue, usually
     * a flag of the country that this language origins from.
     * @return An icon resource id or 0.
     */
    int getIconResId() {
        return mIconResId;
    }

    /**
     * Returns the next tongue, starting from the given tongue. Cycles through all tongues.
     * @param tongue The tongue to start at or null.
     * @return The next tongue or ENGLISH if no tongue given.
     */
    public static Tongue nextTongue(Tongue tongue) {
        if (tongue == null) {
            return ENGLISH;
        }
        int tonguesCount = Tongue.values().length;
        return Tongue.values()[(tongue.ordinal() + 1) % tonguesCount];
    }

    /**
     * Returns the tongue for the given locale if available and supported.
     * @param locale The locale to search.
     * @return The best matching tongue matching the locale's language (and country)
     * or ENGLISH if locale not supported, not valid or no language specified.
     */
    @NonNull
    public static Tongue getTongueOfLocale(Locale locale) {
        if (locale == null) {
            return ENGLISH;
        }
        String language = locale.getLanguage();
        if (TextUtils.isEmpty(language)) {
            return ENGLISH;
        }
        String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            String shortcut = language + "-" + country;
            for (Tongue t : Tongue.values()) {
                if (t.mShortName.equalsIgnoreCase(shortcut)) {
                    return t;
                }
            }
        }
        return getByShortcut(language);
    }
}
