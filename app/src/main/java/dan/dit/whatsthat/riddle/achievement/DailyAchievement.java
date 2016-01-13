package dan.dit.whatsthat.riddle.achievement;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.testsubject.LevelDependency;

/**
 * Created by daniel on 12.01.16.
 */
public abstract class DailyAchievement extends Achievement {

    private static final String KEY_LAST_RESET_TIMESTAMP = "daily_lastreset";
    protected final AchievementPropertiesMapped<String> mMiscData;
    private boolean mInitialized;
    private static final Calendar CHECKER = Calendar.getInstance();
    private static final Date CHECKER_DATE = new Date();
    private long mLastResetTimestamp;

    protected DailyAchievement(int nameResId, int descrResId, int rewardResId,
                               AchievementManager manager, int level, int number,
                               int scoreReward, int maxValue, boolean discovered,
                               @NonNull AchievementPropertiesMapped<String> miscData) {
        super(makeId(number), nameResId, descrResId, rewardResId, manager, level, scoreReward,
                maxValue, discovered);
        mMiscData = miscData;
    }

    public void setDependencies() {
        addLevelDependency();
    }

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
                return R.drawable.eye_refresh;
            }
        } else {
            return R.drawable.eye_blind;
        }
    }

    public final void init(@NonNull Calendar initTime) {
        if (!mInitialized && isNotAchievedToday(initTime)) {
            Log.d("Achievement", "Initializing " + mId);
            mInitialized = true;
            mMiscData.addListener(this);
            onInit();
            if (!gotResetToday(initTime)) {
                reset(initTime);
            }
        }
    }

    private void reset(Calendar initTime) {
        Log.d("Achievement", "Resetting " + mId);
        mLastResetTimestamp = initTime.getTimeInMillis();
        resetAnyProgress();
        onReset();
    }

    protected abstract void onReset();

    private boolean isNotAchievedToday(@NonNull Calendar today) {
        CHECKER_DATE.setTime(mAchievedTimestamp);
        CHECKER.setTime(CHECKER_DATE);
        return isNextDay(today, CHECKER);
    }

    private boolean gotResetToday(@NonNull Calendar today) {
        CHECKER_DATE.setTime(mLastResetTimestamp);
        CHECKER.setTime(CHECKER_DATE);
        Log.d("Achievement", "Checking if got reset today: " + today + " and last reset: " +
                CHECKER);
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
                        mLastResetTimestamp);
    }

    protected void loadData(SharedPreferences prefs) {
        super.loadData(prefs);
        mLastResetTimestamp = prefs.getLong(mId + Achievement.SEPARATOR +
                KEY_LAST_RESET_TIMESTAMP, 0L);
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
        mMiscData.removeListener(this);
        Log.d("Achievement", "Achieved: " + mId);
    }

    protected static String makeId(int number) {
        return "daily_achievement" + number;
    }
}
