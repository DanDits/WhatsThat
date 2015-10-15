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
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleLazor;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 02.10.15.
 */
public class AchievementLazor extends TypeAchievementHolder {
    public static final String KEY_GAME_IS_PROTECTED = "city_is_protected";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_LAZOR_CANNON_FLY_DURATION = "last_lazor_cannon_fly_duration";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT = "last_lazor_meteor_destroyed_count";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_EXPAND_TIME = "last_meteor_cannonball_expand_time";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_COLOR_TYPE = "last_meteor_color_type";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_Y_PERCENT = "last_meteor_destroyed_y_percent";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_Y_PERCENT = "last_meteor_destroyed_cannonball_y_percent";
    public static final String KEY_GAME_METEOR_DESTROYED_COUNT = "meteor_destroyed_count";
    public static final String KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT = "meteor_crashed_in_city_count";
    public static final String KEY_GAME_METEOR_CRASHED_NOT_CITY_COUNT = "meteor_crashed_not_city_count";
    public static final String KEY_GAME_LAZOR_CANNON_COLLIDED_EARLY = "lazor_cannon_collided_early";
    public static final String KEY_GAME_CANNON_BALL_EXPLOSION_END_COUNT = "cannon_ball_explosion_end_count";
    public static final String KEY_GAME_CANNON_BALL_ENDED_DESTROY_COUNT = "cannon_ball_end_destroy_count";
    public static final String KEY_GAME_CANNON_BALL_ENDED_CANNON_ID = "cannon_ball_cannon_id";
    public static final String KEY_GAME_DIFFICULTY = "curr_difficulty";

    public AchievementLazor(PracticalRiddleType type) {
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
        mAchievements.put(Achievement7.NUMBER, new Achievement7(manager, mType));
        mAchievements.put(Achievement8.NUMBER, new Achievement8(manager, mType));
        mAchievements.put(Achievement9.NUMBER, new Achievement9(manager, mType));
        mAchievements.put(Achievement10.NUMBER, new Achievement10(manager, mType));
        mAchievements.put(Achievement11.NUMBER, new Achievement11(manager, mType));
        mAchievements.put(Achievement12.NUMBER, new Achievement12(manager, mType));
        mAchievements.put(Achievement13.NUMBER, new Achievement13(manager, mType));
        mAchievements.put(Achievement14.NUMBER, new Achievement14(manager, mType));
        mAchievements.put(Achievement15.NUMBER, new Achievement15(manager, mType));
        mAchievements.put(Achievement16.NUMBER, new Achievement16(manager, mType));
        mAchievements.put(Achievement17.NUMBER, new Achievement17(manager, mType));
    }

