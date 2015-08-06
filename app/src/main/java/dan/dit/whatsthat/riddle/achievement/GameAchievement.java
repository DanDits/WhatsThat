package dan.dit.whatsthat.riddle.achievement;

import android.content.res.Resources;
import android.util.Log;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.dependencies.MinValueDependency;

/**
 * Created by daniel on 22.05.15.
 */
public abstract class GameAchievement extends Achievement {
    protected final AchievementDataRiddleType mTypeData;
    protected final AchievementDataRiddleGame mGameData;
    protected final AchievementDataTimerRiddle mTimerData;

    private boolean mInitialized;
    private int mIconResId;

    protected GameAchievement(PracticalRiddleType type, int nameResId, int descrResId, int rewardResId, int number, AchievementManager manager, int level, int scoreReward, int maxValue, boolean discovered) {
        super(makeId(type, number), nameResId, descrResId, rewardResId, manager, level, scoreReward, maxValue, discovered);
        mIconResId = type.getIconResId();
        if (isAchieved()) {
            Log.d("Achievement", "Creating achievement " + type.getFullName() + number + ": already achieved.");
            mTypeData = null;
            mGameData = null;
            mTimerData = null;
            return;
        }
        mTypeData = type.getAchievementData(mManager);
        mGameData = type.getAchievementDataGame();
        mTimerData = AchievementDataTimerRiddle.getInstance(mManager);
    }

    public void setDependencies() {
        addLevelDependency();
    }

    @Override
    public int getIconResId() {
        return mIconResId;
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
                return R.drawable.eye;
            }
        } else {
            return R.drawable.eye_blind;
        }
    }

    private void addLevelDependency() {
        mDependencies.add(new LevelDependency(mLevel));
    }

    private static class LevelDependency extends MinValueDependency {

        public LevelDependency(int level) {
            super(TestSubject.getInstance().getLevelDependency(), level);
        }

        @Override
        public CharSequence getName(Resources res) {
            return res.getString(R.string.level_dependency, getMinValue());
        }
    }

    @Override
    protected void onDiscovered() {

    }

    @Override
    protected void onAchieved() {
        mTypeData.removeListener(this);
        mGameData.removeListener(this);
        Log.d("Achievement", "Achieved: " + mId);
    }

    public final void init() {
        if (!isAchieved() && !mInitialized) {
            mInitialized = true;
            mTypeData.addListener(this);
            mGameData.addListener(this);
            onInit();
        }
    }

    public void onInit() {

    }

    private static String makeId(PracticalRiddleType type, int number) {
        return type.getFullName() + number;
    }

}
