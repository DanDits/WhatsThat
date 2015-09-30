package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementDataTimer;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.games.RiddleSnow;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 21.05.15.
 */
public class AchievementSnow extends TypeAchievementHolder {
    public static final String KEY_GAME_FEATURE_ORIENTATION_SENSOR_ENABLED = "feature_orientation_sensor";
    public static final String KEY_GAME_FEATURE_ORIENTATION_SENSOR_CHANGED = "feature_orientation_sensor_changed";
    public static final String KEY_GAME_IDEAS_COLLECTED = "ideas_collected"; // when the cell collects an idea, can trigger a cell_state event afterwards
    public static final String KEY_GAME_COLLISION_COUNT = "collision_count";
    public static final String KEY_GAME_CELL_STATE = "cell_state"; // current size = ideas collected (maybe explosive)
    public static final String KEY_GAME_BIG_EXPLOSION = "big_explosion";
    public static final String KEY_GAME_WALL_EXPLOSION = "wall_explosion";
    public static final String KEY_GAME_PRE_COLLISION_CELL_STATE = "collision_cell_state";
    public static final String KEY_GAME_COLLISION_SPEED = "collision_speed";
    public static final String KEY_TYPE_MAX_SPEED = "max_speed";
    public static final String KEY_GAME_ANGEL_COLLECTED_IDEA = "angle_collected_idea"; // when the angel collects an idea
    public static final long CELL_SPEED_REQUIRED_DELTA = 50L;
    public static final String KEY_GAME_DEVIL_VISIBLE_STATE = "devil_visible";
    public static final String KEY_GAME_CELL_COLLECTED_SPORE = "cell_collected_spore";


    public AchievementSnow(PracticalRiddleType type) {
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
    }

