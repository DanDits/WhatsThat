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
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 28.09.15.
 */
public class AchievementFlow extends  TypeAchievementHolder {
    public static final String KEY_GAME_TOTAL_PIXELS_COUNT = "total_pixels_count";
    public static final String KEY_GAME_REVEALED_PIXELS_COUNT = "revelead_pixels_count";
    public static final String KEY_GAME_CELLI_CREATED = "celli_created";
    public static final String KEY_GAME_CELLI_ACTIVE_COUNT = "celli_active_count";
    public static final String KEY_GAME_CELLI_COLLIDED_COUNT = "celli_collided";
    public static final String KEY_GAME_CELLI_TIMED_OUT_COUNT = "celli_timed_out";



    public AchievementFlow(PracticalRiddleType type) {
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
        private static final int REQUIRED_CELLIS = 11;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_2_name, R.string.achievement_flow_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_CELLIS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData &&
                    event.hasChangedKey(KEY_GAME_CELLI_TIMED_OUT_COUNT)) {
                if (mGameData.getValue(KEY_GAME_CELLI_TIMED_OUT_COUNT, 0L) >= REQUIRED_CELLIS) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    private static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_VISIBLE_PERCENT = 50;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_3_name, R.string.achievement_flow_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_VISIBLE_PERCENT);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_REVEALED_PIXELS_COUNT)) {
                if (mGameData.getValue(KEY_GAME_CELLI_TIMED_OUT_COUNT, 0L) >= REQUIRED_VISIBLE_PERCENT / 100. * mGameData.getValue(KEY_GAME_TOTAL_PIXELS_COUNT, 0L)) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_ACTIVE_CELLIS = 50;
        private static final int AVAILABLE_TIME = 13000; // ms
        private long mStartTime;

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_4_name, R.string.achievement_flow_4_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_ACTIVE_CELLIS, AVAILABLE_TIME / 1000);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_CREATED)) {
                long active = mGameData.getValue(KEY_GAME_CELLI_ACTIVE_COUNT, 0L);
                if (active == 0L && areDependenciesFulfilled()) {
                    mStartTime = System.currentTimeMillis();
                }
            } else if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE) {
                if (mStartTime > 0L && System.currentTimeMillis() - mStartTime <= AVAILABLE_TIME && mGameData.getValue(KEY_GAME_CELLI_ACTIVE_COUNT, 0L) >= REQUIRED_ACTIVE_CELLIS) {
                    achieveAfterDependencyCheck();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.TRIANGLE_INSTANCE, AchievementTriangle.Achievement3.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);// effizienzklicks
            }
        }
    }


    private static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = false;
        private static final int REQUIRED_CELLIS = 300;
        private static final int MAX_TIME_OUTS = 3;

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_5_name, R.string.achievement_flow_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, REQUIRED_CELLIS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_CREATED)) {
               if (mGameData.getValue(KEY_GAME_CELLI_CREATED, 0L) >= REQUIRED_CELLIS && mGameData.getValue(KEY_GAME_CELLI_TIMED_OUT_COUNT, 0L) <= MAX_TIME_OUTS) {
                   achieveAfterDependencyCheck();
               }
            }
        }
    }


    private static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int REQUIRED_CELLIS_AFTER_REOPENING = 100;

        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_6_name, R.string.achievement_flow_6_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_ACTIVE_COUNT)) {
                if (mGameData.getValue(KEY_GAME_CELLI_ACTIVE_COUNT, 0L) >= REQUIRED_CELLIS_AFTER_REOPENING) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    private static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        private static final int TOTAL_TIMED_OUT_CELLIS = 400;

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_7_name, R.string.achievement_flow_7_descr, 0, NUMBER, manager, LEVEL, REWARD, TOTAL_TIMED_OUT_CELLIS, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), TOTAL_TIMED_OUT_CELLIS);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_TIMED_OUT_COUNT)) {
                if (areDependenciesFulfilled()) {
                    achieveDelta(1);
                }
            }
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_GAME_CELLI_CREATED)) {
                Log.d("Achievement", "CELLI CREATED BY CLICK: active" + mGameData.getValue(KEY_GAME_CELLI_ACTIVE_COUNT, 0L)
                        + " created " + mGameData.getValue(KEY_GAME_CELLI_CREATED, 0L)
                        + " collided " + mGameData.getValue(KEY_GAME_CELLI_COLLIDED_COUNT, 0L)
                        + " timed out " + mGameData.getValue(KEY_GAME_CELLI_TIMED_OUT_COUNT, 0L)
                        + "  " + mGameData.getValue(KEY_GAME_REVEALED_PIXELS_COUNT, 0L) + "/" + mGameData.getValue(KEY_GAME_TOTAL_PIXELS_COUNT, 0L));
            }
        }
    }


    private static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        public static final int MAX_CELLIS_FOR_SOLVING = 10;

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_flow_8_name, R.string.achievement_flow_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), MAX_CELLIS_FOR_SOLVING);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (mGameData.getValue(KEY_GAME_CELLI_CREATED, 0L) <= MAX_CELLIS_FOR_SOLVING) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }
}
