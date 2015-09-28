package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 28.09.15.
 */
public class AchievementFlow extends  TypeAchievementHolder {
    public static final String KEY_GAME_TOTAL_PIXELS_COUNT = "total_pixels_count";
    public static final String KEY_GAME_REVEALED_PIXELS_COUNT = "revelead_pixels_count";
    public static final String KEY_GAME_CELLI_CREATED = "celli_created";
    public static final String KEY_GAME_CELLI_ACTIVE_COUNT = "celli_active_count";
    public static final String KEY_GAME_CELLI_COLLIDED_COUNT = "celli_collided";
    public static final String KEY_GAME_CELLI_TIMED_COUNT_COUNT = "celli_timed_out";



    public AchievementFlow(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
        mAchievements.put(Achievement1.NUMBER, new Achievement1(manager, mType));
    }

    private static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        public static final int REQUIRED_CELLIS = 15;
        public static final String KEY_GAME_SINGLE_CELLIS_SET = NUMBER + "single_cellis_set";
        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_1_name, R.string.achievement_flow_1_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_CELLIS);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            mGameData.removeKey(KEY_GAME_SINGLE_CELLIS_SET);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_CREATED)) {
                if (mGameData.getValue(KEY_GAME_CELLI_ACTIVE_COUNT, 0L) > 0L) {
                    mGameData.putValue(KEY_GAME_SINGLE_CELLIS_SET, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                } else {
                    long newCount = mGameData.increment(KEY_GAME_SINGLE_CELLIS_SET, 1L, 0L);
                    if (newCount >= REQUIRED_CELLIS) {
                        achieveAfterDependencyCheck();
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
        private static final int REQUIRED_CELLIS = 20;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_2_name, R.string.achievement_flow_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_CELLIS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {

        }
    }

    private static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_3_name, R.string.achievement_flow_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {

        }
    }
}
