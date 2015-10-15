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

import dan.dit.whatsthat.achievement.AchievementDataTimer;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 20.05.15.
 */
public class AchievementDataTimerRiddle extends AchievementDataTimer {
    private static final String DATA_NAME = "riddletimer";
    private static volatile AchievementDataTimerRiddle INSTANCE;

    private AchievementDataTimerRiddle(AchievementManager manager) throws CompactedDataCorruptException {
        super(DATA_NAME, manager.loadDataEvent(DATA_NAME));
        manager.manageAchievementData(this);
    }

    private AchievementDataTimerRiddle(AchievementManager manager, boolean notUsed) {
        super(DATA_NAME);
        manager.manageAchievementData(this);
    }

    public static AchievementDataTimerRiddle getInstance(AchievementManager manager) {
        if (INSTANCE == null) {
            synchronized (AchievementDataTimerRiddle.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new AchievementDataTimerRiddle(manager);
                    } catch (CompactedDataCorruptException e) {
                        Log.e("Achievement", "Failed recreating data timer: " + e);
                        INSTANCE = new AchievementDataTimerRiddle(manager, true);
                    }
                }
            }
        }
        return INSTANCE;
    }

}
