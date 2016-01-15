package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.achievement.DailyAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Holds a bunch of achievements that are special in the case that they will be able to be
 * achieved (and claimed) multiple times. Achieving is possible once a day, every achievement
 * will be reset after initialization if not achieved on the same day and not yet reset on that day.
 * If the achievement reward wasn't claimed yet, the reward will be discarded!<br>
 *     By default initialized not listening to any achievement data events. So onInit needs to
 *     add the achievement as a listener to the required data events. OnAchieved should as usual
 *     unregister those listeners.<br>
 *     Additionally only some daily achievements will be available per day, these will be chosen
 *     at random on initialization of the holder.
 * Created by daniel on 12.01.16.
 */
public class DailyAchievementsHolder implements AchievementHolder {
    private static final int MAX_ACHIEVEMENTS_PER_DAY = 2;
    private Map<Integer, DailyAchievement> mAchievements;
    private Calendar mToday;

    public DailyAchievementsHolder() {

        //no validation is done if this is really today's date. Would need to request date from
        // some server, we trust the users or think if you find this "flaw" and want to abuse
        // it.. well have fun ;)
        mToday = Calendar.getInstance();
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager));
        mAchievements.put(Achievement2.NUMBER, new Achievement2(manager));
        // ids of SingleType achievements are negative!
        for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
            int number = -type.getId();
            AchievementSingleType achievement = new AchievementSingleType(manager, type, 0,
                    number, -1);
            mAchievements.put(number, achievement);
        }
        mAchievements.put(Achievement3.NUMBER, new Achievement3(manager));
    }

    private static class Achievement1 extends DailyAchievement {
        private static final int REQUIRED_RIDDLES_TO_SOLVE = 5;//>1 for plural reasons
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 20;
        public static final boolean DISCOVERED = true;
        public static final String KEY_GAMES_SOLVED_COUNT = NUMBER +
                "games_solved_today_count";
        private AchievementPropertiesMapped<String> mMiscData;

        public Achievement1(AchievementManager manager) {
            super(R.string.achievement_daily_1_name, R.string.achievement_daily_1_descr, 0, manager,
                    LEVEL, NUMBER, REWARD, REQUIRED_RIDDLES_TO_SOLVE, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_RIDDLES_TO_SOLVE);
        }
        @Override
        public void onInit() {
            super.onInit();
            mMiscData = TestSubject.getInstance().getAchievementHolder().getMiscData();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementDataGame().addListener(this);
            }
        }

        @Override
        protected void onReset() {
            mMiscData.putValue(KEY_GAMES_SOLVED_COUNT, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementDataGame().removeListener(this);
            }
        }

        @Override
        public void onIsAvailableDataEvent(AchievementDataEvent event) {
            if (event.getEventType() != AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    || !areDependenciesFulfilled()) {
                return;
            }
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                AchievementDataRiddleGame data = type.getAchievementDataGame();
                if (data == event.getChangedData()) {
                    if (!data.isCustom() && data.isSolved() && mMiscData != null) {
                        mMiscData.increment(KEY_GAMES_SOLVED_COUNT, 1L, 0L);
                        achieveDelta(1);
                    }
                    break;
                }
            }
        }
    }

    private static class Achievement2 extends DailyAchievement {
        public static final int NUMBER = 2;
        public static final int REWARD = 35;
        public static final boolean DISCOVERED = true;
        public static final String KEY_GAMES_SOLVED_FOR_TYPE_COUNT = NUMBER +
                "games_solved_today_for_type_count";
        private static final int REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE = 1;
        private static final int REQUIRED_DIFFERENT_TYPES = 4;
        public static final int LEVEL = REQUIRED_DIFFERENT_TYPES - 2;

        public Achievement2(AchievementManager manager) {
            super(R.string.achievement_daily_2_name, R.string.achievement_daily_2_descr, 0, manager,
                    LEVEL, NUMBER, REWARD, REQUIRED_DIFFERENT_TYPES * REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE,
                    DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE, REQUIRED_DIFFERENT_TYPES);
        }

        @Override
        protected void onReset() {
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).putValue(KEY_GAMES_SOLVED_FOR_TYPE_COUNT, 0L,
                        AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).removeListener(this);
            }
        }

        @Override
        public void onInit() {
            super.onInit();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementData(mManager).addListener(this);
            }
        }

        @Override
        public void onIsAvailableDataEvent(AchievementDataEvent event) {
            if (!event.hasChangedKey(AchievementDataRiddleType.KEY_GAMES_SOLVED)
                    || !areDependenciesFulfilled()) {
                return;
            }
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                AchievementDataRiddleType data = type.getAchievementData(mManager);
                if (data == event.getChangedData()) {
                    if (!data.isCustom()
                            && data.getValue(KEY_GAMES_SOLVED_FOR_TYPE_COUNT, 0L) < REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE) {
                        data.increment(KEY_GAMES_SOLVED_FOR_TYPE_COUNT, 1L, 0L);
                        achieveDelta(1);
                    }
                    break;
                }
            }
        }
    }

    private static class AchievementSingleType extends DailyAchievement {
        public static final int LEVEL = 0;
        public static final int REWARD = 25;
        public static final boolean DISCOVERED = true;
        public static final String KEY_GAMES_SOLVED_FOR_SINGLE_TYPE_COUNT = "single_type" +
                "games_solved_today_for_single_type_count";
        private static final int REQUIRED_RIDDLES_TO_SOLVE_DEFAULT = 2;//>1 for plural reasons,else >0
        private final PracticalRiddleType mType;

        public AchievementSingleType(AchievementManager manager, PracticalRiddleType type,
                                     int nameResId, int number, int solveCount) {
            super(nameResId == 0 ? R.string.achievement_daily_single_type_default_name : nameResId,
                    R.string.achievement_daily_single_type_descr,
                    0, manager,
                    LEVEL, number, REWARD,
                    solveCount <= 1 ? REQUIRED_RIDDLES_TO_SOLVE_DEFAULT : solveCount,
                    DISCOVERED);
            mType = type;
        }

        @Override
        public int getIconResId() {
            return mType.getIconResId();
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_RIDDLES_TO_SOLVE_DEFAULT,
                    res.getString(mType.getNameResId()));
        }

        @Override
        protected void onReset() {
            mType.getAchievementData(mManager).putValue(KEY_GAMES_SOLVED_FOR_SINGLE_TYPE_COUNT, 0L,
                    AchievementProperties.UPDATE_POLICY_ALWAYS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mType.getAchievementData(mManager).removeListener(this);
        }

        @Override
        public void onInit() {
            super.onInit();
            mType.getAchievementData(mManager).addListener(this);
        }

        @Override
        public void onIsAvailableDataEvent(AchievementDataEvent event) {
            if (!event.hasChangedKey(AchievementDataRiddleType.KEY_GAMES_SOLVED)
                    || !areDependenciesFulfilled()) {
                return;
            }
            AchievementDataRiddleType data = mType.getAchievementData(mManager);
            if (data == event.getChangedData()) {
                if (!data.isCustom()) {
                    data.increment(KEY_GAMES_SOLVED_FOR_SINGLE_TYPE_COUNT, 1L, 0L);
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            mDependencies.add(TestSubject.getInstance().getRiddleTypeDependency(mType));
        }
    }

    private static class Achievement3 extends DailyAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 2;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final long MAX_SOLVING_TIME = 30000;//ms

        public Achievement3(AchievementManager manager) {
            super(R.string.achievement_daily_3_name, R.string.achievement_daily_3_descr, 0, manager,
                    LEVEL, NUMBER, REWARD, 1,
                    DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAX_SOLVING_TIME / 1000L);
        }

        @Override
        protected void onReset() {
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementDataGame().removeListener(this);
            }
        }

        @Override
        public void onInit() {
            super.onInit();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementDataGame().addListener(this);
            }
        }

        @Override
        public void onIsAvailableDataEvent(AchievementDataEvent event) {
            if (event.getEventType() != AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    || !areDependenciesFulfilled()) {
                return;
            }
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                AchievementDataRiddleGame data = type.getAchievementDataGame();
                if (data == event.getChangedData()) {
                    if (!data.isCustom()
                            && data.isSolved()
                            && data.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L) <=
                                MAX_SOLVING_TIME) {
                        achieve();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void addDependencies() {
        for (DailyAchievement achievement : mAchievements.values()) {
            achievement.setDependencies();
        }
    }

    @Override
    public void initAchievements() {
        for (DailyAchievement achievement : mAchievements.values()) {
            achievement.init(mToday);
        }
        makeResetCandidatesAvailable();
    }

    private void makeResetCandidatesAvailable() {
        int availableCount = 0;
        List<DailyAchievement> candidates = new ArrayList<>(mAchievements.size());
        for (DailyAchievement achievement : mAchievements.values()) {
            if (achievement.gotResetToday(mToday) && !achievement.isAvailable()) {
                candidates.add(achievement);
            }
            if (achievement.isAvailable()) {
                availableCount++;
            }
        }
        //by default when one is reset all others are reset too on this day
        if (candidates.size() > 0) {
            // now get only some random of these achievements and make them available
            Collections.shuffle(candidates);

            for (int i = 0;
                 i < Math.min(MAX_ACHIEVEMENTS_PER_DAY - availableCount, candidates.size());
                 i++) {
                candidates.get(i).setAvailable();
            }
        }
    }

    public boolean refresh() {
        mToday = Calendar.getInstance();

        boolean foundAny = false;
        for (DailyAchievement achievement : mAchievements.values()) {
            if (achievement.checkedReset(mToday)) {
                foundAny = true;
            }
        }
        Log.d("Achievement", "Refreshing daily achievements, found any that got reset: " +
                foundAny);
        if (foundAny) {
            makeResetCandidatesAvailable();
        }
        return foundAny;
    }

    @Override
    public List<? extends Achievement> getAchievements() {
        List<DailyAchievement> available = new ArrayList<>(mAchievements.size());
        for (DailyAchievement achievement : mAchievements.values()) {
            if (achievement.isAvailable()) {
                available.add(achievement);
            }
        }
        return available;
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
