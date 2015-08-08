package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.List;
import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleDice;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 22.06.15.
 */
public class AchievementDice extends TypeAchievementHolder {
    public static final String KEY_GAME_ALIEN_FUSED_WITH_RED = "alien_fused_red";
    public static final String KEY_GAME_ALIEN_FUSED_WITH_YELLOW = "alien_fused_yellow";
    public static final String KEY_GAME_ALIEN_FUSED_WITH_GREEN = "alien_fused_green";
    public static final String KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT = "field_completely_visible_count";
    public static final String KEY_GAME_RESET_COUNT = "reset_count";
    public static final String KEY_GAME_ALIEN_COUNT = "alien_count";
    public static final String KEY_GAME_PURPLE_COUNT = "purple_count";
    public static final String KEY_GAME_RED_COUNT = "red_count";
    public static final String KEY_GAME_YELLOW_COUNT = "yellow_count";
    public static final String KEY_GAME_GREEN_COUNT = "green_count";
    public static final String KEY_GAME_LAST_DICE_MOVED_DISTANCE = "last_moved_distance";
    public static final String KEY_GAME_LAST_DICE_MOVED_STATE = "last_moved_state";
    public static final String KEY_GAME_LAST_DICE_MOVED_NUMBER = "last_moved_number";
    public static final String KEY_GAME_LAST_DICE_COMBINED_STATE = "last_combined_state";
    public static final String KEY_GAME_LAST_DICE_COMBINED_POSITION = "last_combined_pos";
    public static final String KEY_GAME_RED_NUMBERS_AVAILABLE = "red_numbers_available";
    public static final String KEY_GAME_YELLOW_NUMBERS_AVAILABLE = "yellow_numbers_available";
    public static final String KEY_GAME_GREEN_NUMBERS_AVAILABLE = "green_numbers_available";
    public static final String KEY_GAME_PURPLE_NUMBERS_AVAILABLE = "purple_numbers_available";

