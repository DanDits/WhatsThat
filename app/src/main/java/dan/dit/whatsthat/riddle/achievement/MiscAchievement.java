package dan.dit.whatsthat.riddle.achievement;

import android.content.res.Resources;
import android.util.Log;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.dependencies.MinValueDependency;

/**
 * Created by daniel on 23.07.15.
 */
public abstract class MiscAchievement extends Achievement {
    protected final AchievementPropertiesMapped<String> mMiscData;

    private boolean mInitialized;

    protected MiscAchievement(AchievementPropertiesMapped<String> miscData, int nameResId, int descrResId, int rewardResId, int number, AchievementManager manager, int level, int scoreReward, int maxValue, boolean discovered) {
        super(makeId(number), nameResId, descrResId, rewardResId, manager, level, scoreReward, maxValue, discovered);
        if (isAchieved()) {
            Log.d("Achievement", "Creating misc achievement " + number + ": already achieved.");
            mMiscData = null;
            return;
        }
        mMiscData = miscData;
    }

    public void setDependencies() {
        addLevelDependency();
    }

    @Override
    public int getIconResId() {
        return getIconResIdByState();
    }

    @Override
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
        mMiscData.removeListener(this);
        Log.d("Achievement", "Achieved: " + mId);
    }

    public final void init() {
        if (!isAchieved() && !mInitialized) {
            mInitialized = true;
            mMiscData.addListener(this);
            onInit();
        }
    }

    public void onInit() {

    }

    protected static String makeId(int number) {
        return "misc_achievement" + number;
    }
}
