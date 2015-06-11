package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;

import dan.dit.whatsthat.testsubject.dependencies.Dependable;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener, Dependable {
    private static final String SEPARATOR = "_";
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_ACHIEVED = "achieved";
    private static final String KEY_VALUE = "value";
    private static final String KEY_MAX_VALUE = "maxvalue";
    public static final boolean DEFAULT_IS_DISCOVERED = true;
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 1;

    protected final String mId;
    protected final int mNameResId;
    protected final int mDescrResId;
    protected final int mRewardResId;
    protected boolean mDiscovered;
    protected final AchievementManager mManager;
    protected int mValue;
    protected int mMaxValue;
    protected int mLevel;
    protected int mScoreReward;

    public Achievement(String id, int nameResId, int descrResId, int rewardResId, AchievementManager manager, int level, int scoreReward) {
        mId = id;
        mNameResId = nameResId;
        mDescrResId = descrResId;
        mRewardResId = rewardResId;
        mManager = manager;
        mLevel = level;
        mScoreReward = Math.max(scoreReward, 0);
        if (TextUtils.isEmpty(mId)) {
            throw new IllegalArgumentException("Null id given.");
        }
        if (manager == null) {
            throw new IllegalArgumentException("Null manager given.");
        }
        loadData(manager.getSharedPreferences());
        onCreated();
    }

    @Override
    public CharSequence getName(Resources res) {
        return mNameResId == 0 ? mId : res.getString(mNameResId);
    }

    public String getDescription(Resources res) {
        return mDescrResId == 0 ? "": res.getString(mDescrResId);
    }

    public String getRewardDescription(Resources res) {
        return mRewardResId == 0 ? ("+" + getScoreReward()) : res.getString(mRewardResId, getScoreReward());
    }

    @Override
    public int getValue() {
        return mValue;
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

    protected abstract void onInit();

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

    private void loadData(SharedPreferences prefs) {
        mDiscovered = prefs.getBoolean(mId + SEPARATOR + KEY_DISCOVERED, DEFAULT_IS_DISCOVERED);
        mValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_VALUE);
        mMaxValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_MAX_VALUE);
    }
}
