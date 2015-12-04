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

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleMemory;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 20.08.15.
 */
public class AchievementMemory extends TypeAchievementHolder {
    public static final String KEY_GAME_PATH_LENGTH = "path_length";
    public static final String KEY_GAME_STATE_RED_COUNT = "state_reds";
    public static final String KEY_GAME_STATE_YELLOW_COUNT = "state_yellow";
    public static final String KEY_GAME_STATE_GREEN_COUNT = "state_green";
    public static final String KEY_GAME_STATE_BLACK_COUNT = "state_black";
    public static final String KEY_GAME_UNCOVERED_PAIRS_COUNT = "uncovered_pairs";
    public static final String KEY_GAME_CARD_UNCOVERED_BY_CLICK_COUNT = "uncovered_by_click";
    public static final String KEY_GAME_UNCOVERED_PAIRS_IN_GREEN_STATE_COUNT = "uncovered_green_pairs";
    public static final String KEY_GAME_UNCOVERED_PAIRS_BY_PATH_COUNT = "uncovered_pairs_by_path";

    public AchievementMemory(PracticalRiddleType type) {
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
    }


    private static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 100;
        public static final boolean DISCOVERED = false;
        private static final long GLACIER_CLICK_PATIENCE = 225L; // clicks required till achieved or
        private static final long GLACIER_MELT_DURATION = 12L * 60L * 1000L; // x minutes in ms

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_1_name, R.string.achievement_memory_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CARD_UNCOVERED_BY_CLICK_COUNT)) {
                long currentPlayed = mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L)
                                        + System.currentTimeMillis() - mGameData.getValue(AchievementDataRiddleGame.KEY_LAST_OPENED, 0L);
                if (currentPlayed >= GLACIER_MELT_DURATION || mGameData.getValue(KEY_GAME_CARD_UNCOVERED_BY_CLICK_COUNT, 0L) >= GLACIER_CLICK_PATIENCE) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 120;
        public static final boolean DISCOVERED = true;
        private static final long REQUIRED_PATH_LENGTH = 11L;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_2_name, R.string.achievement_memory_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_PATH_LENGTH)) {
                if (mGameData.getValue(KEY_GAME_PATH_LENGTH, 0L) >= REQUIRED_PATH_LENGTH) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 95;
        public static final boolean DISCOVERED = true;
        private static final long MAX_UNCOVERED_PAIRS = 2L;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_3_name, R.string.achievement_memory_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_COUNT, 0L) <= MAX_UNCOVERED_PAIRS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 200;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_COVERED_FIELDS = 32;
        private static final String KEY_GAME_HAD_ENOUGH_COVERED_FIELDS = NUMBER + "_enough_covered_fields";

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_4_name, R.string.achievement_memory_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_COVERED_FIELDS);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_STATE_BLACK_COUNT) && mGameData.getValue(KEY_GAME_HAD_ENOUGH_COVERED_FIELDS, 0L) == 0L) {
                if (mGameData.getValue(KEY_GAME_STATE_BLACK_COUNT, 0L) >= REQUIRED_COVERED_FIELDS && areDependenciesFulfilled()) {
                    mGameData.putValue(KEY_GAME_HAD_ENOUGH_COVERED_FIELDS, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
            if (event.getChangedData() == mGameData && mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_COUNT, 0L) >= RiddleMemory.DEFAULT_FIELD_X * RiddleMemory.DEFAULT_FIELD_Y / 2) {
                achieveAfterDependencyCheck();
            }
        }
    }

    //Random trail
    private static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 150;
        public static final int REQUIRED_PAIRS = 4;
        public static final boolean DISCOVERED = true;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_5_name, R.string.achievement_memory_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_PAIRS);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_UNCOVERED_PAIRS_BY_PATH_COUNT)) {
                if (mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_BY_PATH_COUNT, 0L) >= REQUIRED_PAIRS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 1;
        public static final int REWARD = 120;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_BLIND_FOUND_PAIRS = 3;

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_6_name, R.string.achievement_memory_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_BLIND_FOUND_PAIRS);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_UNCOVERED_PAIRS_IN_GREEN_STATE_COUNT)) {
                if (mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_IN_GREEN_STATE_COUNT, 0L) >= REQUIRED_BLIND_FOUND_PAIRS) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            // Luck dependency
            Dependency dep =
                    TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.SNOW_INSTANCE, AchievementSnow.Achievement3.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            }
        }
    }

    private static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 1;
        public static final int REWARD = 120;
        public static final boolean DISCOVERED = false;
        public static final int REQUIRED_BLIND_FOUND_PAIRS = 3;
        public static final int REQUIRED_BLIND_GAMES = 5;
        private static final String KEY_GAME_ALREADY_CONSUMED = NUMBER + "_already_consumed";

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_7_name, R.string.achievement_memory_7_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_BLIND_GAMES, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_UNCOVERED_PAIRS_IN_GREEN_STATE_COUNT)) {
                if (mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_IN_GREEN_STATE_COUNT, 0L) >= REQUIRED_BLIND_FOUND_PAIRS
                        && areDependenciesFulfilled()
                        && mGameData.getValue(KEY_GAME_ALREADY_CONSUMED, 0L) == 0L) {
                    achieveDelta(1);
                    mGameData.putValue(KEY_GAME_ALREADY_CONSUMED, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            // definitly luck dependency
            Dependency dep =
                    TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.MEMORY_INSTANCE, Achievement6.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            }
        }
    }

    private static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 1;
        public static final int REWARD = 200;
        public static final boolean DISCOVERED = true;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_8_name, R.string.achievement_memory_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (mGameData.getValue(KEY_GAME_STATE_RED_COUNT, 0L) == 0L && mGameData.getValue(KEY_GAME_STATE_BLACK_COUNT, 0L) == 0L) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            //Cardships dependency
            Dependency dep =
                    TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.MEMORY_INSTANCE, Achievement7.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            }
        }
    }

    private static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = 250;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_CARDS = 976;
        private static final String KEY_GAME_UNCOVERED_PAIRS_CONSUMED = NUMBER + "_uncovered_pairs_consumed";

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_memory_9_name, R.string.achievement_memory_9_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_CARDS, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), REQUIRED_CARDS);
        }

        @Override
        public void onNonCustomDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_UNCOVERED_PAIRS_COUNT)) {
                long uncoveredPairs = mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_COUNT, 0L);
                long consumed = mGameData.getValue(KEY_GAME_UNCOVERED_PAIRS_CONSUMED, 0L);
                if (uncoveredPairs > consumed) {
                    long toConsume = uncoveredPairs - consumed;
                    mGameData.increment(KEY_GAME_UNCOVERED_PAIRS_CONSUMED, toConsume, 0L);
                    achieveDelta((int) toConsume);
                }
            }
        }
    }

}
