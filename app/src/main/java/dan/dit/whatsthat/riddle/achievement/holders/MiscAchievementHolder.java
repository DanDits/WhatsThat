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
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.achievement.MiscAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 21.06.15.
 */
public class MiscAchievementHolder implements AchievementHolder {
    public static final String KEY_ADMIRED_IMAGE_AUTHOR ="admired_image_author";
    private static final String DATA_NAME = "misc_achievement_data";
    public static final String KEY_SOLUTION_INPUT_CURRENT_TEXT = "misc_solution_input_current_text";
    public static final String KEY_LAST_SOLVED_GAME_PLAYED_TIME = "misc_last_solved_game_played_time";
    public static final String KEY_LAST_SOLVED_GAME_TOTAL_TIME = "misc_last_solved_game_total_time";
    public static final String KEY_ACHIEVEMENTS_EARNED_COUNT = "misc_total_achievements_earned";
    public static final String KEY_FEATURES_PURCHASED = "misc_features_purchased";
    public static final String KEY_SPIN_WHEEL_START_ANGLE_SPEED =
            "misc_spin_wheel_start_angle_speed";
    public static final String KEY_RETRYING_RIDDLE_COUNT = "misc_retrying_riddle_count";
    public static final String KEY_REMADE_RIDDLE_CURRENT_REMADE_COUNT = "misc_remade_riddle_current_remade_count";
    public static final String KEY_REMADE_RIDDLE_CURRENT_COUNT =
            "misc_remade_riddle_current_count";
    public static final String KEY_LEFT_DONATION_SITE_STAY_TIME =
            "misc_left_donation_site_stay_time";

    private AchievementPropertiesMapped<String> mData;
    private SortedMap<Integer, MiscAchievement> mAchievements;

    public AchievementPropertiesMapped<String> getData() {
        return mData;
    }

