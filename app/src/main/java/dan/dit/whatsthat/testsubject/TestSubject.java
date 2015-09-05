package dan.dit.whatsthat.testsubject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.github.johnpersano.supertoasts.SuperToast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.achievement.MiscAchievement;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.intro.GeneralStartingEpisode;
import dan.dit.whatsthat.testsubject.intro.Intro;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.dependencies.Dependable;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.util.dependencies.MinValueDependency;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.sortiment.ShopArticleRiddleHints;
import dan.dit.whatsthat.util.wallet.Wallet;
import dan.dit.whatsthat.util.wallet.WalletEntry;
import dan.dit.whatsthat.util.DelayedQueueProcessor;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 11.04.15.
 */
public class TestSubject {
    private static final TestSubject INSTANCE = new TestSubject();
    public static final String EMAIL_ON_ERROR = "whatsthat.contact@gmail.com";
    public static final String EMAIL_FEEDBACK = "whatsthat.feedback@gmail.com";
    private static final String TEST_SUBJECT_PREFERENCES_FILE = "dan.dit.whatsthat.testsubject_preferences";
    private static final String TEST_SUBJECT_PREF_RIDDLE_TYPES = "key_testsubject_riddletypes";
    private static final String TEST_SUBJECT_PREF_GENDER = "key_testsubject_gender";
    private static final int DEFAULT_SKIPABLE_GAMES = 15;

    private static final int GENDER_NOT_CHOSEN = -1;
    public static final int GENDER_MALE = 0; // genders as int, not boolean .. you never know, male = 0 chosen by fair dice role, take that feminists
    public static final int GENDER_FEMALE = 1;
    public static final int GENDER_WHATEVER = 2;
    public static final int GENDER_ALIEN = 3; // :o


    private boolean mInitialized;


    private SharedPreferences mPreferences;
    private int mGender = GENDER_NOT_CHOSEN;
    private Random mRand = new Random();
    private List<TestSubjectRiddleType> mTypes = new ArrayList<>();

    private Context mApplicationContext;
    private TestSubjectAchievementHolder mAchievementHolder;
    private Purse mPurse;
    private DelayedQueueProcessor<TestSubjectToast> mGeneralToastProcessor;
    private DelayedQueueProcessor<Achievement> mAchievementToastProcessor;
    private ShopArticleHolder mShopArticleHolder;
    private int mCurrLevel;
    private TestSubjectLevel[] mLevels;

    private TestSubject() {
    }

    public static TestSubject initialize(Context context) {
        INSTANCE.mApplicationContext = context.getApplicationContext();
        AchievementManager.initialize(INSTANCE.mApplicationContext);
        INSTANCE.initPreferences();
        INSTANCE.initLevels();
        INSTANCE.mInitialized = true;
        INSTANCE.mAchievementHolder = new TestSubjectAchievementHolder(AchievementManager.getInstance());
        INSTANCE.mAchievementHolder.addDependencies();
        INSTANCE.mShopArticleHolder.makeDependencies();
        INSTANCE.mAchievementHolder.initAchievements();
        return INSTANCE;
    }

    public int getGender() {
        return mGender;
    }

    public void setGender(int gender) {
        mGender = gender;
        mPreferences.edit().putInt(TEST_SUBJECT_PREF_GENDER, mGender).apply();
    }

