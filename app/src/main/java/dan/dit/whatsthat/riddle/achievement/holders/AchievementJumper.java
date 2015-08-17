package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleJumper;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleRiddleHints;

/**
 * Created by daniel on 23.07.15.
 */
public class AchievementJumper extends TypeAchievementHolder {
    public static final String KEY_GAME_DOUBLE_JUMP_COUNT= "double_jump_count";
    public static final String KEY_GAME_CURRENT_DIFFICULTY = "curr_difficulty";
    public static final String KEY_GAME_OBSTACLE_DODGED_COUNT = "obstacle_dodged";
    public static final String KEY_GAME_COLLISION_COUNT = "collision_count";
    public static final String KEY_GAME_COLLIDED_WITH_KNUFFBUFF = "knuffbuff_collision";
    public static final String KEY_GAME_RUN_HIGHSCORE = "highscore";
    public static final String KEY_GAME_CURRENT_DISTANCE_RUN = "curr_distance_run";
    public static final String KEY_TYPE_TOTAL_RUN_HIGHSCORE = "total_highscore";
    public static final String KEY_TYPE_JUMP_COUNT= "all_jumps";
    public static final String KEY_GAME_RUN_STARTED = "run_started";
    public static final long DISTANCE_RUN_THRESHOLD = (long) RiddleJumper.meterToDistanceRun(10);

    public AchievementJumper(PracticalRiddleType type) {
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
        mAchievements.put(Achievement11.NUMBER, new Achievement11(manager, mType));
    }