    private void makeData(AchievementManager manager) {
        try {
            mData = new AchievementPropertiesMapped<>(DATA_NAME, manager.loadDataEvent(DATA_NAME));
        } catch (CompactedDataCorruptException e) {
            mData = new AchievementPropertiesMapped<>(DATA_NAME);
            Log.e("Achievement","Failed creating misc properties from data: " + e);
        }
        manager.manageAchievementData(mData);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        makeData(manager);

        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager, mData));
        mAchievements.put(Achievement2.NUMBER, new Achievement2(manager, mData));
        mAchievements.put(Achievement3.NUMBER, new Achievement3(manager, mData));
        mAchievements.put(Achievement4.NUMBER, new Achievement4(manager, mData));
        mAchievements.put(Achievement5.NUMBER, new Achievement5(manager, mData));
        mAchievements.put(Achievement6.NUMBER, new Achievement6(manager, mData));
        mAchievements.put(Achievement7.NUMBER, new Achievement7(manager, mData));
        mAchievements.put(Achievement8.NUMBER, new Achievement8(manager, mData));
        mAchievements.put(Achievement9.NUMBER, new Achievement9(manager, mData));
        mAchievements.put(Achievement10.NUMBER, new Achievement10(manager, mData));
    }

    private static class Achievement1 extends MiscAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        public static final String CREATOR_NAME_1_1 = "Daniel";
        public static final String CREATOR_NAME_1_2 = "Dani";
        public static final String CREATOR_NAME_2_1 = "Fabian";
        public static final String CREATOR_NAME_2_2 = "Fabi";
        public static final String KEY_FIRST_CREATOR_NAME_ALREADY_ENTERED = MiscAchievement.makeId(NUMBER) + "first_name_entered";

        public Achievement1(AchievementManager manager, AchievementPropertiesMapped<String> miscData) {
            super(miscData, R.string.achievement_misc_1_name, R.string.achievement_misc_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 2, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mMiscData.removeKey(KEY_FIRST_CREATOR_NAME_ALREADY_ENTERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT)) {
                String currentText = mMiscData.getMappedValue(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT);
                if (!TextUtils.isEmpty(currentText) && areDependenciesFulfilled()) {
                    if ((currentText.equalsIgnoreCase(CREATOR_NAME_1_1) || currentText.equalsIgnoreCase(CREATOR_NAME_1_2))
                            && mMiscData.getValue(KEY_FIRST_CREATOR_NAME_ALREADY_ENTERED, 0L) == 0L) {
                        mMiscData.putValue(KEY_FIRST_CREATOR_NAME_ALREADY_ENTERED, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                        achieveDelta(1);
                    } else if ((currentText.equalsIgnoreCase(CREATOR_NAME_2_1) || currentText.equalsIgnoreCase(CREATOR_NAME_2_2))
                            && (getValue() == 0 || mMiscData.getValue(KEY_FIRST_CREATOR_NAME_ALREADY_ENTERED, 0L) == 1L)) {
                        achieveDelta(1);
                    }
                }
            }
        }
    }

    private static class Achievement2 extends MiscAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 75;
        public static final boolean DISCOVERED = false;
        private static final long MS_TO_DAYS = 1000 * 60 * 60 * 24;
        private static final long MIN_TOTAL_TIME = MS_TO_DAYS * 3; // ms

        public Achievement2(AchievementManager manager, AchievementPropertiesMapped<String> miscData) {
            super(miscData, R.string.achievement_misc_2_name, R.string.achievement_misc_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MIN_TOTAL_TIME / MS_TO_DAYS);
        }
        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(KEY_LAST_SOLVED_GAME_TOTAL_TIME)) {
                if (mMiscData.getValue(KEY_LAST_SOLVED_GAME_TOTAL_TIME, 0L) >= MIN_TOTAL_TIME) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement3 extends MiscAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 100;
        public static final boolean DISCOVERED = false;
        private static final long MS_TO_HOURS = 1000 * 60 * 60;
        private static final long MIN_PLAYED_TIME = MS_TO_HOURS * 2; // ms

        public Achievement3(AchievementManager manager, AchievementPropertiesMapped<String> miscData) {
            super(miscData, R.string.achievement_misc_3_name, R.string.achievement_misc_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MIN_PLAYED_TIME / MS_TO_HOURS);
        }
        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(KEY_LAST_SOLVED_GAME_PLAYED_TIME)) {
                if (mMiscData.getValue(KEY_LAST_SOLVED_GAME_PLAYED_TIME, 0L) >= MIN_PLAYED_TIME) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement4 extends MiscAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 2;
        public static final int REWARD = 300;
        public static final boolean DISCOVERED = true;
        public static final int ACHIEVEMENTS_REQUIRED = 60; //Daily achievements count in, too

        public Achievement4(AchievementManager manager, AchievementPropertiesMapped<String> miscData) {
            super(miscData, R.string.achievement_misc_4_name, R.string.achievement_misc_4_descr, 0, NUMBER, manager, LEVEL, REWARD, ACHIEVEMENTS_REQUIRED, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), ACHIEVEMENTS_REQUIRED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(KEY_ACHIEVEMENTS_EARNED_COUNT)) {
                achieveDelta(1); // do not check for conditions for this achievement!
            }
        }
    }


    private static class Achievement5 extends MiscAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 60;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_TIME_ON_DONATION_SITE = 60000;//ms

        public Achievement5(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_5_name, R.string.achievement_misc_5_descr,
                    0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (isAchieved()) {
                return res.getString(R.string.achievement_misc_5_achieved_descr);
            }
            return super.getDescription(res);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey
                    (KEY_LEFT_DONATION_SITE_STAY_TIME)) {
                if (mMiscData.getValue(KEY_LEFT_DONATION_SITE_STAY_TIME, 0L) >=
                        REQUIRED_TIME_ON_DONATION_SITE) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement6 extends MiscAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 100;
        public static final boolean DISCOVERED = true;
        public static final int ONE_TYPE_PLAYED_COUNT = 100;
        public Achievement6(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_6_name, R.string.achievement_misc_6_descr,
                    0, NUMBER, manager, LEVEL, REWARD, ONE_TYPE_PLAYED_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, ONE_TYPE_PLAYED_COUNT);
        }

        @Override
        protected void onInit() {
            super.onInit();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).addListener(this);
            }
        }

        protected void onAchieved() {
            super.onAchieved();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).removeListener(this);
            }
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.hasChangedKey(AchievementDataRiddleType.KEY_GAMES_SOLVED)) {
                int max = 0;
                for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                    AchievementDataRiddleType typeData = type.getAchievementData(mManager);
                    if (typeData != null) {
                        max = (int) Math.max(max, typeData.getValue(AchievementDataRiddleType
                                .KEY_GAMES_SOLVED, 0L));
                    }
                    if (typeData != null && typeData == event.getChangedData()
                            && max >= ONE_TYPE_PLAYED_COUNT) {
                        achieveAfterDependencyCheck();
                        break;
                    } else if (max < ONE_TYPE_PLAYED_COUNT) {
                        achieveProgressPercent((int) ((max / (double) ONE_TYPE_PLAYED_COUNT) *
                                100));
                    }
                }
            }
        }
    }

    private static class Achievement7 extends MiscAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        public static final float SPIN_ANGLE_SPEED_THRESHOLD = 6000f;

        public Achievement7(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_7_name, R.string.achievement_misc_7_descr,
                    0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey
                    (KEY_SPIN_WHEEL_START_ANGLE_SPEED)) {
                if (mMiscData.getValue(KEY_SPIN_WHEEL_START_ANGLE_SPEED, 0L) >=
                        SPIN_ANGLE_SPEED_THRESHOLD) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement8 extends MiscAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_REMADE_COUNT = 3;

        public Achievement8(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_8_name, R.string.achievement_misc_8_descr,
                    0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey
                    (KEY_REMADE_RIDDLE_CURRENT_COUNT)) {
                if (mMiscData.getValue(KEY_REMADE_RIDDLE_CURRENT_REMADE_COUNT, 0L) >=
                        REQUIRED_REMADE_COUNT) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement9 extends MiscAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 1;
        public static final int REWARD = 100;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_TYPES_WITH_BONUS = 3;

        public Achievement9(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_9_name, R.string.achievement_misc_9_descr,
                    0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_TYPES_WITH_BONUS);
        }

        @Override
        protected void onInit() {
            super.onInit();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).addListener(this);
            }
        }

        protected void onAchieved() {
            super.onAchieved();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).removeListener(this);
            }
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() != mMiscData) {
                int hasBonusCount = 0;
                for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                    hasBonusCount += type.getAchievementData(mManager).getValue
                            (AchievementDataRiddleType.KEY_BONUS_GAINED_COUNT, 0L) > 0L ? 1 : 0;
                }
                if (hasBonusCount >= REQUIRED_TYPES_WITH_BONUS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement10 extends MiscAchievement {
        public static final int NUMBER = 10;
        public static final int LEVEL = 0;
        public static final int REWARD = 42;
        public static final boolean DISCOVERED = false;

        public Achievement10(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(miscData, R.string.achievement_misc_10_name, R.string.achievement_misc_10_descr,
                    0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT)) {
                String currentText = mMiscData.getMappedValue(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT);
                if (!TextUtils.isEmpty(currentText) && areDependenciesFulfilled()) {
                    if (currentText.equalsIgnoreCase("keks") || currentText.equalsIgnoreCase
                            ("cookie")) {
                        achieve();
                    }
                }
            }
        }
    }


    @Override
    public void addDependencies() {
        for (MiscAchievement achievement : mAchievements.values()) {
            achievement.setDependencies();
        }
    }

    @Override
    public void initAchievements() {
        for (MiscAchievement achievement : mAchievements.values()) {
            achievement.init();
        }
    }

    @Override
    public List<? extends Achievement> getAchievements() {
        return new ArrayList<>(mAchievements.values());
    }

    @Override
    public int getExpectableTestSubjectScore(int testSubjectLevel) {
        int expected = 0;
        for (Achievement achievement : mAchievements.values()) {
            expected += achievement.getExpectedScore(testSubjectLevel);
        }
        return expected;
    }
}
