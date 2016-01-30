/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.testsubject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.github.johnpersano.supertoasts.SuperToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.intro.Intro;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.sortiment.ShopArticleRiddleHints;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.dependencies.Dependable;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.util.dependencies.MinValueDependency;
import dan.dit.whatsthat.util.general.DelayedQueueProcessor;
import dan.dit.whatsthat.util.wallet.Wallet;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Created by daniel on 11.04.15.
 */
public class TestSubject {
    private static final TestSubject INSTANCE = new TestSubject();
    public static final String EMAIL_ON_ERROR = "whatsthat.contact@gmail.com";
    public static final String EMAIL_FEEDBACK = "whatsthat.feedback@gmail.com";
    private static final String TEST_SUBJECT_PREFERENCES_FILE = "dan.dit.whatsthat.testsubject_preferences";
    private static final String TEST_SUBJECT_PREF_GENDER = "key_testsubject_gender";
    public static final int DEFAULT_SKIPABLE_GAMES = 5;

    public static final int GENDER_NOT_CHOSEN = -1;
    // used for array indices
    public static final int GENDER_MALE = 0; // genders as int, not boolean .. you never know, male = 0 chosen by fair dice role, take that feminists
    public static final int GENDER_FEMALE = 1;
    public static final int GENDER_WHATEVER = 2;
    public static final int GENDER_ALIEN = 3; // :o
    public static final int GENDERS_COUNT = 4; // amount of valid genders
    public static final String SHW_KEY_MAX_AVAILABLE_RIDDLE_TYPES = "shw_key_can_choose_new_riddle";
    private static final int AVAILABLE_RIDDLES_AT_GAME_START = 1; // one additional riddle type is granted per level up (also on level 0)


    private boolean mInitialized;


    private SharedPreferences mPreferences;
    private int mGender = GENDER_NOT_CHOSEN;

    private Context mApplicationContext;
    private TestSubjectAchievementHolder mAchievementHolder;
    private Purse mPurse;
    private DelayedQueueProcessor<TestSubjectToast> mGeneralToastProcessor;
    private DelayedQueueProcessor<Achievement> mAchievementToastProcessor;
    private ShopArticleHolder mShopArticleHolder;
    private int mCurrLevel;
    private TestSubjectLevel[] mLevels;
    private TestSubjectRiddleTypeController mTypesController;

    private TestSubject() {
    }

    public static synchronized TestSubject initialize(Context context) {
        if (isInitialized()) {
            return INSTANCE;
        }
        INSTANCE.mApplicationContext = context.getApplicationContext();
        INSTANCE.initAchievementManager();
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
        return currLevel.makeIntro(intro);
    }

