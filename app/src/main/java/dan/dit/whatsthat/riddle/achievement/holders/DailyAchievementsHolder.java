package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Calendar;
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

/**
 * Created by daniel on 12.01.16.
 */
public class DailyAchievementsHolder implements AchievementHolder {
    protected final AchievementPropertiesMapped<String> mMiscData;
    private Map<Integer, DailyAchievement> mAchievements;

    public DailyAchievementsHolder(AchievementPropertiesMapped<String> miscData) {
        mMiscData = miscData;
        //noinspection ConstantConditions
        if (miscData == null) {
            throw new IllegalArgumentException("No misc data given.");
        }
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager, mMiscData));
        mAchievements.put(Achievement2.NUMBER, new Achievement2(manager, mMiscData));
    }

    private static class Achievement1 extends DailyAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final String KEY_GAMES_SOLVED_COUNT = NUMBER +
                "games_solved_today_count";
        private static final int REQUIRED_RIDDLES_TO_SOLVE = 5;

        public Achievement1(AchievementManager manager, AchievementPropertiesMapped<String> miscData) {
            super(R.string.achievement_daily_1_name, R.string.achievement_daily_1_descr, 0, manager,
                    LEVEL, NUMBER, REWARD, REQUIRED_RIDDLES_TO_SOLVE, DISCOVERED, miscData);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_RIDDLES_TO_SOLVE);
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
        public void onInit() {
            super.onInit();
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                type.getAchievementDataGame().addListener(this);
            }
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getEventType() != AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    || !areDependenciesFulfilled()) {
                return;
            }
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                AchievementDataRiddleGame data = type.getAchievementDataGame();
                if (data == event.getChangedData()) {
                    if (!data.isCustom() && data.isSolved()) {
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
        public static final int LEVEL = 1;
        public static final int REWARD = 35;
        public static final boolean DISCOVERED = true;
        public static final String KEY_GAMES_SOLVED_FOR_TYPE_COUNT = NUMBER +
                "games_solved_today_for_type_count";
        private static final int REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE = 1;
        private static final int REQUIRED_DIFFERENT_TYPES = 3;

        public Achievement2(AchievementManager manager, AchievementPropertiesMapped<String>
                miscData) {
            super(R.string.achievement_daily_2_name, R.string.achievement_daily_2_descr, 0, manager,
                    LEVEL, NUMBER, REWARD, REQUIRED_DIFFERENT_TYPES * REQUIRED_RIDDLES_TO_SOLVE_PER_TYPE,
                    DISCOVERED, miscData);
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
        public void onDataEvent(AchievementDataEvent event) {
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

    @Override
    public void addDependencies() {
        for (DailyAchievement achievement : mAchievements.values()) {
            achievement.setDependencies();
        }
    }

    @Override
    public void initAchievements() {
        Calendar today = Calendar.getInstance();
        //no validation is done if this is really today's date. Would need to request date from
        // some server, we trust the users or think if you find this "flaw" and want to abuse
        // it.. well have fun ;)
        for (DailyAchievement achievement : mAchievements.values()) {
            achievement.init(today);
        }
    }

    @Override
    public List<? extends Achievement> getAchievements() {
        return new ArrayList<>(mAchievements.values());
    }
}
