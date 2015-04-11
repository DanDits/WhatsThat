package dan.dit.whatsthat.testsubject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 11.04.15.
 */
public class TestSubject {
    private static final String TEST_SUBJECT_PREFERENCES_FILE = "dan.dit.whatsthat.testsubject.preferences";
    private static final String TEST_SUBJECT_PREF_LEVEL_KEY = "testsubject_key_level";
    private static final String TEST_SUBJECT_PREF_MAIN_TEXT_INDEX_KEY = "testsubject_key_maintext_index";
    private static SharedPreferences PREFERENCES;

    private static TestSubject INSTANCE;
    public static final int LEVEL_KID_STUPID= 0;
    public static final int LEVEL_KID_NORMAL = 1;


    private int mLevel;
    private int mNameResId;
    private int mIntelligenceResId;
    private int mImageResId;
    private int mTextMainIndex;
    private String[] mIntroTextMain;
    private int mTextNutsIndex;
    private String[] mIntroTextNuts;

    private TestSubject() {
    }

    public static TestSubject getInstance() {
        return INSTANCE;
    }

    public static TestSubject loadInstance(Context context) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        PREFERENCES = context.getSharedPreferences(TEST_SUBJECT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        int level = PREFERENCES.getInt(TEST_SUBJECT_PREF_LEVEL_KEY, LEVEL_KID_STUPID);
        int mainIndex = PREFERENCES.getInt(TEST_SUBJECT_PREF_MAIN_TEXT_INDEX_KEY, 0);
        INSTANCE = new TestSubject();
        INSTANCE.setLevel(context.getResources(), level, mainIndex);
        return INSTANCE;
    }

    private void setLevel(Resources res, int level, int mainTextIndex) {
        mLevel = level;
        mTextMainIndex = mainTextIndex;
        mTextNutsIndex = 0;
        PREFERENCES.edit().putInt(TEST_SUBJECT_PREF_LEVEL_KEY, mLevel)
                .putInt(TEST_SUBJECT_PREF_MAIN_TEXT_INDEX_KEY, mTextMainIndex).apply();
        switch (mLevel) {
            case LEVEL_KID_STUPID:
                mNameResId = R.string.test_subject_0_name;
                mIntelligenceResId = R.string.test_subject_0_int;
                mImageResId = R.drawable.kid;
                mIntroTextMain = res.getStringArray(R.array.test_subject_0_intro_main);
                mIntroTextNuts = res.getStringArray(R.array.test_subject_0_intro_nuts);
                break;
            case LEVEL_KID_NORMAL:
                mNameResId = R.string.test_subject_1_name;
                mIntelligenceResId = R.string.test_subject_1_int;
                mImageResId = R.drawable.kid;
                mIntroTextMain = res.getStringArray(R.array.test_subject_1_intro_main);
                mIntroTextNuts = res.getStringArray(R.array.test_subject_1_intro_nuts);
                break;
            default:
                throw new IllegalArgumentException("Not a valid level for testsubject: " + level);
        }
    }

    public boolean hasNextMainText() {
        return mIntroTextMain.length > mTextMainIndex;
    }

    public String nextMainText() {
        if (!hasNextMainText()) {
            return null;
        }
        String text = mIntroTextMain[mTextMainIndex];
        mTextMainIndex++;
        PREFERENCES.edit().putInt(TEST_SUBJECT_PREF_MAIN_TEXT_INDEX_KEY, mTextMainIndex).apply();
        return text;
    }

    public String nextNutsText() {
        if (mIntroTextNuts != null && mIntroTextNuts.length > 0) {
            String text = mIntroTextNuts[mTextNutsIndex];
            mTextNutsIndex++;
            mTextNutsIndex %= mIntroTextNuts.length;
            return text;
        } else {
            return "";
        }
    }

    public int getIntelligenceResId() {
        return mIntelligenceResId;
    }

    public int getImageResId() {
        return mImageResId;
    }

    public int getNameResId() {
        return mNameResId;
    }
}
