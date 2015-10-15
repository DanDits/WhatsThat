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

import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 13.05.15.
 */
public class AchievementDataRiddleGame extends AchievementProperties {
    private static final int STATE_NONE = 0;
    private static final int STATE_OPENING= 1;
    public static final int STATE_OPENED = 2;
    public static final int STATE_CLOSED = 3;

    private static final String DATA_NAME = "riddlegame";
    private static final String KEY_START_TIME = "start_time";
    public static final String KEY_PLAYED_TIME = "played_time";
    public static final String KEY_LAST_OPENED = "last_opened";
    public static final String KEY_SOLVED = "solved";
    public static final String KEY_CUSTOM = "custom";

    private int mState = STATE_NONE;

    public AchievementDataRiddleGame(PracticalRiddleType type) {
        super(DATA_NAME + type.getFullName());
    }

    public synchronized void loadGame(Compacter achievementData) {
        if (mState == STATE_OPENED || mState == STATE_OPENING) {
            Log.e("Achievement", "Trying to open already opened AchievementDataRiddleGame " + mName);
            return;
        }
        mState = STATE_OPENING;
        enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_CREATE);
        resetData();
        boolean initNew = true;
        if (achievementData != null) {
            try {
                unloadData(achievementData);
                initNew = false;
            } catch (CompactedDataCorruptException e) {
                initNew = true;
            }
        }
        if (initNew) {
            newGame();
        } else {
            openGame();
        }
        disableSilentChanges();
    }

    private void newGame() {
        putValue(KEY_START_TIME, System.currentTimeMillis(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        openGame();
    }

    private boolean isDecliningInput() {
        return mState != STATE_OPENING && mState != STATE_OPENED;
    }

    @Override
    public synchronized Long increment(String key, long delta, long baseValue) {
        if (isDecliningInput()) {
            return baseValue;
        }
        return super.increment(key, delta, baseValue);
    }

    @Override
    public synchronized void putValue(String key, Long value, long requiredValueToOldDelta) {
        if (isDecliningInput()) {
            return;
        }
        super.putValue(key, value, requiredValueToOldDelta);
    }

    public synchronized void putValues(String key1, Long value1, long reqDelta1, String key2, Long value2, long reqDelta2, String key3, Long value3, long reqDelta3) {
        if (isDecliningInput()) {
            return;
        }
        boolean hadSilentChanges = isSilentChangeMode();
        if (!hadSilentChanges) {
            enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
        }
        putValue(key1, value1, reqDelta1);
        putValue(key2, value2, reqDelta2);
        putValue(key3, value3, reqDelta3);
        if (!hadSilentChanges) {
            disableSilentChanges();
        }
    }


    private void openGame() {
        putValue(KEY_LAST_OPENED, System.currentTimeMillis(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        mState = STATE_OPENED;
    }

    public int getState() {
        return mState;
    }

    public synchronized void closeGame(long solved) {
        if (mState == STATE_CLOSED || mState == STATE_NONE) {
            Log.e("Achievement", "Trying to close already closed or not yet opened AchievementDataRiddleGame " + mName);
            return;
        }
        enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_CLOSE);
        long currentTime = System.currentTimeMillis();
        long playedTime = increment(KEY_PLAYED_TIME, currentTime - getValue(KEY_LAST_OPENED, getValue(KEY_START_TIME, 0L)), 0L);
        Log.d("Achievement", "Played time after closing game: " + playedTime);
        putValue(KEY_SOLVED, solved, AchievementProperties.UPDATE_POLICY_ALWAYS);
        mState = STATE_CLOSED;
        disableSilentChanges();

        if (isSolved() && TestSubject.isInitialized()) {
            long totalTime = currentTime - getValue(KEY_START_TIME, 0L);
            AchievementPropertiesMapped<String> data = TestSubject.getInstance().getAchievementHolder().getMiscData();
            data.enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
            data.putValue(MiscAchievementHolder.KEY_LAST_SOLVED_GAME_PLAYED_TIME, playedTime, UPDATE_POLICY_ALWAYS);
            data.putValue(MiscAchievementHolder.KEY_LAST_SOLVED_GAME_TOTAL_TIME, totalTime, UPDATE_POLICY_ALWAYS);
            data.disableSilentChanges();
        }
    }

    public boolean isSolved() {
        return getValue(AchievementDataRiddleGame.KEY_SOLVED, Solution.SOLVED_NOTHING) == Solution.SOLVED_COMPLETELY;
    }

    public boolean isCustom() {
        return getValue(AchievementDataRiddleGame.KEY_CUSTOM, 0L) == 1L;
    }
}
