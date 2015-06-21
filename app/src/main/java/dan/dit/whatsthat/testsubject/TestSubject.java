package dan.dit.whatsthat.testsubject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;

import com.github.johnpersano.supertoasts.SuperToast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.dependencies.Dependable;
import dan.dit.whatsthat.testsubject.dependencies.Dependency;
import dan.dit.whatsthat.testsubject.dependencies.MinValueDependency;
import dan.dit.whatsthat.testsubject.wallet.WalletEntry;
import dan.dit.whatsthat.util.DelayedQueueProcessor;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 11.04.15.
 */
public class TestSubject {
    private static final TestSubject INSTANCE = new TestSubject();
    public static final int LEVEL_NONE = -1;
    public static final int LEVEL_0_KID_STUPID = 0;
    public static final int LEVEL_1_KID_NORMAL = 1;
    public static final String EMAIL_ON_ERROR = "whatsthat.contact@gmail.com";
    public static final String EMAIL_FEEDBACK = "whatsthat.feedback@gmail.com";
    private static final String TEST_SUBJECT_PREFERENCES_FILE = "dan.dit.whatsthat.testsubject_preferences";
    private static final String TEST_SUBJECT_PREF_FINISHED_MAIN_TEXTS = "key_finished_main_texts";
    private static final String TEST_SUBJECT_PREF_RIDDLE_TYPES = "key_testsubject_riddletypes";
    private static final int DEFAULT_SKIPABLE_GAMES = 15;


    private boolean mInitialized;


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
    private List<TestSubjectRiddleType> mTypes = new ArrayList<>();

    private Context mApplicationContext;
    private TestSubjectAchievementHolder mAchievementHolder;
    private Purse mPurse;
    private DelayedQueueProcessor<TestSubjectToast> mGeneralToastProcessor;
    private DelayedQueueProcessor<Achievement> mAchievementToastProcessor;

    private TestSubject() {
    }

    public static TestSubject initialize(Context context) {
        INSTANCE.mApplicationContext = context.getApplicationContext();
        AchievementManager.initialize(INSTANCE.mApplicationContext);
        INSTANCE.initPreferences();
        INSTANCE.initLevel();
        INSTANCE.mInitialized = true;
        INSTANCE.mAchievementHolder = new TestSubjectAchievementHolder(AchievementManager.getInstance());
        INSTANCE.mAchievementHolder.addDependencies();
        INSTANCE.mAchievementHolder.initAchievements();
        return INSTANCE;
    }

    public Dependable getLevelDependency() {
        return mPurse.mRewardWallet.assureEntry(Purse.RW_KEY_TESTSUBJECT_LEVEL, LEVEL_0_KID_STUPID);
    }

    public void initToasts() {
        mGeneralToastProcessor = new DelayedQueueProcessor<>(new DelayedQueueProcessor.Callback<TestSubjectToast>() {
            @Override
            public long process(TestSubjectToast toProcess) {
                showToast(toProcess);
                return toProcess.mDuration;
            }
        });
        mAchievementToastProcessor = new DelayedQueueProcessor<>(new DelayedQueueProcessor.Callback<Achievement>() {
            @Override
            public long process(Achievement toProcess) {
                TestSubjectToast achievementToast = makeAchievementToast(toProcess);
                Log.d("Achievement", "Showing achievement toast: " + achievementToast);
                showToast(achievementToast);
                return achievementToast == null ? 0L : achievementToast.mDuration;
            }
        });
    }

    private void showToast(TestSubjectToast toast) {
        if (toast != null) {
            SuperToast superToast = toast.makeSuperToast(mApplicationContext);
            if (superToast != null) {
                superToast.show();
            }
        }
    }

    private TestSubjectToast makeAchievementToast(Achievement achievement) {
        if (mApplicationContext == null) {
            return null;
        }
        TestSubjectToast toast = new TestSubjectToast(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 5, achievement.getIconResId(), 0, SuperToast.Duration.LONG);
        toast.mIconPosition = SuperToast.IconPosition.LEFT;
        toast.mAnimations = SuperToast.Animations.FLYIN;
        toast.mBackground = R.drawable.achieved_background;
        toast.mTextColor = Color.BLACK;
        Resources res = mApplicationContext.getResources();
        toast.mText = res.getString(R.string.achievement_achieved) + " " + achievement.getName(res);
        return toast;
    }

    public static boolean isInitialized() {
        return INSTANCE.mInitialized;
    }

