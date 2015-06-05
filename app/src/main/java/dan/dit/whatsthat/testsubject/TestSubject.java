package dan.dit.whatsthat.testsubject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import com.github.johnpersano.supertoasts.SuperToast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.AchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 11.04.15.
 */
public class TestSubject implements Runnable {
    private static final String TEST_SUBJECT_PREFERENCES_FILE = "dan.dit.whatsthat.testsubject.preferences";
    private static final String TEST_SUBJECT_PREF_LEVEL_KEY = "testsubject_key_level";
    private static final String TEST_SUBJECT_PREF_SPENT_SCORE_KEY = "testsubject_key_spent_score";
    private static final String TEST_SUBJECT_PREF_FINISHED_MAIN_TEXTS = "testsubject_key_finished_main_texts";

    private static final TestSubject INSTANCE = new TestSubject();
    public static final int LEVEL_0_KID_STUPID= 0;
    public static final int LEVEL_1_KID_NORMAL = 1;
    public static final String EMAIL_ON_ERROR = "whatsthat.feedback@gmail.com";


    private boolean mInitialized;
    private int mSpentScore;
    private int mLevel;


    private SharedPreferences mPreferences;
    private int mNameResId;
    private int mIntelligenceResId;
    private int mImageResId;
    private int mTextMainIndex;
    private int mTextNutsIndex;
    private String[] mIntroTextMain;
    private boolean mFinishedMainTexts;
    private String[] mIntroTextNuts;
    private String[] mRiddleSolvedCandy;
    private Random mRand = new Random();
    private List<TestSubjectRiddleType> mTypes;
    private Handler mHandler;
    private List<TestSubjectToast> mToasts;
    private Context mApplicationContext;
    private AchievementHolder mAchievementHolder;

    private TestSubject() {
    }

