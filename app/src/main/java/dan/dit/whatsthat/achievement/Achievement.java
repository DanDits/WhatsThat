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

package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.dependencies.Dependable;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.util.general.PercentProgressListener;

/**
 * Base of all achievements. An achievement has a name, a description and an icon and is
 * managed by an AchievementManager, which is responsible for saving and loading the current state
 * to and from permanent memory.
 * An achievement can be discovered or undiscovered which only has effect on the display of its description
 * (or others). An achievement starts with a value of zero and is achieved as soon as the value is equal to or greater
 * than the set max value. An achievement will get discovered before being achieved.
 * Each achievement has a reward associated which is any score value greater than or equal to zero. The reward
 * needs to be claimed after the achievement is achieved.
 *
 * Finally an achievement is Dependable and can therefore be a Dependency, e.g. for other achievements. An
 * achievement should only be achieved if the dependencies are fulfilled, though this is up to the achievement
 * as the time to check can vary for example for progressive achievements.
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener, Dependable {
    protected static final String SEPARATOR = "_";
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_VALUE = "value";
    private static final String KEY_ACHIEVED_TIMESTAMP = "achievedtime";
    private static final String KEY_REWARD_CLAIMED = "rewardclaimed";
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 1;

    protected final String mId;
    protected boolean mDiscovered;
    protected final AchievementManager mManager;
    private int mValue;
    private int mMaxValue;
    protected final int mLevel;
    private final int mScoreReward;
    private boolean mRewardClaimed;
    private final int mNameResId;
    protected final int mDescrResId;
    private final int mRewardResId;
    protected final List<Dependency> mDependencies;
    protected long mAchievedTimestamp;

    /**
     * Creates a new achievement. The id string needs to be unique for all achievements. The given name, description
     * and reward are by default read from the given resources.
     * @param id The unique id identifying the achievement.
     * @param nameResId The name.
     * @param descrResId The description.
     * @param rewardResId The reward description, if 0 it displays a default message.
     * @param manager The achievement manager required.
     * @param level The level of the achievement. Can be used to create an additional dependency or changing the displaying.
     * @param scoreReward The score reward that is to be claimed after achievement is achieved.
     * @param maxValue The maximum value to reach to achieve the achievement. Must be positive.
     * @param discovered If the achievement is discovered by default. No effect if achievement was already saved.
     */
    protected Achievement(String id, int nameResId, int descrResId, int rewardResId, AchievementManager manager, int level, int scoreReward, int maxValue, boolean discovered) {
        mId = id;
        mValue = DEFAULT_VALUE;
        mDiscovered = discovered;
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
        loadData(manager.getSharedPreferences());
    }


    @Override
    public int getValue() {
        return mValue;
    }

    /**
     * Returns the icon resource id. This is the default icon for the achievement used in general
     * places like when it is achieved or shown in some generic place.
     * @return The icon resource id.
     */
    public abstract int getIconResId();

    /**
     * Returns the icon resource id depending on the state of the achievement. (like discovered,
     * achieved, claimed,...)
     * @return The icon resource id dependending on current state.
     */
    public abstract int getIconResIdByState();

    /**
     * Returns this achievement's name. This is the id if no name resource given
     * or else the string read of the resources.
     * @param res Resources used to retrieve the string.
     * @return A name for the achievement.
     */
    public CharSequence getName(Resources res) {
        return mNameResId == 0 || res == null ? mId : res.getString(mNameResId);
    }

    /**
     * Returns this achievement's description. This is empty if no description resource
     * given or else the string read of the resources.
     * @param res Resources used to retrieve the string.
     * @return A description of the achievement.
     */
    public CharSequence getDescription(Resources res) {
        return mDescrResId == 0 || res == null ? "": res.getString(mDescrResId);
    }

    /**
     * Returns this achievement's reward description. This is "(+score reward)" for the set
     * score reward if no reward resource given or else the string read of the resources parameterized by the
     * score reward.
     * @param res Resources used to retrieve the string.
     * @return A description of the achievement's reward.
     */
    public CharSequence getRewardDescription(Resources res) {
        return mRewardResId == 0 ? ("+" + getScoreReward()) : res.getString(mRewardResId, getScoreReward());
    }

    /**
     * Checks if all dependencies are fulfilled. If any dependency is not fulfilled
     * this immediately returns false.
     * @return If all dependencies are fulfilled.
     */
    public boolean areDependenciesFulfilled() {
        for (int i = 0; i < mDependencies.size(); i++) {
            if (!mDependencies.get(i).isFulfilled())
                return false;
        }
        return true;
    }

    /**
     * Achieves this achievement if all dependencies are fulfilled. Does nothing
     * if already achieved.
     * @return true only if the dependencies are fulfilled.
     */
    protected  boolean achieveAfterDependencyCheck() {
        if (areDependenciesFulfilled()) {
            achieve();
            return true;
        }
        return false;
    }

    /**
     * Resets any progress of this achievement. This also works if the achievement is already
     * achieved, even if is already claimed. Will notify the manager of this change accordingly.
     * The value will be equal to the default value afterwards.
     */
    protected void resetAnyProgress() {
        boolean unclaimed = isRewardClaimable();
        Log.d("Achievement", "Resetting progress " + mId + " was unclaimed: " + unclaimed);
        mValue = DEFAULT_VALUE;
        mRewardClaimed = false;
        if (unclaimed) {
            // if not claimed do not grant the score if user didn't claim it...
            mManager.onChanged(this, AchievementManager.CHANGED_FROM_UNCLAIMED_TO_RESET);
        } else {
            mManager.onChanged(this, AchievementManager.CHANGED_TO_RESET);
        }
    }

    /**
     * Achieves a percentage of the set max value. This has no effect if achievement
     * already achieved. Will achieve the achievement for progress equal to 100.
     * Does NO dependency checks.
     * @param progress A percent value which is cut between 0 to 100 inclusive.
     */
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
        mValue = Math.round(progress * mMaxValue / 100.f);
        if (mValue >= mMaxValue || progress == 100) {
            achieveUnchecked();
        } else if (mValue != oldValue) {
            mManager.onChanged(this, AchievementManager.CHANGED_PROGRESS);
        }
    }

    /**
     * Achieves the given delta which can be any value. Does nothing if already achieved.
     * Does NO dependency check.
     * @param delta The delta to achieve.
     */
    protected void achieveDelta(int delta) {
        if (isAchieved()) {
            return;
        }
        mValue += delta;
        if (mValue >= mMaxValue) {
            achieveUnchecked();
        } else if (delta != 0) {
            mManager.onChanged(this, AchievementManager.CHANGED_PROGRESS);
        }
    }

    /**
     * Achieves the given delta if this does not achieve this achievement. Does nothing
     * if already achieved. Does NO dependency check.
     * @param delta The delta to add. Can be any value.
     * @return True if the given delta was achieved, this is when the achievement was not yet
     * achieved and the delta added on the current value is lower than the maximum value.
     */
    protected boolean addDeltaIfNotAchieved(int delta) {
        if (isAchieved() || mValue + delta >= mMaxValue) {
            return false;
        }
        achieveDelta(delta);
        return true;
    }

    /**
     * Returns the set level.
     * @return The level.
     */
    public int getLevel() {
        return mLevel;
    }

    /**
     * Returns the set score reward.
     * @return The score reward.
     */
    protected int getScoreReward() {
        return mScoreReward;
    }

    /**
     * The maximum possible score reward. By default equal to getScoreReward().
     * @return The maximum possible reward.
     */
    public int getMaxScoreReward() {
        return getScoreReward();
    }

    /**
     * Checks if the reward is claimable. This is when the achievement is
     * achieved and the reward was not yet claimed.
     * @return If this reward is claimable.
     */
    public boolean isRewardClaimable() {
        return !mRewardClaimed && isAchieved();
    }

    /**
     * Claims the reward. Does nothing if reward not claimable.
     */
    public void claimReward() {
        if (isRewardClaimable()) {
            mRewardClaimed = true;
            mManager.onChanged(this, AchievementManager.CHANGED_GOT_CLAIMED);
        }
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

    /**
     * Discovers the achievement if not yet discovered. This will invoke
     * onDiscovered() before notifying the manager.
     */
    protected synchronized void discover() {
        if (mDiscovered) {
            return; // already discovered
        }
        mDiscovered = true;
        onDiscovered();
        mManager.onChanged(this, AchievementManager.CHANGED_TO_DISCOVERED);
    }

    /**
     * Covers the achievement if not yet covered.
     */
    protected synchronized final void cover() {
        if (!mDiscovered) {
            return;
        }
        mDiscovered = false;
        mManager.onChanged(this, AchievementManager.CHANGED_TO_COVERED);
    }

    /**
     * Invoked when the achievement is discovered after previously being covered.
     */
    protected abstract void onDiscovered();

    /**
     * Checks if this achievement is achieved.
     * @return If the achievement is achieved.
     */
    public boolean isAchieved() {
        return mValue >= mMaxValue;
    }

    /**
     * No checks at all.
     * Achieves this achievement. If the achievement previously is not discovered
     * it will be discovered before being achieved.
     */
    private void achieveUnchecked() {
        if (!mDiscovered) {
            discover();
        }
        mValue = mMaxValue;
        mAchievedTimestamp = System.currentTimeMillis();
        onAchieved();
        mManager.onChanged(this, AchievementManager.CHANGED_TO_ACHIEVED_AND_UNCLAIMED);
    }

    /**
     * Achieve this achievement. Does nothing if already achieved. Does NO dependency check.
     */
    protected synchronized final void achieve() {
        if (isAchieved()) {
            return;
        }
        achieveUnchecked();
    }

    /**
     * Invoked when the achievement is being achieved.
     */
    protected abstract void onAchieved();

    /**
     * Adds this data that is required to be stored permanently.
     * @param editor The editor to put data into.
     */
    protected void addData(SharedPreferences.Editor editor) {
        editor
                .putBoolean(mId + SEPARATOR + KEY_DISCOVERED, mDiscovered)
                .putInt(mId + SEPARATOR + KEY_VALUE, mValue)
                .putLong(mId + SEPARATOR + KEY_ACHIEVED_TIMESTAMP, mAchievedTimestamp)
                .putBoolean(mId + SEPARATOR + KEY_REWARD_CLAIMED, mRewardClaimed);

    }

    /**
     * Loads data out of the given shared preferences.
     * @param prefs The preferences to load data from.
     */
    protected void loadData(SharedPreferences prefs) {
        mDiscovered = prefs.getBoolean(mId + SEPARATOR + KEY_DISCOVERED, mDiscovered);
        mValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, mValue);
        mAchievedTimestamp = prefs.getLong(mId + SEPARATOR + KEY_ACHIEVED_TIMESTAMP, 0L);
        mRewardClaimed = prefs.getBoolean(mId + SEPARATOR + KEY_REWARD_CLAIMED, false);
        //Log.d("Achievement", "Loaded achievement data : " + mDiscovered + " " + mValue + " " + mMaxValue + " " + mAchievedTimestamp + " " + mRewardClaimed);
    }

    /**
     * Checks if this achievement is discovered.
     * @return If the achievement is discovered.
     */
    public final boolean isDiscovered() {
        return mDiscovered;
    }

    /**
     * Returns the progress in percent. A progress of 100 is equal to the achievement being achieved.
     * @return The progress.
     */
    public final int getProgress() {
        return (int) (PercentProgressListener.PROGRESS_COMPLETE * mValue / (double) mMaxValue);
    }

    /**
     * Returns the set maximum value.
     * @return The max value.
     */
    public final int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Returns the dependencies of this achievement. List is backed by the achievement.
     * @return All dependencies of this achievement.
     */
    public List<Dependency> getDependencies() {
        return mDependencies;
    }

    /**
     * Builds the text for this achievement's dependencies. By default this is the names of all
     * not fulfilled dependencies appended separated by commata.
     * @param res The resources used to load the dependencies' name.
     * @return The text describing all not fulfilled achievements.
     */
    public CharSequence buildDependenciesText(Resources res) {
        StringBuilder builder = new StringBuilder();
        builder.append(res.getString(R.string.dependency_required));
        builder.append(' ');
        boolean addSeparator = false;
        for (int i = 0; i < mDependencies.size(); i++) {
            Dependency dep = mDependencies.get(i);
            if (!dep.isFulfilled()) {
                if (addSeparator) {
                    builder.append(", ");
                }
                builder.append(mDependencies.get(i).getName(res));
                addSeparator = true;
            }
        }
        return builder.toString();
    }

    /**
     * Returns the unique id of this Achievement.
     * @return The unique id for managing this achievement.
     */
    public String getId() {
        return mId;
    }
}