    @SuppressLint("CommitPrefEdits")
    public void saveIntro(Intro intro) {
        if (intro == null) {
            return;
        }
        intro.save(mPreferences.edit(), mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_TESTSUBJECT_LEVEL));
    }

    public Intro makeIntro(View introView) {
        int level = mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_TESTSUBJECT_LEVEL);
        TestSubjectLevel currLevel = mLevels[level];
        Intro intro = Intro.makeIntro(introView, currLevel);
        intro.load(mPreferences, level);
        Resources res = intro.getResources();
        String testSubjDescr =
                res.getString(R.string.intro_test_subject_name)
                        +"\n"
                        + res.getString(currLevel.mNameResId)
                        + "\n"
                        + res.getString(R.string.intro_test_subject_estimated_intelligence)
                        +"\n"
                        + res.getString(currLevel.mIntelligenceResId);
        ((TextView) intro.findViewById(R.id.intro_subject_descr)).setText(testSubjDescr);
        new GeneralStartingEpisode(intro, res.getString(R.string.intro_starting_episode, res.getString(currLevel.mNameResId)), currLevel).start();
        if (intro.getCurrentEpisode() == null) {
            intro.nextEpisode(); // if this level is loaded for the first time we need to set the initial episode
        } else {
            intro.startUnmanagedEpisode(intro.getCurrentEpisode());
        }
        return intro;
    }

    public Dependable getLevelDependency() {
        return mPurse.mShopWallet.assureEntry(Purse.SHW_KEY_TESTSUBJECT_LEVEL);
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
        mShopArticleHolder = new SortimentHolder(mApplicationContext, new ForeignPurse(mPurse));
        mGender = mPreferences.getInt(TEST_SUBJECT_PREF_GENDER, GENDER_NOT_CHOSEN);

        // TestSubjectRiddleTypes
        String typesDataRaw = mPreferences.getString(TEST_SUBJECT_PREF_RIDDLE_TYPES, null);
        Compacter typesData = TextUtils.isEmpty(typesDataRaw) ? null : new Compacter(typesDataRaw);
        if (typesData != null) {
            for (String typeRaw : typesData) {
                try {
                    TestSubjectRiddleType type = new TestSubjectRiddleType(new Compacter(typeRaw));
                    if (!mTypes.contains(type)) {
                        mTypes.add(type);
                    }
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

    public boolean levelUp() {
        if (mCurrLevel >= mLevels.length - 1) {
            return false;
        }
        mPurse.mShopWallet.editEntry(Purse.SHW_KEY_TESTSUBJECT_LEVEL).add(1);
        mCurrLevel = mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_TESTSUBJECT_LEVEL);
        TestSubjectLevel currLevel = mLevels[mCurrLevel];
        currLevel.onLeveledUp();
        currLevel.applyLevel(mApplicationContext.getResources());
        return true;
    }

    private void initLevels() {
        mLevels = TestSubjectLevel.makeAll(this);
        WalletEntry levelEntry = mPurse.mShopWallet.assureEntry(Purse.SHW_KEY_TESTSUBJECT_LEVEL, TestSubjectLevel.LEVEL_NONE);
        mCurrLevel = levelEntry.getValue();
        if (mCurrLevel == TestSubjectLevel.LEVEL_NONE) {
            levelUp();
        } else {
            mLevels[mCurrLevel].applyLevel(mApplicationContext.getResources());
        }
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
        mAchievementHolder.getMiscData().updateMappedValue(MiscAchievementHolder.KEY_ACHIEVEMENTS_EARNED_COUNT, achievement.getId());
    }

    protected void addNewType(PracticalRiddleType type) {
        for (TestSubjectRiddleType currType : mTypes) {
            if (currType.getType().equals(type)) {
                return;
            }
        }
        mTypes.add(new TestSubjectRiddleType(type));
        mPurse.setAvailableRiddleHintsAtStartCount(type);
    }

    public List<TestSubjectRiddleType> getAvailableTypes() {
        return new ArrayList<>(mTypes);
    }

    public boolean canSkip() {
        return mPurse.mShopWallet.assureEntry(Purse.SHW_KEY_SKIPABLE_GAMES, DEFAULT_SKIPABLE_GAMES).getValue()
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
            types = new ArrayList<>(mTypes);
            return types.get(mRand.nextInt(types.size())).getType();
        }
    }

    public void addSolvedRiddleScore(int score) {
        mPurse.mScoreWallet.editEntry(Purse.SW_KEY_SOLVED_RIDDLE_SCORE).add(score);
        if (BuildConfig.DEBUG) {
            WalletEntry entry = mPurse.mScoreWallet.assureEntry(Purse.SW_KEY_SOLVED_RIDDLE_SCORE);
            Log.d("HomeStuff", "Adding " + score + " to wallet, new riddle score: " + entry.getValue());
        }
    }

    public void addAchievementScore(int score) {
        mPurse.mScoreWallet.editEntry(Purse.SW_KEY_ACHIEVEMENT_SCORE).add(score);
        Log.d("HomeStuff", "Adding " + score + " to wallet, new achievement score: " + mPurse.mScoreWallet.assureEntry(Purse.SW_KEY_ACHIEVEMENT_SCORE).getValue());

    }

    public TestSubjectAchievementHolder getAchievementHolder() {
        return mAchievementHolder;
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

    public int getCurrentRiddleHintNumber(PracticalRiddleType type) {
        return mPurse.getCurrentRiddleHintNumber(type);
    }

    public void increaseRiddleHintsDisplayed(PracticalRiddleType type) {
        int newNumber = mPurse.increaseCurrentRiddleHintNumber(type);
        mAchievementHolder.getMiscData().putValue(ShopArticleRiddleHints.makeKey(type), (long) (newNumber - 1), AchievementProperties.UPDATE_POLICY_ALWAYS);
    }

    public boolean hasAvailableHint(PracticalRiddleType type) {
        return hasAvailableHint(type, mPurse.getCurrentRiddleHintNumber(type));
    }

    public boolean hasAvailableHint(PracticalRiddleType type, int hintNumber) {
        return hintNumber < mPurse.getAvailableRiddleHintsCount(type);
    }

    public boolean hasFeature(String featureKey) {
        return mPurse.mShopWallet.getEntryValue(featureKey) != WalletEntry.FALSE;
    }

    public Dependency makeProductPurchasedDependency(String articleKey, int productIndex) {
        return new ProductPurchasedDependency(mShopArticleHolder.getArticle(articleKey), productIndex);
    }

    private static final Comparator<PracticalRiddleType> TYPE_COMPARATOR = new Comparator<PracticalRiddleType>() {
        @Override
        public int compare(PracticalRiddleType t1, PracticalRiddleType t2) {
            if (t1.equals(t2)) {
                return 0;
            } else if (TestSubject.isInitialized()) {
                List<TestSubjectRiddleType> sortedTypes = TestSubject.getInstance().mTypes;
                int index = 0;
                int pos1 = 0;
                int pos2 = 0;
                for (TestSubjectRiddleType type : sortedTypes) {
                    if (type.getType().equals(t1)) {
                        pos1 = index;
                    } else if (type.getType().equals(t2)) {
                        pos2 = index;
                    }
                    index++;
                }
                return pos1 - pos2;
            } else {
                return t1.getFullName().compareTo(t2.getFullName());
            }
        }
    };
    public static void sortTypes(List<PracticalRiddleType> types) {
        Collections.sort(types, TYPE_COMPARATOR);
    }

    public boolean purchaseNextHintForFree(PracticalRiddleType type) {
        ForeignPurse purse = mShopArticleHolder.getPurse();
        return purse.purchaseHint(type, 0);
    }

    public int getRiddleSolvedResIds() {
        return mLevels[mCurrLevel].mRiddleSolvedCandy;
    }

    public int getImageResId() {
        return mLevels[mCurrLevel].mImageResId;
    }

    private static final String KEY_LAST_SCORE_MULTIPLIER_REFRESH_DAY_OF_YEAR = "dan.dit.whatsthat.score_multiplier_day_of_year";
    private static final String KEY_SCORE_BONUS_COUNT = "dan.dit.whatsthat.score_bonus_count";
    public int getAndIncrementTodaysScoreBonusCount() {
        Calendar now = Calendar.getInstance();
        int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        int lastMultiplierRefreshDay = mPreferences.getInt(KEY_LAST_SCORE_MULTIPLIER_REFRESH_DAY_OF_YEAR, -1);
        //this will not refresh if the last refresh day was exactly one year ago, this is not a bug but a runtime feature
        if (lastMultiplierRefreshDay == -1 || lastMultiplierRefreshDay != currentDayOfYear) {
            //refresh the day (of year) to get a new bonus for today
            mPreferences.edit()
                    .putInt(KEY_LAST_SCORE_MULTIPLIER_REFRESH_DAY_OF_YEAR, currentDayOfYear)
                    .putInt(KEY_SCORE_BONUS_COUNT, -1)
                    .apply();
        }
        // get and increment bonus count
        int bonusCount = mPreferences.getInt(KEY_SCORE_BONUS_COUNT, 0);
        mPreferences.edit().putInt(KEY_SCORE_BONUS_COUNT, bonusCount + 1).apply();
        return bonusCount;
    }

    public int getCurrentScore() {
        return mPurse.getCurrentScore();
    }

    public void registerScoreChangedListener(Wallet.OnEntryChangedListener listener) {
        mPurse.mScoreWallet.addChangedListener(listener);
    }

    public void removeScoreChangedListener(Wallet.OnEntryChangedListener listener) {
        mPurse.mScoreWallet.removeChangedListener(listener);
    }

    private static class ProductPurchasedDependency extends Dependency {
        private final int mProduct;
        private final ShopArticle mArticle;

        private ProductPurchasedDependency(ShopArticle article, int product) {
            mArticle = article;
            mProduct = product;
            if (article == null) {
                throw new IllegalArgumentException("No article for dependency given.");
            }
        }
        @Override
        public boolean isNotFulfilled() {
            return mArticle.isPurchasable(mProduct) != ShopArticle.HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }

        @Override
        public CharSequence getName(Resources res) {
            if (mProduct < 0) {
                return mArticle.getName(res);
            } else {
                return mArticle.getName(res) + " #" + (mProduct + 1);
            }
        }
    }

    private Map<PracticalRiddleType, Dependency> mTypeDependencies = new HashMap<>();
    public Dependency getRiddleTypeDependency(PracticalRiddleType type) {
        Dependency dep = mTypeDependencies.get(type);
        if (dep == null) {
            dep = new RiddleTypeDependency(type);
            mTypeDependencies.put(type, dep);
        }
        return dep;
    }

    private class RiddleTypeDependency extends Dependency {
        private PracticalRiddleType mType;
        private RiddleTypeDependency(PracticalRiddleType type) {
            mType = type;
        }
        @Override
        public boolean isNotFulfilled() {
            for (TestSubjectRiddleType testType : mTypes) {
                if (testType.getType().equals(mType)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public CharSequence getName(Resources res) {
            return res.getString(mType.getNameResId());
        }
    }

    public ShopArticleHolder getShopSortiment() {
        return mShopArticleHolder;
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
        public boolean isNotFulfilled() {
            return !mAchievement.isAchieved() || mAchievement.isRewardClaimable();
        }

        @Override
        public CharSequence getName(Resources res) {
            return mAchievement.getName(res);
        }
    }
}