    private static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = false;
        public static final int SOLUTION = 43;

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_1_name, R.string.achievement_jumper_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            if (isAchieved() && !isRewardClaimable()) {
                return res.getString(R.string.achievement_jumper_1_descr_claimed);
            }
            return super.getDescription(res);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_COLLISION_COUNT)) {
                if (mGameData.getValue(KEY_GAME_OBSTACLE_DODGED_COUNT, 0L) == SOLUTION - 1
                        && mGameData.getValue(KEY_GAME_COLLISION_COUNT, 0L) == 1L) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = false;
        public static final int FAIL_COUNT = 30;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_2_name, R.string.achievement_jumper_2_descr, 0, NUMBER, manager, LEVEL, REWARD, FAIL_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, FAIL_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_COLLISION_COUNT) && areDependenciesFulfilled()) {
                if (mGameData.getValue(KEY_GAME_CURRENT_DIFFICULTY, RiddleJumper.DIFFICULTY_EASY + 1) == RiddleJumper.DIFFICULTY_EASY) {
                    achieveDelta(1);
                }
            }
        }
    }

    // Super Mario
    private static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_DOUBLE_JUMPS = 30;
        public static final String KEY_GAME_DOUBLE_JUMPS_BEFORE_MEDIUM = NUMBER + "jumps_before_medium";

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_3_name, R.string.achievement_jumper_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_DOUBLE_JUMPS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_DOUBLE_JUMPS_BEFORE_MEDIUM);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_DOUBLE_JUMP_COUNT) && areDependenciesFulfilled()) {
                if (mGameData.getValue(KEY_GAME_CURRENT_DIFFICULTY, RiddleJumper.DIFFICULTY_EASY) < RiddleJumper.DIFFICULTY_MEDIUM) {
                    long jumpsDone = mGameData.increment(KEY_GAME_DOUBLE_JUMPS_BEFORE_MEDIUM, 1L, 0L);
                    if (jumpsDone >= REQUIRED_DOUBLE_JUMPS) {
                        achieve();
                    }
                }
            } else if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_RUN_STARTED)) {
                if (mGameData.getValue(KEY_GAME_CURRENT_DIFFICULTY, RiddleJumper.DIFFICULTY_EASY) < RiddleJumper.DIFFICULTY_MEDIUM) {
                    mGameData.putValue(KEY_GAME_DOUBLE_JUMPS_BEFORE_MEDIUM, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
            }
        }
    }

    private static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_4_name, R.string.achievement_jumper_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                if (mGameData.getValue(KEY_GAME_CURRENT_DIFFICULTY, RiddleJumper.DIFFICULTY_EASY) >= RiddleJumper.DIFFICULTY_HARD
                        && mGameData.getValue(KEY_GAME_COLLISION_COUNT, 0L) == 0L) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = false;
        public static final int OBSTACLES_TO_DODGE = 400;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_5_name, R.string.achievement_jumper_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, OBSTACLES_TO_DODGE);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_OBSTACLE_DODGED_COUNT)) {
                if (mGameData.getValue(KEY_GAME_OBSTACLE_DODGED_COUNT, 0L) >= OBSTACLES_TO_DODGE) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = false;
        public static final long OUR_DISTANCE_RUN = (long) RiddleJumper.meterToDistanceRun(1996);

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_6_name, R.string.achievement_jumper_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CURRENT_DISTANCE_RUN)) {
                if (mGameData.getValue(KEY_GAME_CURRENT_DISTANCE_RUN, 0L) >= OUR_DISTANCE_RUN - DISTANCE_RUN_THRESHOLD) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, Achievement5.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency jumper Achievement5 not created.");
            }
        }
    }

    private static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        private static final Long REQUIRED_HINT_NUMBER = 11L;
        private AchievementPropertiesMapped<String> mMiscData;

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_7_name, R.string.achievement_jumper_7_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onInit() {
            super.onInit();
            mMiscData = TestSubject.getInstance().getAchievementHolder().getMiscData();
            mMiscData.addListener(this);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            if (mMiscData != null) {
                mMiscData.removeListener(this);
            }
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData) {
                String key = ShopArticleRiddleHints.makeKey(PracticalRiddleType.JUMPER_INSTANCE);
                if (event.hasChangedKey(key) && mMiscData.getValue(key, -1L) >= REQUIRED_HINT_NUMBER) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = false;
        public static final int REQUIRED_KNUFFBUFF_COLLISIONS = 6;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_8_name, R.string.achievement_jumper_8_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_KNUFFBUFF_COLLISIONS, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_COLLIDED_WITH_KNUFFBUFF)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_CURRENT_DIFFICULTY, RiddleJumper.DIFFICULTY_EASY) >= RiddleJumper.DIFFICULTY_ULTRA) {
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, Achievement7.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency jumper Achievement7 not created.");
            }
        }
    }

    private static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_RUN_DISTANCE = (int) RiddleJumper.meterToDistanceRun(42195);
        public static final String KEY_TYPE_DISTANCE_RUN = NUMBER + "marathon_distance_run";

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_9_name, R.string.achievement_jumper_9_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_RUN_DISTANCE, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, RiddleJumper.distanceRunToMeters(REQUIRED_RUN_DISTANCE));
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mTypeData.removeKey(KEY_TYPE_DISTANCE_RUN);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED
                    && areDependenciesFulfilled()) {
                long distanceRun = mTypeData.increment(KEY_TYPE_DISTANCE_RUN, mGameData.getValue(KEY_GAME_RUN_HIGHSCORE, 0L) + DISTANCE_RUN_THRESHOLD, 0L);
                Log.d("Achievement", "Marathon progress distance " + distanceRun + " current highscore was: " + mGameData.getValue(KEY_GAME_RUN_HIGHSCORE, 0L));
                achieveProgressPercent((int) (100.0 * distanceRun / (double) REQUIRED_RUN_DISTANCE));
            } else if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CURRENT_DISTANCE_RUN)) {
                if (mTypeData.getValue(KEY_TYPE_DISTANCE_RUN, 0L) + mGameData.getValue(KEY_GAME_CURRENT_DISTANCE_RUN, 0L) >= REQUIRED_RUN_DISTANCE) {
                    Log.d("Achievement", "Marathon achieved by current distance run: " + mTypeData.getValue(KEY_TYPE_DISTANCE_RUN, 0L) + " current distance: " + mGameData.getValue(KEY_GAME_CURRENT_DISTANCE_RUN, 0L));
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    // Natural talent
    private static class Achievement11 extends GameAchievement {
        public static final int NUMBER = 11;
        public static final int LEVEL = 0;
        public static final int REWARD = 5;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_SUCCESSIVE_GAMES = 3;
        private static final long REQUIRED_RUN_DISTANCE = (long) RiddleJumper.meterToDistanceRun(1000);
        private static final String KEY_TYPE_SUCCESSIVE_CHECKPOINTS_REACHED = NUMBER + "checkpoints_reached";

        public Achievement11(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_jumper_11_name, R.string.achievement_jumper_11_descr, 0, NUMBER, manager, LEVEL, REWARD, 3, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_SUCCESSIVE_GAMES, RiddleJumper.distanceRunToMeters(REQUIRED_RUN_DISTANCE));
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved() && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED
                    && areDependenciesFulfilled()) {
                double achievedGames;
                if (mGameData.getValue(KEY_GAME_RUN_HIGHSCORE, 0L) >= REQUIRED_RUN_DISTANCE - DISTANCE_RUN_THRESHOLD) {
                    achievedGames = mTypeData.increment(KEY_TYPE_SUCCESSIVE_CHECKPOINTS_REACHED, 1L, 0L).doubleValue();
                } else {
                    achievedGames = 0.;
                    mTypeData.putValue(KEY_TYPE_SUCCESSIVE_CHECKPOINTS_REACHED, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                }
                achieveProgressPercent((int) (100. * achievedGames / REQUIRED_SUCCESSIVE_GAMES));
            }
        }
    }
}