    private static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_HITS = 3; // not synced with strings

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_1_name, R.string.achievement_lazor_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT, 0L) >= REQUIRED_HITS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    public static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final String KEY_GAME_METEOR_CRASHED_IN_UNPROTECTED_CITY = NUMBER + "crashed_in_unprotected_city";

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_2_name, R.string.achievement_lazor_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_METEOR_CRASHED_IN_UNPROTECTED_CITY);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT) || event.hasChangedKey(KEY_GAME_METEOR_CRASHED_NOT_CITY_COUNT))) {
                if (mGameData.getValue(KEY_GAME_IS_PROTECTED, 0L) == 0L) {
                    mGameData.increment(KEY_GAME_METEOR_CRASHED_IN_UNPROTECTED_CITY, 1L, 0L);
                }
            }
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_IS_PROTECTED)
                    && mGameData.getValue(KEY_GAME_METEOR_CRASHED_IN_UNPROTECTED_CITY, 0L) == 0L) {
                achieveAfterDependencyCheck();
            }
        }
    }

    private static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int MAX_REQUIRED_DIFFICULTY = 80;
        private static final int MIN_REQUIRED_DIFFICULTY = 30;
        private static final String KEY_GAME_REACHED_MAX_REQ_DIFFICULTY = NUMBER + "_reached_required";

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_3_name, R.string.achievement_lazor_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAX_REQUIRED_DIFFICULTY, MIN_REQUIRED_DIFFICULTY);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_DIFFICULTY)) {
                long diff = mGameData.getValue(KEY_GAME_DIFFICULTY, 0L);
                if (diff >= MAX_REQUIRED_DIFFICULTY) {
                    mGameData.putValue(KEY_GAME_REACHED_MAX_REQ_DIFFICULTY, diff == MAX_REQUIRED_DIFFICULTY ? 1L : 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                } else if (diff <= MIN_REQUIRED_DIFFICULTY && mGameData.getValue(KEY_GAME_REACHED_MAX_REQ_DIFFICULTY, 0L) == 1L) {
                    if (diff == MIN_REQUIRED_DIFFICULTY) {
                        achieveAfterDependencyCheck();
                    } else {
                        mGameData.putValue(KEY_GAME_REACHED_MAX_REQ_DIFFICULTY, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    }
                }
            }
        }
    }

    private static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = false;
        private static final String KEY_GAME_UNPROTECTED_CITY_CRASHES = NUMBER + "unprotected_city_crash";
        private static final long REQUIRED_CRASHES_IN_UNPROTECTED_CITY = 40;

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_4_name, R.string.achievement_lazor_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_UNPROTECTED_CITY_CRASHES);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT)) {
                if (mGameData.getValue(KEY_GAME_IS_PROTECTED, 0L) == 0L) {
                    long crashCount = mGameData.increment(KEY_GAME_UNPROTECTED_CITY_CRASHES, 1L, 0L);
                    if (crashCount >= REQUIRED_CRASHES_IN_UNPROTECTED_CITY) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }
    }

    private static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int METEORS_TO_SHOOT = 50;
        private static final int MAX_METEOR_Y_PERCENT = 33; // not synced to description string!

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_5_name, R.string.achievement_lazor_5_descr, 0, NUMBER, manager, LEVEL, REWARD, METEORS_TO_SHOOT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, METEORS_TO_SHOOT);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_METEOR_DESTROYED_Y_PERCENT)) {
                if (areDependenciesFulfilled() &&
                        mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_Y_PERCENT, 100L) <= MAX_METEOR_Y_PERCENT) {
                    achieveDelta(1);
                }
            }
        }
    }

    private static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int MORE_THAN_THIS_SHOOTS = 5;
        private static final int EXACTLY_THIS_SHOOTS = 40;
        private static final int LESS_THAN_THIS_SHOOTS = 100;
        private static final String KEY_GAME_EXPLOSION_COUNTER = NUMBER + "counter";

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_6_name, R.string.achievement_lazor_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MORE_THAN_THIS_SHOOTS, LESS_THAN_THIS_SHOOTS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_EXPLOSION_COUNTER);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {

            if (event.getChangedData() == mGameData && (event.hasChangedKey(KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT)
                    || event.hasChangedKey(KEY_GAME_METEOR_CRASHED_NOT_CITY_COUNT))) {
                mGameData.putValue(KEY_GAME_EXPLOSION_COUNTER, -1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CANNON_BALL_EXPLOSION_END_COUNT)
                    && mGameData.getValue(KEY_GAME_EXPLOSION_COUNTER, 0L) >= 0L) {
                if (mGameData.getValue(KEY_GAME_CANNON_BALL_ENDED_DESTROY_COUNT, 0L) == 0L) {
                    mGameData.putValue(KEY_GAME_EXPLOSION_COUNTER, -1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                } else {
                    if (mGameData.increment(KEY_GAME_EXPLOSION_COUNTER, 1L, 0L) >= EXACTLY_THIS_SHOOTS) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }
    }


    public static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_MIN_GENERATOR_PERCENT = 70;
        private static final int REQUIRED_METEORS_TO_SPAWN_AFTER = 30;
        private static final String KEY_SPAWN_COUNTER = NUMBER + "spawn_counter";

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_7_name, R.string.achievement_lazor_7_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_MIN_GENERATOR_PERCENT, REQUIRED_METEORS_TO_SPAWN_AFTER);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_SPAWN_COUNTER);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_DIFFICULTY)) {
                if (mGameData.getValue(KEY_GAME_DIFFICULTY, 0L) >= REQUIRED_MIN_GENERATOR_PERCENT) {
                    mGameData.putValue(KEY_SPAWN_COUNTER, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                } else {
                    mGameData.putValue(KEY_SPAWN_COUNTER, -1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
            if (event.getChangedData() == mGameData
                    && mGameData.getValue(KEY_SPAWN_COUNTER, -1L) >= 0L
                    && (event.hasChangedKey(KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT) || event.hasChangedKey(KEY_GAME_METEOR_CRASHED_NOT_CITY_COUNT)
                        || event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT))) {
                if (mGameData.increment(KEY_SPAWN_COUNTER, 1L, 0L) >= REQUIRED_METEORS_TO_SPAWN_AFTER) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final Long PERCENT_DELTA = 3L;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_8_name, R.string.achievement_lazor_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_Y_PERCENT, 0L) >= PERCENT_DELTA + mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_Y_PERCENT, 0L)) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_SHOTS_WITH_EACH_CANNON = 75;
        private static final String KEY_GAME_LEFT_CANNON_SHOT_COUNT = NUMBER + "left_cannon_shot";
        private static final String KEY_GAME_RIGHT_CANNON_SHOT_COUNT = NUMBER + "right_cannon_shot";

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_9_name, R.string.achievement_lazor_9_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_SHOTS_WITH_EACH_CANNON);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_LEFT_CANNON_SHOT_COUNT);
            mGameData.removeKey(KEY_GAME_RIGHT_CANNON_SHOT_COUNT);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_IS_PROTECTED)) {
                if (mGameData.getValue(KEY_GAME_IS_PROTECTED, 0L) == 0L) {
                    //unprotected, clear shot count
                    mGameData.putValue(KEY_GAME_LEFT_CANNON_SHOT_COUNT, -1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    mGameData.putValue(KEY_GAME_RIGHT_CANNON_SHOT_COUNT, -1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                } else if (areDependenciesFulfilled()) {
                    // enable counter
                    mGameData.putValue(KEY_GAME_LEFT_CANNON_SHOT_COUNT, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    mGameData.putValue(KEY_GAME_RIGHT_CANNON_SHOT_COUNT, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
            if (event.getChangedData() == mGameData
                    && mGameData.getValue(KEY_GAME_LEFT_CANNON_SHOT_COUNT, -1L) >= 0L
                    && mGameData.getValue(KEY_GAME_RIGHT_CANNON_SHOT_COUNT, -1L) >= 0L
                    && event.hasChangedKey(KEY_GAME_CANNON_BALL_EXPLOSION_END_COUNT)) {
                if (mGameData.getValue(KEY_GAME_CANNON_BALL_ENDED_CANNON_ID, 0L).equals(RiddleLazor.LEFT_CANNON_ID)) {
                    mGameData.increment(KEY_GAME_LEFT_CANNON_SHOT_COUNT, 1L, 0L);
                } else if (mGameData.getValue(KEY_GAME_CANNON_BALL_ENDED_CANNON_ID, 0L).equals(RiddleLazor.RIGHT_CANNON_ID)) {
                    mGameData.increment(KEY_GAME_RIGHT_CANNON_SHOT_COUNT, 1L, 0L);
                }
                if (mGameData.getValue(KEY_GAME_LEFT_CANNON_SHOT_COUNT, 0L) >= REQUIRED_SHOTS_WITH_EACH_CANNON
                        && mGameData.getValue(KEY_GAME_RIGHT_CANNON_SHOT_COUNT, 0L) >= REQUIRED_SHOTS_WITH_EACH_CANNON) {
                    achieve();
                }
            }
        }
    }

    private static class Achievement10 extends GameAchievement {
        public static final int NUMBER = 10;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final String KEY_GAME_DESTROYED_RED_OR_WHITE_METEOR = NUMBER + "destroyed_red_or_white_meteor";

        public Achievement10(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_10_name, R.string.achievement_lazor_10_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_DESTROYED_RED_OR_WHITE_METEOR);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_COLOR_TYPE, 0L) == RiddleLazor.COLOR_TYPE_RED
                        || mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_COLOR_TYPE, 0L) == RiddleLazor.COLOR_TYPE_BONUS) {
                    mGameData.putValue(KEY_GAME_DESTROYED_RED_OR_WHITE_METEOR, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()
                    && mGameData.getValue(KEY_GAME_DESTROYED_RED_OR_WHITE_METEOR, 0L) == 0L) {
                achieveAfterDependencyCheck();
            }
        }
    }

    private static class Achievement11 extends GameAchievement {
        public static final int NUMBER = 11;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int METEORS_TO_HIT_COUNT = 8;
        private static final double LAST_SECOND_FACTOR = 0.97;
        private static final String KEY_GAME_METEOR_COUNT = NUMBER + "meteor_count";

        public Achievement11(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_11_name, R.string.achievement_lazor_11_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, METEORS_TO_HIT_COUNT);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_METEOR_COUNT);
        }
        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_IS_PROTECTED, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_EXPAND_TIME, 0L) >= LAST_SECOND_FACTOR * RiddleLazor.CANNONBALL_EXPLOSION_DURATION) {
                    if (mGameData.increment(KEY_GAME_METEOR_COUNT, 1L, 0L) >= METEORS_TO_HIT_COUNT) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }
    }

    private static class Achievement12 extends GameAchievement {
        public static final int NUMBER = 12;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_HITS = 4; // not synced with strings

        public Achievement12(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_12_name, R.string.achievement_lazor_12_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT, 0L) >= REQUIRED_HITS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement13 extends GameAchievement {
        public static final int NUMBER = 13;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int METEORS_TO_HIT_COUNT = 5;
        private static final String KEY_GAME_METEOR_COUNT = NUMBER + "meteor_count";
        private static final long LAST_SECOND_Y_PERCENT = 97;

        public Achievement13(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_13_name, R.string.achievement_lazor_13_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_METEOR_COUNT);
        }
        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_Y_PERCENT, 0L) >= LAST_SECOND_Y_PERCENT) {
                    if (mGameData.increment(KEY_GAME_METEOR_COUNT, 1L, 0L) >= METEORS_TO_HIT_COUNT) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }
    }

    private static class Achievement14 extends GameAchievement {
        public static final int NUMBER = 14;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_INTERRUPTED_SHOTS = 500;

        public Achievement14(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_14_name, R.string.achievement_lazor_14_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_INTERRUPTED_SHOTS, DISCOVERED);
        }

        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_INTERRUPTED_SHOTS);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAZOR_CANNON_COLLIDED_EARLY)) {
                if (areDependenciesFulfilled()) {
                    achieveDelta(1);
                }
            }
        }
    }


    private static class Achievement15 extends GameAchievement {
        public static final int NUMBER = 15;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_SHOTS = 300;
        private static final long MAX_FLY_DURATION = 1000; //ms

        public Achievement15(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_15_name, R.string.achievement_lazor_15_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_SHOTS, DISCOVERED);
        }

        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAX_FLY_DURATION / 1000);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT, 0L) == 1L) {
                    // only count one hit meteor per cannonball
                    if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_LAZOR_CANNON_FLY_DURATION, 0L) <= MAX_FLY_DURATION) {
                        if (areDependenciesFulfilled()) {
                            achieveDelta(1);
                        }
                    }
                }
            }
        }
    }

    private static class Achievement16 extends GameAchievement {
        public static final int NUMBER = 16;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_SHOTS = 100;
        private static final long MIN_FLY_DURATION = 4000L;//ms

        public Achievement16(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_16_name, R.string.achievement_lazor_16_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_SHOTS, DISCOVERED);
        }

        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MIN_FLY_DURATION / 1000);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_METEOR_DESTROYED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_CANNONBALL_DESTROY_COUNT, 0L) == 1L) {
                    // only count one hit meteor per cannonball
                    if (mGameData.getValue(KEY_GAME_LAST_METEOR_DESTROYED_LAZOR_CANNON_FLY_DURATION, 0L) >= MIN_FLY_DURATION) {
                        if (areDependenciesFulfilled()) {
                            achieveDelta(1);
                        }
                    }
                }
            }
        }
    }

    private static class Achievement17 extends GameAchievement {
        public static final int NUMBER = 17;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int DIFFICULTY_TO_REACH = 100;

        public Achievement17(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_lazor_17_name, R.string.achievement_lazor_17_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_DIFFICULTY)) {
                if (mGameData.getValue(KEY_GAME_DIFFICULTY, 0L) >= DIFFICULTY_TO_REACH) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }
}
