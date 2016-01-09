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

import android.support.annotation.NonNull;
import android.util.Log;

import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementDataEventListener;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 18.05.15.
 */
public class AchievementDataRiddleType extends AchievementProperties {
    private static final String DATA_NAME_PREFIX = "riddletype";
    private static final String KEY_NEW_GAMES_STARTED = "new_games_started";
    public static final String KEY_GAMES_SOLVED = "games_solved";
    public static final String KEY_BEST_SOLVED_TIME = "best_solved_time";
    private static final AchievementDataRiddleType sNull = new NullObject();
    public static final String KEY_BONUS_GAINED_COUNT = "bonus_gained";

    public AchievementDataRiddleType(PracticalRiddleType type, AchievementManager manager) throws CompactedDataCorruptException {
        super(DATA_NAME_PREFIX + type.getFullName(), manager.loadDataEvent(DATA_NAME_PREFIX + type.getFullName()));
        manager.manageAchievementData(this);
    }

    public AchievementDataRiddleType(PracticalRiddleType type, AchievementManager manager, boolean notUsed) {
        super(DATA_NAME_PREFIX + type.getFullName());
        manager.manageAchievementData(this);
    }

    private AchievementDataRiddleType(String name) {
        super(name);
    }

    public static @NonNull AchievementDataRiddleType getNullObject() {
        return sNull;
    }

    // Nullobject that does nothing meaningful but implements the full interface
    private static class NullObject extends AchievementDataRiddleType {

        private static final String NULL_DATA_NAME = "riddletypedata_null";

        public NullObject() {
            super(NULL_DATA_NAME);
        }

        public boolean removeListener(AchievementDataEventListener listener) {return false;}
        public void addListener(AchievementDataEventListener listener) {}
        public void notifyListeners(AchievementDataEvent event) {}
        public void enableSilentChanges(int eventType) {}
        public void disableSilentChanges() {}

        protected synchronized void resetData() {}
        public synchronized void putValue(String key, Long value, long requiredValueToOldDelta) {}
        public synchronized Long increment(String key, long delta, long baseValue) {return baseValue;}
    }

    public void onNewGame() {
        Long newValue = increment(KEY_NEW_GAMES_STARTED, 1, 0);
        Log.d("Achievement", "OnNewGame: " + mName + ", count=" + newValue);
    }

    public void onSolvedGame() {
        Long newValue = increment(KEY_GAMES_SOLVED, 1, 0);
        Log.d("Achievement", "onSolvedGame: " + mName + ", count=" + newValue);
    }

    public boolean isCustom() {
        return getValue(AchievementDataRiddleGame.KEY_CUSTOM, 0L) == 1L;
    }
}
