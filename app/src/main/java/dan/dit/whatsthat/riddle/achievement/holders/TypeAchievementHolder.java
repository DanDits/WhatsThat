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

package dan.dit.whatsthat.riddle.achievement.holders;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;


/**
 * Created by daniel on 15.06.15.
 */
public abstract class TypeAchievementHolder implements AchievementHolder {
    final PracticalRiddleType mType;
    Map<Integer, GameAchievement> mAchievements;

    TypeAchievementHolder(PracticalRiddleType type) {
        mType = type;
    }

    @Override
    public void addDependencies() {
        for (GameAchievement achievement : mAchievements.values()) {
            achievement.setDependencies();
        }
    }

    @Override
    public void initAchievements() {
        if (mAchievements == null) {
            Log.e("Achievement", "Trying to init achievements before creating them.");
            return;
        }
        for (GameAchievement achievement : mAchievements.values()) {
            achievement.init();
        }
    }

    public List<? extends Achievement> getAchievements() {
        if (mAchievements != null && !mAchievements.isEmpty()) {
            return new ArrayList<>(mAchievements.values());
        }
        return Collections.emptyList();
    }

    public GameAchievement getByNumber(int number) {
        return mAchievements.get(number);
    }

    @Override
    public int getExpectedTestSubjectScore(int testSubjectLevel) {
        int expected = 0;
        for (Achievement achievement : mAchievements.values()) {
            expected += achievement.getExpectedScore(testSubjectLevel);
        }
        return expected;
    }
}