    public int getCurrentLevel() {
        return mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_TESTSUBJECT_LEVEL, TestSubjectLevel.LEVEL_NONE);
    }

    public int getMaximumLevel() {
        return mLevels.length - 1; // since level numbers start with zero
    }

    public Dependable getLevelDependency() {
        return mPurse.mShopWallet.assureEntry(Purse.SHW_KEY_TESTSUBJECT_LEVEL);
    }

    public void initToasts() {
        mGeneralToastProcessor = new DelayedQueueProcessor<>(new DelayedQueueProcessor.Callback<TestSubjectToast>() {
            @Override
            public long process(@NonNull TestSubjectToast toProcess) {
                showToast(toProcess);
                return toProcess.mDuration;
            }
        });
        mAchievementToastProcessor = new DelayedQueueProcessor<>(new DelayedQueueProcessor.Callback<Achievement>() {
            @Override
            public long process(@NonNull Achievement toProcess) {
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

    private void initAchievementManager() {
        AchievementManager.initialize(mApplicationContext);
        AchievementManager.getInstance().addAchievementChangedListener(new AchievementManager.OnAchievementChangedListener() {
            @Override
            public void onDataEvent(AchievementManager.AchievementChangeEvent changeEvent) {
                switch (changeEvent.getChangedHint()) {
                    case AchievementManager.CHANGED_TO_ACHIEVED_AND_UNCLAIMED:
                        TestSubject.this.postAchievementAchieved(changeEvent.getAchievement());
                        break;
                    case AchievementManager.CHANGED_GOT_CLAIMED:
                        TestSubject.this.addAchievementScore(changeEvent.getAchievement()
                                .getScoreReward());
                        break;
                }
            }
        });
    }

    private void initPreferences() {
        mPreferences = mApplicationContext.getSharedPreferences(TEST_SUBJECT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mPurse = new Purse(mApplicationContext);
        mShopArticleHolder = new SortimentHolder(mApplicationContext, new ForeignPurse(mPurse));
        mGender = mPreferences.getInt(TEST_SUBJECT_PREF_GENDER, GENDER_NOT_CHOSEN);
        mTypesController = new TestSubjectRiddleTypeController(mPreferences);
    }


    public int getNextLevelUpCost() {
        if (mCurrLevel >= mLevels.length -1) {
            return -1; // already at max level
        }
        double fraction = mLevels[mCurrLevel + 1].getLevelUpAchievementScoreFraction();

        int maxAchievementScore = mAchievementHolder.getExpectableTestSubjectScore(mCurrLevel);
        int alreadySpent = mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_SPENT_SCORE_ON_LEVEL_UP);
        int cost =  (int) (maxAchievementScore * fraction) - alreadySpent;
        if (cost < 0) {
            return 0;
        }
        // now round to some decent value
        final int roundingPrecision = 5;
        return cost + roundingPrecision - (cost % roundingPrecision);
    }

    public synchronized boolean purchaseLevelUp() {
        return isLevelUpAvailable() && mPurse.purchaseLevelUp(getNextLevelUpCost())
                && levelUp();
    }

    public synchronized boolean isLevelUpAvailable() {
        if (mCurrLevel >= mLevels.length -1) {
            return false; // already at max level
        }
        return mCurrLevel == TestSubjectLevel.LEVEL_NONE || !canChooseNewRiddle();
    }

    /**
     * Increases the TestSubject level by one.
     * @return This is almost always true. An exception is if there is no more level available.
     */
    public boolean levelUp() {
        // every condition that will imply this method to return false also needs to imply that
        // isLevelUpAvailable also returns false. This method is only public to make available
        // for cheats.
        if (mCurrLevel >= mLevels.length -1) {
            return false; // already at max level
        }
        mPurse.mShopWallet.editEntry(SHW_KEY_MAX_AVAILABLE_RIDDLE_TYPES, AVAILABLE_RIDDLES_AT_GAME_START).add(1);
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
                Log.e("HomeStuff", "Trying to post toast. No handler initialized for testsubject.");
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

    public boolean addNewType(PracticalRiddleType type) {
        if (!mTypesController.addNewType(type)) {
            return false;
        }
        mPurse.setAvailableRiddleHintsAtStartCount(type);
        mTypesController.saveTypes();
        return true;
    }

    /**
     * Returns a copy of the list of all currently available TestSubjectRiddleTypes.
     * @return A new list containing the riddle types.
     */
    public List<TestSubjectRiddleType> getAvailableTypes() {
        return new ArrayList<>(mTypesController.getAll());
    }

    public boolean canSkip() {
        return mPurse.mShopWallet.assureEntry(Purse.SHW_KEY_SKIPABLE_GAMES, DEFAULT_SKIPABLE_GAMES).getValue()
                > RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddleCount();
    }

    public void ensureSkipableGames(int amount) {
        int current = mPurse.mShopWallet.getEntryValue(Purse.SHW_KEY_SKIPABLE_GAMES,
                DEFAULT_SKIPABLE_GAMES);
        if (current < amount) {
            mPurse.mShopWallet.editEntry(Purse.SHW_KEY_SKIPABLE_GAMES, DEFAULT_SKIPABLE_GAMES)
                    .set(amount);
        }
    }

    public boolean canChooseNewRiddle() {
        return mPurse.mShopWallet.getEntryValue(SHW_KEY_MAX_AVAILABLE_RIDDLE_TYPES,
                AVAILABLE_RIDDLES_AT_GAME_START) > mTypesController.getCount()
                && mTypesController.getCount() < PracticalRiddleType.ALL_PLAYABLE_TYPES.size();
    }

    public synchronized boolean chooseNewRiddle(PracticalRiddleType type) {
        if (mTypesController.isTypeAvailable(type) || !canChooseNewRiddle()) {
            return false;
        }
        if (addNewType(type)) {
            Log.d("Riddle", "Added and saved new type: " + type);
            return true;
        }
        return false;
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
                return new DependencyHolder.ClaimedAchievementDependency(dep);
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

    public boolean hasToggleableFeature(String featureKey) {
        return mPurse.hasToggleableFeature(featureKey);
    }

    public Dependency makeProductPurchasedDependency(String articleKey, int productIndex) {
        return new DependencyHolder.ProductPurchasedDependency(mShopArticleHolder.getArticle
                (articleKey), productIndex);
    }

    public Dependency makeFeatureAvailableDependency(String articleKey, int requiredFeatureValue) {
        return new DependencyHolder.FeatureAvailableDependency(mShopArticleHolder.getArticle
                (articleKey), requiredFeatureValue);
    }

    public static void sortTypes(List<PracticalRiddleType> types) {
        Collections.sort(types, TestSubjectRiddleTypeController.TYPE_COMPARATOR);
    }

    public boolean purchaseNextHintForFree(PracticalRiddleType type) {
        ForeignPurse purse = mShopArticleHolder.getPurse();
        return purse.purchaseHint(type, 0);
    }

    public int getRiddleSolvedResIds() {
        return mLevels[mCurrLevel].mRiddleSolvedCandy;
    }

    public int getImageResId() {
        return mLevels[mCurrLevel].getImageResourceId();
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

    public int getShopValue(String key) {
        return mPurse.mShopWallet.getEntryValue(key);
    }

    public TestSubjectRiddleTypeController getTypesController() {
        return mTypesController;
    }

    private final Dependable mTestSubjectRiddleTypeCountDependable = new Dependable() {

        @Override
        public CharSequence getName(Resources res) {
            return res.getString(R.string.dependable_testsubject_riddle_type_count);
        }

        @Override
        public int getValue() {
            return mTypesController.getCount();
        }
    };
    public Dependable getTestSubjectRiddleTypeCountDependency() {
            return mTestSubjectRiddleTypeCountDependable;
    }

    private Map<PracticalRiddleType, Dependency> mTypeDependencies = new HashMap<>();
    public Dependency getRiddleTypeDependency(PracticalRiddleType type) {
        Dependency dep = mTypeDependencies.get(type);
        if (dep == null) {
            dep = new DependencyHolder.RiddleTypeDependency(type, this);
            mTypeDependencies.put(type, dep);
        }
        return dep;
    }

    public ShopArticleHolder getShopSortiment() {
        return mShopArticleHolder;
    }

}