    public static TestSubject initialize(Context context) {
        INSTANCE.mApplicationContext = context.getApplicationContext();
        AchievementManager.initialize(INSTANCE.mApplicationContext);
        INSTANCE.mPreferences = context.getSharedPreferences(TEST_SUBJECT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        INSTANCE.init(context.getResources());
        INSTANCE.mInitialized = true;
        INSTANCE.initTypes();
        INSTANCE.mAchievementHolder = AchievementHolder.getInstance(AchievementManager.getInstance(), INSTANCE.mLevel, INSTANCE.mTypes);
        return INSTANCE;
    }

    public void initToasts() {
        mHandler = new Handler();
        mToasts = new LinkedList<>();
    }

    private void initTypes() {
        //TODO implement save and load
        mTypes = new ArrayList<>();
        mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.CIRCLE_INSTANCE));
        mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.SNOW_INSTANCE));
        mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.TRIANGLE_INSTANCE));
        mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.DICE_INSTANCE));
        mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.JUMPER_INSTANCE));
    }

    public static boolean isInitialized() {
        return INSTANCE.mInitialized;
    }

    private void init(Resources res) {
        mLevel = mPreferences.getInt(TEST_SUBJECT_PREF_LEVEL_KEY, LEVEL_0_KID_STUPID);
        mFinishedMainTexts = mPreferences.getBoolean(TEST_SUBJECT_PREF_FINISHED_MAIN_TEXTS, false);
        applyLevel(res);
        mSpentScore = mPreferences.getInt(TEST_SUBJECT_PREF_SPENT_SCORE_KEY, 0);
    }

    public static TestSubject getInstance() {
        if (!INSTANCE.mInitialized) {
            throw new IllegalStateException("Subject not initialized!");
        }
        return INSTANCE;
    }

    public void postToast(TestSubjectToast toast, long delay) {
        if (mHandler == null || toast == null) {
            if (mHandler == null) {
                Log.e("HomeStuff", "Trying to post toast. No handler initialized for test subject.");
            }
            return;
        }
        mToasts.add(toast);
        if (delay > 0) {
            mHandler.postDelayed(this, delay);
        } else {
            mHandler.post(this);
        }
    }

    @Override
    public void run() {
        if (mToasts.isEmpty()) {
            return;
        }
        TestSubjectToast toast = mToasts.remove(0);
        SuperToast superToast = new SuperToast(mApplicationContext);
        if (toast.mTextResId != 0) {
            superToast.setText(mApplicationContext.getResources().getText(toast.mTextResId));
        }
        if (toast.mIconResId != 0) {
            superToast.setIcon(toast.mIconResId, toast.mIconPosition == null ? SuperToast.IconPosition.LEFT : toast.mIconPosition);
        }
        superToast.setGravity(toast.mGravity, toast.mOffsetX, toast.mOffsetY);
        superToast.setDuration(toast.mDuration);
        superToast.show();
    }

    private void applyLevel(Resources res) {
        mTextMainIndex = 0;
        mTextNutsIndex = 0;
        switch (mLevel) {
            case LEVEL_0_KID_STUPID:
                mNameResId = R.string.test_subject_0_name;
                mIntelligenceResId = R.string.test_subject_0_int;
                mImageResId = R.drawable.kid0;
                mIntroTextMain = res.getStringArray(R.array.test_subject_0_intro_main);
                mIntroTextNuts = res.getStringArray(R.array.test_subject_0_intro_nuts);
                mRiddleSolvedCandy = res.getStringArray(R.array.test_subject_0_riddle_solved_candy);
                break;
            case LEVEL_1_KID_NORMAL:
                mNameResId = R.string.test_subject_1_name;
                mIntelligenceResId = R.string.test_subject_1_int;
                mImageResId = R.drawable.kid;
                mIntroTextMain = res.getStringArray(R.array.test_subject_1_intro_main);
                mIntroTextNuts = res.getStringArray(R.array.test_subject_1_intro_nuts);
                mRiddleSolvedCandy = res.getStringArray(R.array.test_subject_1_riddle_solved_candy);
                break;
            default:
                throw new IllegalArgumentException("Not a valid level " + mLevel);
        }
    }

    public String nextText() {
        if (!hasNextMainText() || mFinishedMainTexts) {
            return nextNutsText();
        } else {
            String text = nextMainText();
            if (!hasNextMainText()) {
                mPreferences.edit().putBoolean(TEST_SUBJECT_PREF_FINISHED_MAIN_TEXTS, true).apply();
            }
            return text;
        }
    }

    public boolean finishedMainTexts() {
        return mFinishedMainTexts;
    }

    private boolean hasNextMainText() {
        return mIntroTextMain.length > mTextMainIndex;
    }

    private String nextMainText() {
        if (!hasNextMainText() || mIntroTextMain.length == 0) {
            return "";
        }
        String text = mIntroTextMain[mTextMainIndex];
        mTextMainIndex++;
        return text;
    }

    private String nextNutsText() {
        if (mIntroTextNuts != null && mIntroTextNuts.length > 0) {
            String text = mIntroTextNuts[mTextNutsIndex];
            mTextNutsIndex++;
            mTextNutsIndex %= mIntroTextNuts.length;
            return text;
        } else {
            return "";
        }
    }

    public String nextRiddleSolvedCandy() {
        if (mRiddleSolvedCandy.length > 0) {
            return mRiddleSolvedCandy[mRand.nextInt(mRiddleSolvedCandy.length)];
        }
        return "";
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

    public int getSpentScore() {
        return mSpentScore;
    }

    public List<TestSubjectRiddleType> getAvailableTypes() {
        return mTypes;
    }

    public boolean canSkip() {
        return true; // TODO make it a listener of unsolved riddles changed, only allow if less than some number defined by subject level and stuff
    }

    public PracticalRiddleType findNextRiddleType() {
        List<TestSubjectRiddleType> types = new ArrayList<>(mTypes);
        Iterator<TestSubjectRiddleType> it = types.iterator();
        while (it.hasNext()) {
            TestSubjectRiddleType next = it.next();
            if (!next.isSelected()) {
                it.remove();
            }
        }
        if (types.size() > 0) {
            return types.get(mRand.nextInt(types.size())).getType();
        } else {
            return mTypes.get(mRand.nextInt(mTypes.size())).getType();
        }
    }

}
