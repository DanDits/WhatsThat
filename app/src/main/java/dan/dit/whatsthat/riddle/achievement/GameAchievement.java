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

package dan.dit.whatsthat.riddle.achievement;

import android.util.Log;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.LevelDependency;

/**
 * Created by daniel on 22.05.15.
 */
public abstract class GameAchievement extends Achievement {
    protected final AchievementDataRiddleType mTypeData;
    protected final AchievementDataRiddleGame mGameData;
    protected final AchievementDataTimerRiddle mTimerData;
    public static final String KEY_DATA_IS_OF_CUSTOM_GAME = "custom";

    private boolean mInitialized;
    private int mIconResId;

    protected GameAchievement(PracticalRiddleType type, int nameResId, int descrResId, int rewardResId, int number, AchievementManager manager, int level, int scoreReward, int maxValue, boolean discovered) {
        super(makeId(type, number), nameResId, descrResId, rewardResId, manager, level, scoreReward, maxValue, discovered);
        mIconResId = type.getIconResId();
        if (isAchieved()) {
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
        mDependencies.add(LevelDependency.getInstance(mLevel));
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

    @Override
    public void onDataEvent(AchievementDataEvent event) {
        if ((event.getChangedData() == mGameData && mGameData.isCustom())
            || (event.getChangedData() == mTypeData && mTypeData.isCustom())) {
            return;
        }
        onNonCustomDataEvent(event);
    }

    protected abstract void onNonCustomDataEvent(AchievementDataEvent event);


    private static String makeId(PracticalRiddleType type, int number) {
        return type.getFullName() + number;
    }

}
