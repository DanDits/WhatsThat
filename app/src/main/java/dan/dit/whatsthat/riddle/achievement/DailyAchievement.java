package dan.dit.whatsthat.riddle.achievement;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.LevelDependency;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 12.01.16.
 */
public abstract class DailyAchievement extends Achievement {

    private static final String KEY_LAST_RESET_TIMESTAMP = "daily_lastreset";
    private static final String KEY_AVAILABLE_TODAY = "daily_available";
    private static final boolean DEFAULT_IS_AVAILABLE = false;
    private static final int REQUIRED_DIFFERENT_RIDDLE_TYPES_FOR_DAILY_ACHIEVEMENTS = 2;
    private boolean mInitialized;
    private static final Calendar CHECKER = Calendar.getInstance();
    private static final Date CHECKER_DATE = new Date();
    private long mLastResetTimestamp;
    private boolean mAvailableToday;

    protected DailyAchievement(int nameResId, int descrResId, int rewardResId,
                               AchievementManager manager, int level, int number,
                               int scoreReward, int maxValue, boolean discovered) {
        super(makeId(number), nameResId, descrResId, rewardResId, manager, level, scoreReward,
                maxValue, discovered);
    }

    private static final Dependency MIN_DIFFERENT_RIDDLE_TYPES_DEPENDENCY = new Dependency() {
            @Override
            public boolean isFulfilled() {
                return TestSubject.getInstance().getTestSubjectRiddleTypeCountDependency().getValue()
                        >= REQUIRED_DIFFERENT_RIDDLE_TYPES_FOR_DAILY_ACHIEVEMENTS;
            }

            @Override
            public CharSequence getName(Resources res) {
                return res.getString(R.string.dependency_testsubject_riddle_type_min_count,
                        REQUIRED_DIFFERENT_RIDDLE_TYPES_FOR_DAILY_ACHIEVEMENTS);
            }
        };

    public void setDependencies() {
        addLevelDependency();
        // get achievement "the beginning"
        mDependencies.add(TestSubject.getInstance().makeClaimedAchievementDependency
                (PracticalRiddleType.CIRCLE_INSTANCE, AchievementCircle.Achievement1.NUMBER));

        // get another riddle type
        mDependencies.add(MIN_DIFFERENT_RIDDLE_TYPES_DEPENDENCY);
    }

    @Override
    public final void onDataEvent(AchievementDataEvent event) {
        if (isAvailable()) {
            onIsAvailableDataEvent(event);
        }
    }

    protected abstract void onIsAvailableDataEvent(AchievementDataEvent event);

    @Override
    public int getIconResId() {
        return R.drawable.eye_refresh;
    }

    public int getIconResIdByState() {
        if (!areDependenciesFulfilled()) {
            if (isDiscovered()) {
                return R.drawable.eye_locked;
            } else {
                return R.drawable.eye_blind_locked;
            }
        }
        if (mDiscovered) {
            if (isAchieved()) {
                if (isRewardClaimable()) {
                    return R.drawable.alien_achieved;
                } else {
                    return R.drawable.alien_achieved_claimed;
                }
            } else {
                return getIconResId();
            }
        } else {
            return R.drawable.eye_blind;
        }
    }

    public final void init(@NonNull Calendar initTime) {
        if (!mInitialized && isNotAchievedToday(initTime)) {
            Log.d("Achievement", "Initializing " + mId);
            mInitialized = true;
            onInit();
            checkedReset(initTime);
        }
    }

    public boolean checkedReset(@NonNull Calendar initTime) {
        if (isNotAchievedToday(initTime) && !gotResetToday(initTime)) {
            // ensure initialized
            if (!mInitialized) {
                init(initTime); // will also invoke reset by invoking checkedReset again
            } else {
                reset(initTime);
            }
            return true;
        }
        return false;
    }

    private void reset(Calendar initTime) {
        Log.d("Achievement", "Resetting " + mId + " on " + initTime);
        mLastResetTimestamp = initTime.getTimeInMillis();
        mAvailableToday = DEFAULT_IS_AVAILABLE;
        resetAnyProgress();
        onReset();
    }

    protected abstract void onReset();

    private boolean isNotAchievedToday(@NonNull Calendar today) {
        CHECKER_DATE.setTime(mAchievedTimestamp);
        CHECKER.setTime(CHECKER_DATE);
        return isNextDay(today, CHECKER);
    }

    public boolean gotResetToday(@NonNull Calendar today) {
        CHECKER_DATE.setTime(mLastResetTimestamp);
        CHECKER.setTime(CHECKER_DATE);
        return isToday(today, CHECKER);
    }

    private static boolean isNextDay(Calendar today, Calendar previous) {
        return (today.get(Calendar.DAY_OF_YEAR) > previous.get(Calendar.DAY_OF_YEAR)
                    && today.get(Calendar.YEAR) == previous.get(Calendar.YEAR))
                || today.get(Calendar.YEAR) > previous.get(Calendar.YEAR);
    }

    private static boolean isToday(Calendar today, Calendar check) {
        return today.get(Calendar.DAY_OF_YEAR) == check.get(Calendar.DAY_OF_YEAR)
                && today.get(Calendar.YEAR) == check.get(Calendar.YEAR);
    }

    @Override
    protected void addData(SharedPreferences.Editor editor) {
        super.addData(editor);
        editor
                .putLong(mId + Achievement.SEPARATOR + KEY_LAST_RESET_TIMESTAMP,
                        mLastResetTimestamp)
                .putBoolean(mId + Achievement.SEPARATOR + KEY_AVAILABLE_TODAY, mAvailableToday);
    }

    protected void loadData(SharedPreferences prefs) {
        super.loadData(prefs);
        mLastResetTimestamp = prefs.getLong(mId + Achievement.SEPARATOR +
                KEY_LAST_RESET_TIMESTAMP, 0L);
        mAvailableToday = prefs.getBoolean(mId + Achievement.SEPARATOR + KEY_AVAILABLE_TODAY,
                DEFAULT_IS_AVAILABLE);
    }

    public void setAvailable() {
        mAvailableToday = true;
        mManager.onChanged(this, AchievementManager.CHANGED_OTHER);
    }

    public boolean isAvailable() {
        return mAvailableToday;
    }

    public void onInit() {

    }

    @Override
    protected void onDiscovered() {

    }

    private void addLevelDependency() {
        mDependencies.add(LevelDependency.getInstance(mLevel));
    }

    @Override
    protected void onAchieved() {
        Log.d("Achievement", "Achieved: " + mId);
    }

    public int getExpectedScore(int forLevel) {
        return 0; // do not expect daily achievements to produce any score
    }

    protected static String makeId(int number) {
        return "daily_achievement" + number;
    }
}