    private void initPreferences() {
        mPreferences = mApplicationContext.getSharedPreferences(TEST_SUBJECT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mPurse = new Purse(mApplicationContext);
        mFinishedMainTexts = mPreferences.getBoolean(TEST_SUBJECT_PREF_FINISHED_MAIN_TEXTS, false);

        // TestSubjectRiddleTypes
        String typesDataRaw = mPreferences.getString(TEST_SUBJECT_PREF_RIDDLE_TYPES, null);
        Compacter typesData = TextUtils.isEmpty(typesDataRaw) ? null : new Compacter(typesDataRaw);
        if (typesData != null) {
            for (String typeRaw : typesData) {
                try {
                    mTypes.add(new TestSubjectRiddleType(new Compacter(typeRaw)));
                } catch (CompactedDataCorruptException e) {
                    Log.e("HomeStuff", "Could not load testsubject riddle type: " + e);
                }
            }
        }

    }

    public void saveTypes() {
        if (mTypes != null) {
            Compacter typesData = new Compacter(mTypes.size());
            for (TestSubjectRiddleType type : mTypes) {
                typesData.appendData(type.compact());
            }
            mPreferences.edit().putString(TEST_SUBJECT_PREF_RIDDLE_TYPES, typesData.compact()).apply();
        }
    }

    private void initLevel() {
        WalletEntry levelEntry = mPurse.mRewardWallet.assureEntry(Purse.RW_KEY_TESTSUBJECT_LEVEL, LEVEL_NONE);
        int oldLevel = levelEntry.getValue();
        if (oldLevel == LEVEL_NONE) {
            mPurse.mRewardWallet.editEntry(Purse.RW_KEY_TESTSUBJECT_LEVEL).set(LEVEL_0_KID_STUPID);
        }
        int newLevel = levelEntry.getValue();
        if (newLevel > oldLevel) {
            onLevelUp(newLevel);
            saveTypes();
        }

        applyLevel(mApplicationContext.getResources(), newLevel);
    }

    public static TestSubject getInstance() {
        if (!INSTANCE.mInitialized) {
            throw new IllegalStateException("Subject not initialized!");
        }
        return INSTANCE;
    }

    public void postToast(TestSubjectToast toast, long delay) {
        if (mGeneralToastProcessor == null || toast == null) {
            if (mGeneralToastProcessor == null) {
                Log.e("HomeStuff", "Trying to post toast. No handler initialized for test subject.");
            }
            return;
        }
        mGeneralToastProcessor.append(toast, delay);
        mGeneralToastProcessor.start();
    }

    public void postAchievementAchieved(Achievement achievement) {
        mAchievementToastProcessor.append(achievement, 200L);
        mAchievementToastProcessor.start();
    }

    private void onLevelUp(int newLevel) {
        if (newLevel == LEVEL_0_KID_STUPID) {
            mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.CIRCLE_INSTANCE));
            mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.SNOW_INSTANCE));
            mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.TRIANGLE_INSTANCE));
            mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.DICE_INSTANCE));
            mTypes.add(new TestSubjectRiddleType(PracticalRiddleType.JUMPER_INSTANCE));
        }
    }

    private void applyLevel(Resources res, int level) {
        mTextMainIndex = 0;
        mTextNutsIndex = 0;
        switch (level) {
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
                throw new IllegalArgumentException("Not a valid level " + level);
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
        return mPurse.mScoreWallet.assureEntry(Purse.SW_KEY_SPENT_SCORE).getValue();
    }

    public List<TestSubjectRiddleType> getAvailableTypes() {
        return mTypes;
    }

    public boolean canSkip() {
        return mPurse.mRewardWallet.assureEntry(Purse.RW_KEY_SKIPABLE_GAMES, DEFAULT_SKIPABLE_GAMES).getValue()
                > RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddleCount();
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
            if (mTypes.size() == 0) {
                Log.e("HomeStuff", "No types initialized when trying to find a riddle type!");
                return PracticalRiddleType.SNOW_INSTANCE; // just a dummy, so there is a riddle
            }
            return mTypes.get(mRand.nextInt(mTypes.size())).getType();
        }
    }

    public void addSolvedRiddleScore(int score) {
        mPurse.mScoreWallet.editEntry(Purse.SW_KEY_SOLVED_RIDDLE_SCORE).add(score);
        if (BuildConfig.DEBUG) {
            WalletEntry entry = mPurse.mScoreWallet.assureEntry(Purse.SW_KEY_SOLVED_RIDDLE_SCORE);
            Log.d("HomeStuff", "Adding " + score + " to wallet, new riddle score: " + entry.getValue() + " (loaded " + RiddleInitializer.INSTANCE.getRiddleManager().getLoadedScore() + ")");
        }
    }

    public void addAchievementScore(int score) {
        mPurse.mScoreWallet.editEntry(Purse.SW_KEY_ACHIEVEMENT_SCORE).add(score);
        Log.d("HomeStuff", "Adding " + score + " to wallet, new achievement score: " + mPurse.mScoreWallet.assureEntry(Purse.SW_KEY_ACHIEVEMENT_SCORE).getValue() + " (loaded " + RiddleInitializer.INSTANCE.getRiddleManager().getLoadedScore() + ")");

    }

    public TestSubjectAchievementHolder getAchievementHolder() {
        return mAchievementHolder;
    }

    public int getCurrentScore() {
        return mPurse.getCurrentScore();
    }

    public int getAchievementScore() {
        return mPurse.getAchievementScore();
    }

    public Dependency makeAchievementDependency(PracticalRiddleType type, int number) {
        TypeAchievementHolder typeAchievements = mAchievementHolder.getTypeAchievementHolder(type);
        if (typeAchievements != null) {
            Achievement dep = typeAchievements.getByNumber(number);
            if (dep != null) {
                return new MinValueDependency(dep, dep.getMaxValue());
            }
        }
        return null;
    }

    public Dependency makeClaimedAchievementDependency(PracticalRiddleType type, int number) {
        TypeAchievementHolder typeAchievementHolder = mAchievementHolder.getTypeAchievementHolder(type);
        if (typeAchievementHolder != null) {
            Achievement dep = typeAchievementHolder.getByNumber(number);
            if (dep != null) {
                return new ClaimedAchievementDependency(dep);
            }
        }
        return null;
    }

    private static class ClaimedAchievementDependency extends Dependency {
        private Achievement mAchievement;

        public ClaimedAchievementDependency(Achievement achievement) {
            mAchievement = achievement;
            if (mAchievement == null) {
                throw new IllegalArgumentException("No achievement given.");
            }
        }

        @Override
        public boolean isFulfilled() {
            return mAchievement.isAchieved() && !mAchievement.isRewardClaimable();
        }

        @Override
        public CharSequence getName(Resources res) {
            return mAchievement.getName(res);
        }
    }
}