    //Need for speed
    public static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 60;
        public static final boolean DISCOVERED = true;
        private static final Long REQUIRED_SPEED = 1750L; // within required delta tolerance

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_2_name, R.string.achievement_snow_2_descr, 0, NUMBER, manager, LEVEL, REWARD, REQUIRED_SPEED.intValue(), DISCOVERED);
        }

        @Override
        public String getDescription(Resources res) {
            long reachedValue = REQUIRED_SPEED;
            if (mTypeData != null) {
                reachedValue = mTypeData.getValue(KEY_TYPE_MAX_SPEED, 0L);
                reachedValue = Math.min(reachedValue, REQUIRED_SPEED);
            }
            return res.getString(mDescrResId, reachedValue, REQUIRED_SPEED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mTypeData && event.hasChangedKey(KEY_TYPE_MAX_SPEED)) {
                if (areDependenciesFulfilled()) {
                    long achievedSpeed = mTypeData.getValue(KEY_TYPE_MAX_SPEED, 0L);
                    if (achievedSpeed > REQUIRED_SPEED - CELL_SPEED_REQUIRED_DELTA) {
                        achieve();
                    } else {
                        achieveProgressPercent((int) (100 * achievedSpeed / REQUIRED_SPEED.doubleValue()));
                    }
                }
            }
        }
    }

    //Hurry up
    public static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 85;
        public static final boolean DISCOVERED = true;
        private static final String KEY_TIMER_SOLVED = Types.Snow.NAME + NUMBER + "_solved"; // timed data key
        private static final int SOLVED_COUNT = 2;
        private static final long SOLVED_MAX_TIME = 120000; //ms

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_1_name, R.string.achievement_snow_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onInit() {
            super.onInit();
            mTimerData.ensureTimeKeeper(KEY_TIMER_SOLVED, SOLVED_COUNT);
            mTimerData.addListener(this);
        }
        @Override
        public String getDescription(Resources res) {
            return res.getString(mDescrResId, SOLVED_COUNT, SOLVED_MAX_TIME / 1000);
        }

        @Override
        protected void onAchieved() {
            super.onAchieved();
            mTimerData.removeTimerKeeper(KEY_TIMER_SOLVED);
            mTimerData.removeListener(this);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                // game data closed, game is solved, mark this event
                mTimerData.onTimeKeeperUpdate(KEY_TIMER_SOLVED, mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L));
            }
            if (event.getChangedData() == mTimerData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_TIMER_SOLVED)) {
                AchievementDataTimer.TimeKeeper keeper = mTimerData.getTimeKeeper(KEY_TIMER_SOLVED);
                if (keeper != null && keeper.getTimesCount() == SOLVED_COUNT) {
                    long duration = keeper.getTotalTimeConsumed();
                    if (duration > 0L && duration <= SOLVED_MAX_TIME) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }


    }

    //Luck...
    public static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 150;
        public static final boolean DISCOVERED = true;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_3_name, R.string.achievement_snow_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.getValue(AchievementDataRiddleGame.KEY_SOLVED, Solution.SOLVED_NOTHING) == Solution.SOLVED_COMPLETELY
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                // game data closed, game is solved, check explosion count
                long explosionCount = mGameData.getValue(KEY_GAME_BIG_EXPLOSION, 0L) + mGameData.getValue(KEY_GAME_WALL_EXPLOSION, 0L);
                if (explosionCount == 1) {
                    achieveAfterDependencyCheck();
                }
            }

        }

    }

    //Mission impossible
    public static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 1;
        public static final int REWARD = 200;
        public static final boolean DISCOVERED = true;
        private static final String KEY_TIMER_COLLECT_IDEA = Types.Snow.NAME + NUMBER + "_collectidea"; // timed data key
        private static final int COLLECT_COUNT = 13;
        private static final long COLLECT_MAX_TIME = 20000; //ms

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_4_name, R.string.achievement_snow_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onInit() {
            super.onInit();
            mTimerData.ensureTimeKeeper(KEY_TIMER_COLLECT_IDEA, COLLECT_COUNT);
            mTimerData.addListener(this);
        }

        @Override
        public String getDescription(Resources res) {
            return res.getString(mDescrResId, COLLECT_COUNT, COLLECT_MAX_TIME / 1000);
        }

        @Override
        protected void onAchieved() {
            super.onAchieved();
            mTimerData.removeTimerKeeper(KEY_TIMER_COLLECT_IDEA);
            mTimerData.removeListener(this);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (areDependenciesFulfilled()) {
                if (event.getChangedData() == mGameData
                        && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                        && event.hasChangedKey(KEY_GAME_IDEAS_COLLECTED)) {
                    // idea collected
                    mTimerData.onTimeKeeperUpdate(KEY_TIMER_COLLECT_IDEA, 0L);
                }
                if (event.getChangedData() == mTimerData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                        && event.hasChangedKey(KEY_TIMER_COLLECT_IDEA)) {
                    AchievementDataTimer.TimeKeeper keeper = mTimerData.getTimeKeeper(KEY_TIMER_COLLECT_IDEA);
                    if (keeper != null && keeper.getTimesCount() == COLLECT_COUNT) {
                        long duration = keeper.getTotalTimeConsumed();
                        if (duration > 0L && duration <= COLLECT_MAX_TIME) {
                            achieve();
                        }
                    }
                }
            }
        }
    }

    //Bombastic
    public static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 150;
        public static final boolean DISCOVERED = true;
        private static final int EXPLOSION_COUNT = 3;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_5_name, R.string.achievement_snow_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public String getDescription(Resources res) {
            return res.getString(mDescrResId, EXPLOSION_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_GAME_BIG_EXPLOSION)) {
                long bigExplosions = mGameData.getValue(KEY_GAME_BIG_EXPLOSION, 0L);
                if (bigExplosions >= EXPLOSION_COUNT && mGameData.getValue(KEY_GAME_COLLISION_COUNT, 0L) == 0L) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    //Not today!
    public static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 85;
        public static final boolean DISCOVERED = true;
        private static final int TOUCH_COUNT = 5;
        private static final String KEY_GAME_COLLISION_WITH_MAX_SIZE = NUMBER + "collision_with_max_size";

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_6_name, R.string.achievement_snow_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_COLLISION_WITH_MAX_SIZE);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_GAME_PRE_COLLISION_CELL_STATE)) {
                long oldState = mGameData.getValue(KEY_GAME_PRE_COLLISION_CELL_STATE, 0L);
                if (oldState == RiddleSnow.IDEAS_REQUIRED_FOR_MAX_SIZE) {
                    //was explosion cell
                    long newValue = mGameData.increment(KEY_GAME_COLLISION_WITH_MAX_SIZE, 1L, 0L);
                    if (newValue >= TOUCH_COUNT) {
                        achieveAfterDependencyCheck();
                    }
                }
            }
        }
    }

    //...or maybe skill?
    public static class Achievement11 extends GameAchievement {
        public static final int NUMBER = 11;
        public static final int LEVEL = 0;
        public static final int REWARD = 100;
        public static final boolean DISCOVERED = true;
        private static final int SOLVE_WITH_ONE_EXPLOSION_COUNT = 6; //one will be triggered when Achievement 3 is fulfilled

        public Achievement11(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_11_name, R.string.achievement_snow_11_descr, 0, NUMBER, manager, LEVEL, REWARD, SOLVE_WITH_ONE_EXPLOSION_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, SOLVE_WITH_ONE_EXPLOSION_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
                if (event.getChangedData() == mGameData
                        && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                        && mGameData.getValue(AchievementDataRiddleGame.KEY_SOLVED, Solution.SOLVED_NOTHING) == Solution.SOLVED_COMPLETELY
                        && mGameData.getState() == AchievementDataRiddleGame.STATE_CLOSED) {
                    // game data closed, game is solved, check explosion count
                    long explosionCount = mGameData.getValue(KEY_GAME_BIG_EXPLOSION, 0L) + mGameData.getValue(KEY_GAME_WALL_EXPLOSION, 0L);
                    if (explosionCount == 1) {
                        if (!areDependenciesFulfilled()) {
                            addDeltaIfNotAchieved(1);
                        } else {
                            achieveDelta(1);
                        }
                    }
                }

        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.SNOW_INSTANCE, Achievement3.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency snow Achievement3 not created.");
            }
        }

    }

    //They see me rolling'
    public static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 80;
        public static final boolean DISCOVERED = true;
        public static final long IDLE_TIME_PASSED = 60000; //ms
        public static final String KEY_IDLE_TIME_PASSED = NUMBER + "idle_time_passed";

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_7_name, R.string.achievement_snow_7_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_IDLE_TIME_PASSED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_IDLE_TIME_PASSED)) {
                if (mGameData.getValue(KEY_GAME_BIG_EXPLOSION, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_COLLISION_COUNT, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_WALL_EXPLOSION, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_IDEAS_COLLECTED, 0L) == 0L
                        && mGameData.getValue(KEY_GAME_ANGEL_COLLECTED_IDEA, 0L) == 0L) {
                    achieveAfterDependencyCheck();
                }
            }
        }

    }


    //Gotta catch 'em all
    public static class Achievement13 extends GameAchievement {
        public static final int NUMBER = 13;
        public static final int LEVEL = 0;
        public static final int REWARD = 300;
        public static final boolean DISCOVERED = true;
        public static final int CATCH_EM_ALL = 721;

        public Achievement13(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_13_name, R.string.achievement_snow_13_descr, 0, NUMBER, manager, LEVEL, REWARD, CATCH_EM_ALL, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), CATCH_EM_ALL);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_IDEAS_COLLECTED)) {
                achieveDelta(1);
            }
        }

    }

    //Stubborn donkey
    public static class Achievement10 extends GameAchievement {
        public static final int NUMBER = 10;
        public static final int LEVEL = 0;
        public static final int REWARD = 65;
        public static final boolean DISCOVERED = false;
        private static final int COLLISION_COUNT = 100;
        private static final long MIN_COLLISION_SPEED = 30;
        private static final String KEY_COLLISION_COUNT = NUMBER + "collision_count";

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_COLLISION_COUNT);
        }

        public Achievement10(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_10_name, R.string.achievement_snow_10_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && (event.hasChangedKey(KEY_GAME_COLLISION_SPEED))) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_GAME_COLLISION_SPEED, 0L) >= MIN_COLLISION_SPEED) {
                    long newValue = mGameData.increment(KEY_COLLISION_COUNT, 1L, 0L);
                    if (newValue >= COLLISION_COUNT) {
                        achieve();
                    }
                }
            }
        }

    }

    //Bouncing cell
    public static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 90;
        public static final boolean DISCOVERED = true;

        public static final int COLLISION_COUNT = 4;
        public static final long TIME_FOR_COLLISIONS = 1000; //ms
        private long mExplosionTimestamp;
        private int mCollisionsInTime;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_8_name, R.string.achievement_snow_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        private boolean isInTime(long timestamp) {
            return timestamp - mExplosionTimestamp <= TIME_FOR_COLLISIONS;
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, COLLISION_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (areDependenciesFulfilled() && event.getChangedData() == mGameData && (event.hasChangedKey(KEY_GAME_BIG_EXPLOSION) || event.hasChangedKey(KEY_GAME_WALL_EXPLOSION))) {
                long currTime = System.currentTimeMillis();
                if (mExplosionTimestamp == 0L || !isInTime(currTime)) {
                    // start the time interval when we count collisions
                    mExplosionTimestamp = currTime;
                    mCollisionsInTime = 0;
                }
            }

            if (event.getChangedData() == mGameData
                    && mExplosionTimestamp != 0L && event.hasChangedKey(KEY_GAME_COLLISION_COUNT)) {
                long currTime = System.currentTimeMillis();
                if (isInTime(currTime)) {
                    mCollisionsInTime++;
                    Log.d("Achievement", "Collisions in " + (currTime - mExplosionTimestamp) + "milliseconds: " + mCollisionsInTime);
                    if (mCollisionsInTime >= COLLISION_COUNT) {
                        achieve();
                    }
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeClaimedAchievementDependency(PracticalRiddleType.SNOW_INSTANCE, Achievement2.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency snow Achievement2 not created.");
            }
        }
    }

    //Lovely lie
    public static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = 75;
        public static final boolean DISCOVERED = false;
        public static final String KEY_GAME_DEVIL_KILLED_COUNT = NUMBER + "devil_killed";
        private static final long KILL_COUNT = 3;

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_9_name, R.string.achievement_snow_9_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_DEVIL_KILLED_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (areDependenciesFulfilled() &&
                    event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_DEVIL_VISIBLE_STATE)) {
                if (mGameData.getValue(KEY_GAME_DEVIL_VISIBLE_STATE, RiddleSnow.DEFAULT_DEVIL_IS_VISIBLE ? 1L : 0L) == 0L) {
                    long killedCount = mGameData.increment(KEY_GAME_DEVIL_KILLED_COUNT, 1L, 0L);
                    if (killedCount >= KILL_COUNT) {
                        achieve();
                    }
                }
            }
        }
    }

    //Overkill
    public static class Achievement12 extends GameAchievement {
        public static final int NUMBER = 12;
        public static final int LEVEL = 0;
        public static final int REWARD = 80;
        public static final boolean DISCOVERED = true;

        public static final int OVERKILL_COUNT = 2;
        private boolean mStateOverkill;
        private int mOverkillCount;

        public Achievement12(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_12_name, R.string.achievement_snow_12_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, OVERKILL_COUNT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (areDependenciesFulfilled() && event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELL_STATE) ) {
                if (mGameData.getValue(KEY_GAME_CELL_STATE, 0L) == RiddleSnow.IDEAS_REQUIRED_FOR_MAX_SIZE) {
                    mOverkillCount = 0;
                    mStateOverkill = true;
                    Log.d("Achievement", "Starting overkill collection.");
                    return;
                } else {
                    mStateOverkill = false;
                }
            }

            if (mStateOverkill && event.getChangedData() == mGameData
                    && event.hasChangedKey(KEY_GAME_IDEAS_COLLECTED)) {
                mOverkillCount++;
                Log.d("Achievement", "Overkill collected: " + mOverkillCount);
                if (mOverkillCount >= OVERKILL_COUNT) {
                    achieve();
                }
            }
        }

    }

    //Allergy
    public static class Achievement14 extends GameAchievement {
        public static final int NUMBER = 14;
        public static final int LEVEL = 0;
        public static final int REWARD = 90;
        public static final boolean DISCOVERED = true;
        public static final int MAX_SPORES = 10;

        public Achievement14(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_snow_14_name, R.string.achievement_snow_14_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAX_SPORES);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData
                    && areDependenciesFulfilled()
                    && event.hasChangedKey(KEY_GAME_BIG_EXPLOSION)) {
                if (mGameData.getValue(KEY_GAME_CELL_COLLECTED_SPORE, 0L) <= MAX_SPORES) {
                    achieve();
                }
            }
        }
    }
}
