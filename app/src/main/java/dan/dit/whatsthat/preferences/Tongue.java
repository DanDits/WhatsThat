package dan.dit.whatsthat.preferences;

import android.text.TextUtils;

import java.util.Random;

/**
 * Created by daniel on 24.03.15.
 */
public enum Tongue {
    // source https://de.wikipedia.org/wiki/Buchstabenh%C3%A4ufigkeit#Buchstabenh.C3.A4ufigkeiten_in_deutschsprachigen_Texten
    // de (sums up to 99.668%; some modifications for Umlaute)
    // en (without ï; sums up to 99.999%):
    GERMAN("Deutsch","de","ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ", new double[] {5.58,1.96,3.16,4.98,17.29,1.49,3.02,4.98,8.02,0.24,1.32,3.6,2.55,10.53,2.24,0.67,0.02,7.99,7.19,5.99,3.83,0.84,0.178,0.05,0.05,1.21,0.24,0.20,0.35}),

    ENGLISH("English","en","ABCDEFGHIJKLMNOPQRSTUVWXYZ", new double[] {8.167,1.492,2.782,4.253,12.702,2.228,2.015,6.094,6.966,0.153,0.772,4.025,2.406,6.749,7.507,1.929,0.095,5.987,6.327,9.056,2.758,0.978,2.360,0.150,1.974,0.074});

    private String mLocalizedName;
    private String mShortName; // by ISO 639-1
    private String mAlphabet;
    private double[] mLetterDistribution; // frequency distribution of letters of the alphabet
    private Random mRandom;

    private Tongue(String localizedName, String shortName, String alphabet, double[] frequencies) {
        mLocalizedName = localizedName;
        mShortName = shortName;
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
