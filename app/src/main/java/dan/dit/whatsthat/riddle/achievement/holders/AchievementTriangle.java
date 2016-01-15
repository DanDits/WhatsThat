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

import android.content.res.Resources;
import android.util.Log;

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementDataTimer;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleTriangle;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.Types;

/**
 * Created by daniel on 23.07.15.
 */
public class AchievementTriangle extends TypeAchievementHolder {
    public static final String KEY_TRIANGLE_COUNT = "triangle_count";
    public static final String KEY_TRIANGLE_DIVIDED_BY_CLICK = "divided_by_click";
    public static final String KEY_TRIANGLE_DIVIDED_BY_MOVE = "divided_by_move";
    public static final String KEY_TRIANGLE_COUNT_BEFORE_LAST_INTERACTION = "triangle_count_before";

    public AchievementTriangle(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager, mType));
        mAchievements.put(Achievement2.NUMBER, new Achievement2(manager, mType));
        mAchievements.put(Achievement3.NUMBER, new Achievement3(manager, mType));
        mAchievements.put(Achievement4.NUMBER, new Achievement4(manager, mType));
        mAchievements.put(Achievement5.NUMBER, new Achievement5(manager, mType));
        mAchievements.put(Achievement6.NUMBER, new Achievement6(manager, mType));
    }

    private static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 75;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_GAMES_COUNT = 5;
        private static final String KEY_TIMER_SOLVED = Types.Triangle.NAME + NUMBER + "games_solved";
        private static final long MAX_TIME_DURATION = 180000L;

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_1_name, R.string.achievement_triangle_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (!isAchieved() || isRewardClaimable()) {
                return super.getDescription(res);
            }
            return res.getString(R.string.achievement_triangle_1_descr_unachieved, REQUIRED_GAMES_COUNT);
        }
        @Override
        public void onInit() {
            super.onInit();
            mTimerData.ensureTimeKeeper(KEY_TIMER_SOLVED, REQUIRED_GAMES_COUNT);
            mTimerData.addListener(this);
        }
        @Override
        protected void onAchieved() {
            super.onAchieved();
            mTimerData.removeTimerKeeper(KEY_TIMER_SOLVED);
            mTimerData.removeListener(this);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                if (areDependenciesFulfilled()) {
                    // game data closed, game is solved, mark this event
                    mTimerData.onTimeKeeperUpdate(KEY_TIMER_SOLVED, mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L));
                }
            }
            if (event.getChangedData() == mTimerData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_TIMER_SOLVED)) {
                AchievementDataTimer.TimeKeeper keeper = mTimerData.getTimeKeeper(KEY_TIMER_SOLVED);
                if (keeper != null && keeper.getTimesCount() == REQUIRED_GAMES_COUNT) {
                    long duration = keeper.getTotalTimeConsumed();
                    if (duration > 0L && duration <= MAX_TIME_DURATION) {
                        achieve();
                    }
                }
            }
        }
    }

    private static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int TRIANGLES_TO_DIVIDE = Math.min(8, RiddleTriangle.MAX_SPLIT_PER_CLICK);
        private static final int MAX_TRIANGLES_THRESHOLD = 20;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_2_name, R.string.achievement_triangle_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (!isAchieved() || isRewardClaimable()) {
                return super.getDescription(res);
            }
            return res.getString(R.string.achievement_triangle_2_descr_unachieved);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
           if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_TRIANGLE_COUNT)) {
               long totalTriangles = mGameData.getValue(KEY_TRIANGLE_COUNT, 0L);
               long changedAmount = totalTriangles - mGameData.getValue(KEY_TRIANGLE_COUNT_BEFORE_LAST_INTERACTION, 0L);
               if (changedAmount >= TRIANGLES_TO_DIVIDE && totalTriangles <= MAX_TRIANGLES_THRESHOLD) {
                   achieveAfterDependencyCheck();
               }
           }
        }
    }

    public static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 80;
        public static final boolean DISCOVERED = true;
        private static final int MAX_TRIANGLES = 100;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_3_name, R.string.achievement_triangle_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (!isAchieved() || isRewardClaimable()) {
                return super.getDescription(res);
            }
            return res.getString(R.string.achievement_triangle_3_descr_unachieved);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData  && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                if (mGameData.getValue(KEY_TRIANGLE_COUNT, MAX_TRIANGLES + 1) <= MAX_TRIANGLES) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 85;
        public static final boolean DISCOVERED = true;
        private static final int MIN_TRIANGLES = 8000;

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_4_name, R.string.achievement_triangle_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (!isAchieved() || isRewardClaimable()) {
                return super.getDescription(res);
            }
            return res.getString(R.string.achievement_triangle_4_descr_unachieved);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData  && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                if (mGameData.getValue(KEY_TRIANGLE_COUNT, MIN_TRIANGLES - 1) >= MIN_TRIANGLES) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 200;
        public static final boolean DISCOVERED = true;
        private static final int GAMES_SOLVE_COUNT = 25;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_5_name, R.string.achievement_triangle_5_descr, 0, NUMBER, manager, LEVEL, REWARD, GAMES_SOLVE_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (!isAchieved() || isRewardClaimable()) {
                return super.getDescription(res);
            }
            return res.getString(R.string.achievement_triangle_5_descr_unachieved);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData  && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED
                    && areDependenciesFulfilled()) {
                achieveDelta(1);
            }
        }
    }


    private static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 2; //displayed as 3
        public static final int REWARD = 333;
        public static final boolean DISCOVERED = true;
        private static final int ILLUMINATI = 3;
        private static final String KEY_TIMER_SOLVED = PracticalRiddleType.TRIANGLE_INSTANCE
                .getFullName() + NUMBER + "_solved_experiments";
        private static final long SOLVED_MAX_TIME = 3 * 33L * 1000L; //ms

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_triangle_6_name, R.string
                    .achievement_triangle_6_descr, 0, NUMBER, manager, LEVEL, REWARD,
                    1, DISCOVERED);
        }

        @Override
        public void onInit() {
            super.onInit();
            mTimerData.ensureTimeKeeper(KEY_TIMER_SOLVED, ILLUMINATI);
            mTimerData.addListener(this);
        }

        @Override
        public int getIconResIdByState() {
            if (isAchieved()) {
                return R.drawable.illuminati_eye;
            }
            return super.getIconResIdByState();
        }

        @Override
        protected void onAchieved() {
            super.onAchieved();
            mTimerData.removeTimerKeeper(KEY_TIMER_SOLVED);
            mTimerData.removeListener(this);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData  && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED
                    && areDependenciesFulfilled()) {
                mTimerData.onTimeKeeperUpdate(KEY_TIMER_SOLVED, mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L));
            }

            if (event.getChangedData() == mTimerData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_TIMER_SOLVED)) {
                AchievementDataTimer.TimeKeeper keeper = mTimerData.getTimeKeeper(KEY_TIMER_SOLVED);
                if (keeper != null && keeper.getTimesCount() == ILLUMINATI) {
                    long duration = keeper.sumDurations(); // breaks allowed
                    if (duration > 0L && duration <= SOLVED_MAX_TIME) {
                        confirmIlluminati();
                    }
                }
            }
        }

        private void confirmIlluminati() {
            achieve();
        }
    }
}