    public AchievementDice(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager, mType));
        mAchievements.put(Achievement2.NUMBER, new Achievement2(manager, mType));
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
        mAchievements.put(Achievement18.NUMBER, new Achievement18(manager, mType));
        mAchievements.put(Achievement19.NUMBER, new Achievement19(manager, mType));
        mAchievements.put(Achievement20.NUMBER, new Achievement20(manager, mType));
        mAchievements.put(Achievement21.NUMBER, new Achievement21(manager, mType));
    }

    //Unpleasant beginning
    public static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = true;

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_1_name, R.string.achievement_dice_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_ALIEN_FUSED_WITH_RED) || event.hasChangedKey(KEY_GAME_ALIEN_FUSED_WITH_YELLOW)
                            || event.hasChangedKey(KEY_GAME_ALIEN_FUSED_WITH_GREEN))) {
                if (mGameData.getValue(KEY_GAME_ALIEN_FUSED_WITH_RED, 0L) >= 1
                        && mGameData.getValue(KEY_GAME_ALIEN_FUSED_WITH_YELLOW, 0L) >= 1
                        && mGameData.getValue(KEY_GAME_ALIEN_FUSED_WITH_GREEN, 0L) >= 1) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    //Color diversity
    public static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 10;
        public static final boolean DISCOVERED = true;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_2_name, R.string.achievement_dice_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_GREEN_NUMBERS_AVAILABLE)
                            || event.hasChangedKey(KEY_GAME_YELLOW_NUMBERS_AVAILABLE)
                            || event.hasChangedKey(KEY_GAME_RED_NUMBERS_AVAILABLE)
                            || event.hasChangedKey(KEY_GAME_PURPLE_NUMBERS_AVAILABLE))
                    && mGameData.getValue(KEY_GAME_RESET_COUNT, 0L) == 0L) {
                long greenNumbersAvailable = mGameData.getValue(KEY_GAME_GREEN_NUMBERS_AVAILABLE, 0L);
                long yellowNumbersAvailable = mGameData.getValue(KEY_GAME_YELLOW_NUMBERS_AVAILABLE, 0L);
                long redNumbersAvailable = mGameData.getValue(KEY_GAME_RED_NUMBERS_AVAILABLE, 0L);
                long purpleNumbersAvailable = mGameData.getValue(KEY_GAME_PURPLE_NUMBERS_AVAILABLE, 0L);
                boolean colorfulDiversity = true;
                for (int i = 1; colorfulDiversity && i <= 6; i++) {
                    if (((greenNumbersAvailable & (1L << i)) == 0)
                            || ((yellowNumbersAvailable & (1L << i)) == 0)
                            || ((redNumbersAvailable & (1L << i)) == 0)) {
                        colorfulDiversity = false;
                    }
                }
                for (int i = 7; colorfulDiversity && i <= 9; i++) {
                    if (((purpleNumbersAvailable & (1L << i)) == 0)) {
                        colorfulDiversity = false;
                    }
                }
                if (colorfulDiversity) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //powerful corner
    public static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 10;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_COMBINATIONS_IN_EACH_CORNER = 3;
        private static final String KEY_GREENS_IN_BOTTOM_LEFT = NUMBER + "greens_bottom_left";
        private static final String KEY_GREENS_IN_BOTTOM_RIGHT = NUMBER + "greens_bottom_right";
        private static final String KEY_GREENS_IN_TOP_LEFT = NUMBER + "greens_top_left";
        private static final String KEY_GREENS_IN_TOP_RIGHT= NUMBER + "greens_top_right";

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_4_name, R.string.achievement_dice_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_COMBINATIONS_IN_EACH_CORNER);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GREENS_IN_TOP_RIGHT);
            mGameData.removeKey(KEY_GREENS_IN_TOP_LEFT);
            mGameData.removeKey(KEY_GREENS_IN_BOTTOM_LEFT);
            mGameData.removeKey(KEY_GREENS_IN_BOTTOM_RIGHT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_POSITION)) {
                int number = mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_POSITION, -1L).intValue();
                if (mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_STATE, RiddleDice.STATE_RED) == RiddleDice.STATE_YELLOW
                        && number >= 0 && number < RiddleDice.FIELD_X * RiddleDice.FIELD_Y) {
                    int x = number % RiddleDice.FIELD_X;
                    int y = number / RiddleDice.FIELD_X;
                    Log.d("Achievement", "Combined yellow to green at " + x + "/" +  y + " number "+ number);
                    if (x == 0) {
                        if (y == 0) {
                            mGameData.increment(KEY_GREENS_IN_TOP_LEFT, 1L, 0L);
                        } else if (y == RiddleDice.FIELD_Y - 1) {
                            mGameData.increment(KEY_GREENS_IN_BOTTOM_LEFT, 1L, 0L);
                        }
                    } else if (x == RiddleDice.FIELD_X - 1) {
                        if (y == 0) {
                            mGameData.increment(KEY_GREENS_IN_TOP_RIGHT, 1L, 0L);
                        } else if (y == RiddleDice.FIELD_Y - 1) {
                            mGameData.increment(KEY_GREENS_IN_BOTTOM_RIGHT, 1L, 0L);
                        }
                    }
                }
            }
            if (event.getChangedData() == mGameData && (event.hasChangedKey(KEY_GREENS_IN_BOTTOM_LEFT)
                    || event.hasChangedKey(KEY_GREENS_IN_BOTTOM_RIGHT) || event.hasChangedKey(KEY_GREENS_IN_TOP_LEFT)
                    || event.hasChangedKey(KEY_GREENS_IN_TOP_RIGHT))) {
                if (mGameData.getValue(KEY_GREENS_IN_BOTTOM_LEFT, 0L) >= REQUIRED_COMBINATIONS_IN_EACH_CORNER
                        && mGameData.getValue(KEY_GREENS_IN_BOTTOM_RIGHT, 0L) >= REQUIRED_COMBINATIONS_IN_EACH_CORNER
                        && mGameData.getValue(KEY_GREENS_IN_TOP_LEFT, 0L) >= REQUIRED_COMBINATIONS_IN_EACH_CORNER
                        && mGameData.getValue(KEY_GREENS_IN_TOP_RIGHT, 0L) >= REQUIRED_COMBINATIONS_IN_EACH_CORNER) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Translation error
    public static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_RESETS = 3;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_5_name, R.string.achievement_dice_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_RESETS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_RESET_COUNT)) {
                if (mGameData.getValue(KEY_GAME_RESET_COUNT, 0L) >= REQUIRED_RESETS) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Green peace
    public static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        private static final int NOT_ENOUGH_GREENS = 20;
        private static final int ENOUGH_GREENS = 28;

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_6_name, R.string.achievement_dice_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, NOT_ENOUGH_GREENS, ENOUGH_GREENS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_GREEN_COUNT)) {
                if (mGameData.getValue(KEY_GAME_GREEN_COUNT, 0L) >= ENOUGH_GREENS) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Ludo
    public static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_TIMES = 4;
        public static final int POWERFUL_DICE_NUMBER = 6;
        private static final String KEY_GAME_POWERFUL_NUMBER_USED = NUMBER + "powerful_number_used";

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_7_name, R.string.achievement_dice_7_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_TIMES, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), REQUIRED_TIMES, POWERFUL_DICE_NUMBER);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_GREEN_NUMBERS_AVAILABLE)
                    || event.hasChangedKey(KEY_GAME_YELLOW_NUMBERS_AVAILABLE)
                    || event.hasChangedKey(KEY_GAME_RED_NUMBERS_AVAILABLE)
                    || event.hasChangedKey(KEY_GAME_PURPLE_NUMBERS_AVAILABLE))
                    && mGameData.getValue(KEY_GAME_RESET_COUNT, 0L) == 0L
                    && mGameData.getValue(KEY_GAME_POWERFUL_NUMBER_USED, 0L) == 0L) {
                long greenNumbersAvailable = mGameData.getValue(KEY_GAME_GREEN_NUMBERS_AVAILABLE, 0L);
                long yellowNumbersAvailable = mGameData.getValue(KEY_GAME_YELLOW_NUMBERS_AVAILABLE, 0L);
                long redNumbersAvailable = mGameData.getValue(KEY_GAME_RED_NUMBERS_AVAILABLE, 0L);
                long purpleNumbersAvailable = mGameData.getValue(KEY_GAME_PURPLE_NUMBERS_AVAILABLE, 0L);
            boolean colorfulNumber = ((greenNumbersAvailable & (1L << POWERFUL_DICE_NUMBER)) != 0) && ((yellowNumbersAvailable & (1L << POWERFUL_DICE_NUMBER)) != 0)
                    && ((redNumbersAvailable & (1L << POWERFUL_DICE_NUMBER)) != 0) && ((purpleNumbersAvailable & (1L << POWERFUL_DICE_NUMBER)) != 0);
                if (areDependenciesFulfilled() && colorfulNumber) {
                    mGameData.putValue(KEY_GAME_POWERFUL_NUMBER_USED, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Curious onlooker
    public static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        private static final int FIELDS_TO_REVEAL_COUNT = 5;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_8_name, R.string.achievement_dice_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, FIELDS_TO_REVEAL_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT)) {
                if (mGameData.getValue(KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT, 0L) >= FIELDS_TO_REVEAL_COUNT) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Logistic master
    public static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_ALIEN_COUNT = 5;
        private static final int REQUIRED_PURPLE_COUNT = 5;
        private static final int REQUIRED_GREEN_COUNT = 10;
        private static final int REQUIRED_YELLOW_COUNT = 10;

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_9_name, R.string.achievement_dice_9_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_ALIEN_COUNT, REQUIRED_PURPLE_COUNT, REQUIRED_GREEN_COUNT, REQUIRED_YELLOW_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_GREEN_COUNT)
                        || event.hasChangedKey(KEY_GAME_PURPLE_COUNT)
                        || event.hasChangedKey(KEY_GAME_ALIEN_COUNT)
                        || event.hasChangedKey(KEY_GAME_YELLOW_COUNT))) {
                if (mGameData.getValue(KEY_GAME_ALIEN_COUNT, 0L) >= REQUIRED_ALIEN_COUNT
                        && mGameData.getValue(KEY_GAME_PURPLE_COUNT, 0L) >= REQUIRED_PURPLE_COUNT
                        && mGameData.getValue(KEY_GAME_GREEN_COUNT, 0L) >= REQUIRED_GREEN_COUNT
                        && mGameData.getValue(KEY_GAME_YELLOW_COUNT, 0L) >= REQUIRED_YELLOW_COUNT) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //X-ray vision
    public static class Achievement10 extends GameAchievement {
        public static final int NUMBER = 10;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = true;

        public Achievement10(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_10_name, R.string.achievement_dice_10_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (mGameData.getValue(KEY_GAME_GREEN_COUNT, 0L) == 0L && mGameData.getValue(KEY_GAME_PURPLE_COUNT, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_ALIEN_COUNT, 0L) == 0L && mGameData.getValue(KEY_GAME_RESET_COUNT, 0L) == 0L) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Going for a walk
    public static class Achievement11 extends GameAchievement {
        public static final int NUMBER = 11;
        public static final int LEVEL = 0;
        public static final int REWARD = 25;
        public static final boolean DISCOVERED = true;

        private static final int DOGGY_1_STATE = RiddleDice.STATE_ALIEN;
        private static final int DOGGY_2_STATE = RiddleDice.STATE_YELLOW;
        private static final int DOGGY_1_DISTANCE = 400;
        private static final int DOGGY_2_DISTANCE = 1000;
        private static final int DOGGY_1_NUMBER = 4;
        private static final int DOGGY_2_NUMBER = 2;
        private static final String KEY_GAME_DOGGY_1_WALKED = NUMBER + "doggy_1_walked";
        private static final String KEY_GAME_DOGGY_2_WALKED = NUMBER + "doggy_2_walked";

        public Achievement11(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_11_name, R.string.achievement_dice_11_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, DOGGY_1_NUMBER, DOGGY_2_NUMBER);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (areDependenciesFulfilled() && event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_MOVED_DISTANCE)) {
                int state = mGameData.getValue(KEY_GAME_LAST_DICE_MOVED_STATE, RiddleDice.STATE_RED).intValue();
                int number = mGameData.getValue(KEY_GAME_LAST_DICE_MOVED_NUMBER, 0L).intValue();
                Long walked = mGameData.getValue(KEY_GAME_LAST_DICE_MOVED_DISTANCE, 0L);
                if (state == DOGGY_1_STATE && number == DOGGY_1_NUMBER) {
                    mGameData.increment(KEY_GAME_DOGGY_1_WALKED, walked, 0L);
                } else if (state == DOGGY_2_STATE && number == DOGGY_2_NUMBER) {
                    mGameData.increment(KEY_GAME_DOGGY_2_WALKED, walked, 0L);
                }
            }
            if (event.getChangedData() == mGameData && (event.hasChangedKey(KEY_GAME_DOGGY_1_WALKED) || event.hasChangedKey(KEY_GAME_DOGGY_2_WALKED))) {
                if (mGameData.getValue(KEY_GAME_DOGGY_1_WALKED, 0L) >= DOGGY_1_DISTANCE
                        && mGameData.getValue(KEY_GAME_DOGGY_2_WALKED, 0L) >= DOGGY_2_DISTANCE) {
                    achieve();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //This...
    public static class Achievement12 extends GameAchievement {
        public static final int NUMBER = 12;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_RED_COMBINATIONS = 600;


        public Achievement12(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_12_name, R.string.achievement_dice_12_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_RED_COMBINATIONS, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), REQUIRED_RED_COMBINATIONS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_STATE)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_STATE, RiddleDice.STATE_GREEN) == RiddleDice.STATE_RED) {
                    achieveDelta(1);
                }
            }
        }
    }

    //...is...
    public static class Achievement13 extends GameAchievement {
        public static final int NUMBER = 13;
        public static final int LEVEL = 0;
        public static final int REWARD = 25;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_YELLOW_COMBINATIONS = 200;


        public Achievement13(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_13_name, R.string.achievement_dice_13_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_YELLOW_COMBINATIONS, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), REQUIRED_YELLOW_COMBINATIONS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_STATE)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_STATE, RiddleDice.STATE_RED) == RiddleDice.STATE_YELLOW) {
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement12.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement12 not created.");
            }
        }
    }

    //...SPARTA
    public static class Achievement14 extends GameAchievement {
        public static final int NUMBER = 14;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_GREEN_COMBINATIONS = 50;


        public Achievement14(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_14_name, R.string.achievement_dice_14_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_GREEN_COMBINATIONS, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), REQUIRED_GREEN_COMBINATIONS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_STATE)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_STATE, RiddleDice.STATE_RED) == RiddleDice.STATE_GREEN) {
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement13.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement13 not created.");
            }
        }
    }

    //NO THIS IS PATRICK!
    public static class Achievement15 extends GameAchievement {
        public static final int NUMBER = 15;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_ALIEN_FUSIONS = 78; // 78=Patrick in "numbers" (source: fabian)


        public Achievement15(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_15_name, R.string.achievement_dice_15_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_ALIEN_FUSIONS, DISCOVERED);
        }

        @Override
        public CharSequence getName(Resources res) {
            if (isAchieved()) {
                return super.getName(res);
            } else {
                return res.getString(R.string.achievement_name_hidden);
            }
        }

        @Override
        public int getIconResIdByState() {
            if (isAchieved()) {
                if (isRewardClaimable()) {
                    return R.drawable.patrick_angry;
                } else {
                    return R.drawable.patrick;
                }
            } else {
                return super.getIconResIdByState();
            }
        }

        @Override
        public int getIconResId() {
            if (isAchieved()) {
                if (isRewardClaimable()) {
                    return R.drawable.patrick_angry;
                } else {
                    return R.drawable.patrick;
                }
            } else {
                return super.getIconResId();
            }
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_STATE)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_STATE, RiddleDice.STATE_RED) == RiddleDice.STATE_ALIEN) {
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement14.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement14 not created.");
            }
        }
    }

    //Warp jump
    public static class Achievement16 extends GameAchievement {
        public static final int NUMBER = 16;
        public static final int LEVEL = 0;
        public static final int REWARD = 15;
        public static final boolean DISCOVERED = true;

        private static final int JUMP_DISTANCE = 22;

        public Achievement16(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_16_name, R.string.achievement_dice_16_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_MOVED_DISTANCE)) {
                Long walked = mGameData.getValue(KEY_GAME_LAST_DICE_MOVED_DISTANCE, 0L);
                if (walked >= JUMP_DISTANCE) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Invasion
    public static class Achievement17 extends GameAchievement {
        public static final int NUMBER = 17;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = false;

        private static final int REQUIRED_ALIENS = 10;

        public Achievement17(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_17_name, R.string.achievement_dice_17_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_ALIEN_COUNT)) {
                if (mGameData.getValue(KEY_GAME_ALIEN_COUNT, 0L) >= REQUIRED_ALIENS) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
        }
    }

    //Paparazzi
    public static class Achievement18 extends GameAchievement {
        public static final int NUMBER = 18;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        private static final long TIME_TO_GET_PICTURE = 90000L; //ms

        public Achievement18(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_18_name, R.string.achievement_dice_18_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (isAchieved() && !isRewardClaimable()) {
                return res.getString(R.string.achievement_dice_18_descr_claimed);
            }
            return res.getString(mDescrResId, TIME_TO_GET_PICTURE / 1000L);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE) {
                Log.d("Achievement", "Paparazzi took " + mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L));
                if (mGameData.getValue(KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT, 0L) == 1
                        && mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L) <= TIME_TO_GET_PICTURE) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement8.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement8 not created.");
            }
        }
    }

    //Perfectionist
    public static class Achievement19 extends GameAchievement {
        public static final int NUMBER = 19;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;

        public Achievement19(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_19_name, R.string.achievement_dice_19_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT)) {
                if (mGameData.getValue(KEY_GAME_FIELDS_COMPLETELY_VISIBLE_COUNT, 0L) == RiddleDice.FIELD_X * RiddleDice.FIELD_Y) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement8.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement8 not created.");
            }
        }
    }

    //focus
    public static class Achievement20 extends GameAchievement {
        public static final int NUMBER = 20;
        public static final int LEVEL = 0;
        public static final int REWARD = 15;
        public static final boolean DISCOVERED = true;
        private static final int MAXIMUM_COMBINATIONS = 75;
        private static final String KEY_GAME_CHOSEN_COMBINATION_POSITION = NUMBER + "chosen_comb_position";
        private static final String KEY_GAME_COMBINATIONS_COUNT = NUMBER + "combinations_count";

        public Achievement20(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_20_name, R.string.achievement_dice_20_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAXIMUM_COMBINATIONS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_CHOSEN_COMBINATION_POSITION);
            mGameData.removeKey(KEY_GAME_COMBINATIONS_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_POSITION)) {
                int number = mGameData.getValue(KEY_GAME_LAST_DICE_COMBINED_POSITION, -1L).intValue();
                if (number >= 0 && areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_RESET_COUNT, 0L) == 0L) {
                    int chosenPosition = mGameData.getValue(KEY_GAME_CHOSEN_COMBINATION_POSITION, -1L).intValue();
                    if (chosenPosition == -1) {
                        if (number == 14 || number == 15 || number == 20 || number == 21) {
                            mGameData.putValue(KEY_GAME_CHOSEN_COMBINATION_POSITION, (long) number, AchievementProperties.UPDATE_POLICY_ALWAYS);
                        }
                    } else if (chosenPosition == number) {
                        mGameData.increment(KEY_GAME_COMBINATIONS_COUNT, 1L, 0L);
                    } else {
                        // combined somewhere else, invalidate this game for this achievement
                        mGameData.putValue(KEY_GAME_CHOSEN_COMBINATION_POSITION, -2L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    }

                }
            }
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
               if (mGameData.getValue(KEY_GAME_COMBINATIONS_COUNT, -1L) <= MAXIMUM_COMBINATIONS
                       && mGameData.getValue(KEY_GAME_CHOSEN_COMBINATION_POSITION, -1L) >= 0) {
                   achieve();
               }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement10.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement10 not created.");
            }
        }
    }

    //Minimalist
    public static class Achievement21 extends GameAchievement {
        public static final int NUMBER = 21;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = true;
        private static final int MAXIMUM_COMBINATIONS = 30;
        private static final String KEY_GAME_COMBINATIONS_COUNT = NUMBER + "combinations_count";

        public Achievement21(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_dice_21_name, R.string.achievement_dice_21_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (isAchieved() && !isRewardClaimable()) {
                return res.getString(R.string.achievement_dice_21_descr_claimed);
            }
            return res.getString(mDescrResId, MAXIMUM_COMBINATIONS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_COMBINATIONS_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()
                    && mGameData.getValue(KEY_GAME_COMBINATIONS_COUNT, 0L) <= MAXIMUM_COMBINATIONS) {
                achieveAfterDependencyCheck();
            }
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_LAST_DICE_COMBINED_STATE)) {
                mGameData.increment(KEY_GAME_COMBINATIONS_COUNT, 1L, 0L);
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            addUnpleasantBeginningDependency(mDependencies);
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement19.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency dice Achievement19 not created.");
            }
        }
    }

    private static void addUnpleasantBeginningDependency(List<Dependency> dependencies) {
        Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.DICE_INSTANCE, Achievement1.NUMBER);
        if (dep != null) {
            dependencies.add(dep);
        } else {
            Log.e("Achievement", "Dependency dice Achievement1 (unpleasant beginning) not created.");
        }
    }
}
