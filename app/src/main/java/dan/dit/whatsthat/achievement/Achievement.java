package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.testsubject.dependencies.Dependency;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener {
    private static final String SEPARATOR = "_";
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_VALUE = "value";
    private static final String KEY_MAX_VALUE = "maxvalue";
    public static final boolean DEFAULT_IS_DISCOVERED = true;
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 1;

    protected final String mId;
    protected boolean mDiscovered;
    protected final AchievementManager mManager;
    protected int mValue;
    protected int mMaxValue;
    protected final int mLevel;
    protected final int mScoreReward;
    protected final int mNameResId;
    protected final int mDescrResId;
    protected final int mRewardResId;
    protected final List<Dependency> mDependencies;

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

    public abstract int getIconResId();

    public CharSequence getName(Resources res) {
        return mNameResId == 0 ? mId : res.getString(mNameResId);
    }

    public String getDescription(Resources res) {
        return mDescrResId == 0 ? "": res.getString(mDescrResId);
    }

    public String getRewardDescription(Resources res) {
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

    public int getLevel() {
        return mLevel;
    }

    public int getScoreReward() {
        return mScoreReward;
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
        onAchieved();
        mManager.onChanged(this);
    }

    protected abstract void onAchieved();

    protected final void addData(SharedPreferences.Editor editor) {
        editor
                .putBoolean(mId + SEPARATOR + KEY_DISCOVERED, mDiscovered)
                .putInt(mId + SEPARATOR + KEY_VALUE, mValue)
                .putInt(mId + SEPARATOR + KEY_MAX_VALUE, mMaxValue);

    }

    private void loadData(SharedPreferences prefs, int defaultMaxValue) {
        mDiscovered = prefs.getBoolean(mId + SEPARATOR + KEY_DISCOVERED, DEFAULT_IS_DISCOVERED);
        mValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_VALUE);
        mMaxValue = prefs.getInt(mId + SEPARATOR + KEY_MAX_VALUE, defaultMaxValue);
        Log.d("Achievement", "Loaded achievement data : " + mDiscovered + " " + mValue + " " + mMaxValue);
    }

    public boolean isDiscovered() {
        return mDiscovered;
    }

    public int getProgress() {
        return (int) (PercentProgressListener.PROGRESS_COMPLETE * mValue / (double) mMaxValue);
    }
}
