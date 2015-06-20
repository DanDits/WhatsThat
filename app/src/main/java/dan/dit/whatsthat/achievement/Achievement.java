package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.testsubject.dependencies.Dependable;
import dan.dit.whatsthat.testsubject.dependencies.Dependency;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener, Dependable {
    private static final String SEPARATOR = "_";
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_VALUE = "value";
    private static final String KEY_MAX_VALUE = "maxvalue";
    private static final String KEY_ACHIEVED_TIMESTAMP = "achievedtime";
    private static final String KEY_REWARD_CLAIMED = "rewardclaimed";
    public static final boolean DEFAULT_IS_DISCOVERED = true;
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 1;

    protected final String mId;
    protected boolean mDiscovered;
    protected final AchievementManager mManager;
    protected int mValue;
    protected int mMaxValue;
    protected final int mLevel;
    private final int mScoreReward;
    private boolean mRewardClaimed;
    protected final int mNameResId;
    protected final int mDescrResId;
    protected final int mRewardResId;
    protected final List<Dependency> mDependencies;
    protected long mAchievedTimestamp;

    public Achievement(String id, int nameResId, int descrResId, int rewardResId, AchievementManager manager, int level, int scoreReward, int maxValue) {
        mId = id;
        mMaxValue = Math.max(DEFAULT_MAX_VALUE, maxValue);
        mNameResId = nameResId;
        mDescrResId = descrResId;
        mRewardResId = rewardResId;
        mDependencies = new ArrayList<>();
        mManager = manager;
        mLevel = level;
        mScoreReward = Math.max(scoreReward, 0);
        if (TextUtils.isEmpty(mId)) {
            throw new IllegalArgumentException("Null id given.");
        }
        if (manager == null) {
            throw new IllegalArgumentException("Null manager given.");
        }
        loadData(manager.getSharedPreferences(), mMaxValue);
        onCreated();
    }


    @Override
    public int getValue() {
        return mValue;
    }

    public abstract int getIconResId();

    public abstract int getIconResIdByState();

    public CharSequence getName(Resources res) {
        return mNameResId == 0 ? mId : res.getString(mNameResId);
    }

    public CharSequence getDescription(Resources res) {
        return mDescrResId == 0 ? "": res.getString(mDescrResId);
    }

    public CharSequence getRewardDescription(Resources res) {
        return mRewardResId == 0 ? ("+" + getScoreReward()) : res.getString(mRewardResId, getScoreReward());
    }

    public boolean areDependenciesFulfilled() {
        for (Dependency d : mDependencies) {
            if (!d.isFulfilled()) {
                return false;
            }
        }
        return true;
    }

    protected  boolean achieveAfterDependencyCheck() {
        if (areDependenciesFulfilled()) {
            achieve();
            return true;
        }
        Log.d("Achievement", "Trying to achieve " + mId + ", but depencies are not fulfilled: " + mDependencies);
        return false;
    }

    protected void achieveProgressPercent(int progress) {
        if (isAchieved()) {
            return;
        }
        if (progress > 100) {
            progress = 100;
        } else if (progress < 0) {
            progress = 0;
        }
        int oldValue = mValue;
        mValue = (int) (progress * mMaxValue / 100.);
        Log.d("Achievement", "Achieving " + progress + " percent of " + mMaxValue + ": " + oldValue + "->" + mValue);
        if (mValue >= mMaxValue || progress == 100) {
            achieve();
        } else if (mValue != oldValue) {
            mManager.onChanged(this);
        }
    }

    protected void achieveDelta(int delta) {
        if (isAchieved()) {
            return;
        }
        mValue += delta;
        if (mValue >= mMaxValue) {
            achieve();
        } else if (delta != 0) {
            mManager.onChanged(this);
        }
    }

    protected void addDeltaIfNotAchieved(int delta) {
        if (isAchieved() || mValue + delta >= mMaxValue) {
            return;
        }
        achieveDelta(delta);
    }

    public int getLevel() {
        return mLevel;
    }

    public int getScoreReward() {
        return mScoreReward;
    }

    public boolean isRewardClaimable() {
        return !mRewardClaimed && isAchieved();
    }

    public int claimReward() {
        if (isRewardClaimable()) {
            mRewardClaimed = true;
            mManager.onChanged(this);
            return mScoreReward;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Achievement) {
            return mId.equals(((Achievement) other).mId);
        } else {
            return super.equals(other);
        }
    }

    protected abstract void onCreated();

    protected synchronized final void discover() {
        if (mDiscovered) {
            return; // already discovered
        }
        mDiscovered = true;
        onDiscovered();
        mManager.onChanged(this);
    }

    protected synchronized final void cover() {
        if (!mDiscovered) {
            return;
        }
        mDiscovered = false;
        mManager.onChanged(this);
    }

    protected abstract void onDiscovered();

    public boolean isAchieved() {
        return mValue >= mMaxValue;
    }

    protected synchronized final void achieve() {
        if (isAchieved()) {
            return;
        }
        if (!mDiscovered) {
            discover();
        }
        mValue = mMaxValue;
        mAchievedTimestamp = System.currentTimeMillis();
        onAchieved();
        mManager.onChanged(this);
    }

    protected abstract void onAchieved();

    protected final void addData(SharedPreferences.Editor editor) {
        editor
                .putBoolean(mId + SEPARATOR + KEY_DISCOVERED, mDiscovered)
                .putInt(mId + SEPARATOR + KEY_VALUE, mValue)
                .putInt(mId + SEPARATOR + KEY_MAX_VALUE, mMaxValue)
                .putLong(mId + SEPARATOR + KEY_ACHIEVED_TIMESTAMP, mAchievedTimestamp)
                .putBoolean(mId + SEPARATOR + KEY_REWARD_CLAIMED, mRewardClaimed);
        Log.d("Achievement", "Adding achievement data : " + mDiscovered + " " + mValue + " " + mMaxValue + " " + mAchievedTimestamp + " " + mRewardClaimed);

    }

    private void loadData(SharedPreferences prefs, int defaultMaxValue) {
        mDiscovered = prefs.getBoolean(mId + SEPARATOR + KEY_DISCOVERED, DEFAULT_IS_DISCOVERED);
        mValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_VALUE);
        mMaxValue = prefs.getInt(mId + SEPARATOR + KEY_MAX_VALUE, defaultMaxValue);
        mAchievedTimestamp = prefs.getLong(mId + SEPARATOR + KEY_ACHIEVED_TIMESTAMP, 0L);
        mRewardClaimed = prefs.getBoolean(mId + SEPARATOR + KEY_REWARD_CLAIMED, false);
        Log.d("Achievement", "Loaded achievement data : " + mDiscovered + " " + mValue + " " + mMaxValue + " " + mAchievedTimestamp + " " + mRewardClaimed);
    }

    public boolean isDiscovered() {
        return mDiscovered;
    }

    public int getProgress() {
        return (int) (PercentProgressListener.PROGRESS_COMPLETE * mValue / (double) mMaxValue);
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public List<Dependency> getDependencies() {
        return mDependencies;
    }
}
