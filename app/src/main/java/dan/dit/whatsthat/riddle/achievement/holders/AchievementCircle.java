package dan.dit.whatsthat.riddle.achievement.holders;

import android.content.res.Resources;
import android.util.Log;

import java.util.TreeMap;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementDataTimer;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;

/**
 * Created by daniel on 21.06.15.
 */
public class AchievementCircle extends TypeAchievementHolder {
    public static final String KEY_CIRCLE_COUNT = "circle_count";
    public static final String KEY_CIRCLE_DIVIDED_BY_CLICK = "divided_by_click";
    public static final String KEY_CIRCLE_DIVIDED_BY_MOVE = "divided_by_move";

    public AchievementCircle(PracticalRiddleType type) {
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
    }

    // The beginning
    public static class Achievement1 extends GameAchievement {
        public static final int NUMBER = 1;
        public static final int LEVEL = 0;
        public static final int REWARD = 50;
        public static final boolean DISCOVERED = true;
        public static final int FINISH_GAMES_COUNT = 5;

        public Achievement1(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_1_name, R.string.achievement_circle_1_descr, 0, NUMBER, manager, LEVEL, REWARD, FINISH_GAMES_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), FINISH_GAMES_COUNT);
        }

        @Override
        public void onAchieved() {
            super.onAchieved();
            TestSubject.getInstance().purchaseNextHintForFree(PracticalRiddleType.CIRCLE_INSTANCE);
            TestSubject.getInstance().purchaseNextHintForFree(PracticalRiddleType.CIRCLE_INSTANCE);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mTypeData && event.hasChangedKey(AchievementDataRiddleType.KEY_GAMES_SOLVED)) {
                if (areDependenciesFulfilled()) {
                    achieveDelta(1);
                }
            }
        }
    }


    public static class Achievement2 extends GameAchievement {
        public static final int NUMBER = 2;
        public static final int LEVEL = 0;
        public static final int REWARD = 70;
        public static final boolean DISCOVERED = true;
        public static final int MAX_CIRCLES = 40;

        public Achievement2(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_2_name, R.string.achievement_circle_2_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, MAX_CIRCLES);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_CIRCLE_COUNT, 0L) <= MAX_CIRCLES) {
                    achieve();
                }
            }
        }
    }

    //So beautiful
    public static class Achievement3 extends GameAchievement {
        public static final int NUMBER = 3;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int MIN_CIRCLES = 64*64;
        private AchievementProperties mMiscData;

        public Achievement3(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_3_name, R.string.achievement_circle_3_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
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
            mMiscData.removeListener(this);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(MiscAchievementHolder.KEY_ADMIRED_IMAGE_AUTHOR)
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_OPENED) {
                if (mGameData.getValue(KEY_CIRCLE_COUNT, 0L) >= MIN_CIRCLES) {
                    mGameData.putValue(Achievement4.KEY_GAME_ADMIRED, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    achieveAfterDependencyCheck();
                }
            }
        }
    }


    public static class Achievement4 extends GameAchievement {
        public static final int NUMBER = 4;
        public static final int LEVEL = 0;
        public static final boolean DISCOVERED = true;
        public static final int MIN_CIRCLES = Achievement3.MIN_CIRCLES;
        public static final int JOBS_COUNT = 10;
        public static final int REWARD = JOBS_COUNT * 7;
        public static final String KEY_GAME_ADMIRED = NUMBER + MiscAchievementHolder.KEY_ADMIRED_IMAGE_AUTHOR;
        private AchievementProperties mMiscData;

        public Achievement4(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_4_name, R.string.achievement_circle_4_descr, 0, NUMBER, manager, LEVEL, REWARD, JOBS_COUNT, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, JOBS_COUNT);
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
            mMiscData.removeListener(this);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mMiscData && event.hasChangedKey(MiscAchievementHolder.KEY_ADMIRED_IMAGE_AUTHOR)
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_OPENED) {
                Log.d("Achievement", "Admired achievement3: " + mMiscData.getValue(MiscAchievementHolder.KEY_ADMIRED_IMAGE_AUTHOR, 0L) + " circles " + mGameData.getValue(KEY_CIRCLE_COUNT, 0L));
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_CIRCLE_COUNT, 0L) >= MIN_CIRCLES && mGameData.getValue(KEY_GAME_ADMIRED, 0L) == 0L) {
                    mGameData.putValue(KEY_GAME_ADMIRED, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, Achievement3.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency circle Achievement3 not created.");
            }
        }
    }

    //Mission possible
    public static class Achievement5 extends GameAchievement {
        public static final int NUMBER = 5;
        public static final int LEVEL = 0;
        public static final int REWARD = 60;
        public static final boolean DISCOVERED = true;
        public static final int FINISH_WITHIN_DURATION = 20000; //ms

        public Achievement5(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_5_name, R.string.achievement_circle_5_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, FINISH_WITHIN_DURATION / 1000);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (areDependenciesFulfilled()
                        && mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L) <= FINISH_WITHIN_DURATION
                        && mGameData.getValue(KEY_CIRCLE_DIVIDED_BY_CLICK, 0L) > mGameData.getValue(KEY_CIRCLE_DIVIDED_BY_MOVE, 0L)) {
                    achieve();
                }
            }
        }
    }

    //Click domination
    public static class Achievement6 extends GameAchievement {
        public static final int NUMBER = 6;
        public static final int LEVEL = 0;
        public static final int CLICKS_OVER_SWIPES_COUNT = 5;
        public static final int REWARD = CLICKS_OVER_SWIPES_COUNT*10;
        public static final boolean DISCOVERED = true;


        public Achievement6(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_6_name, R.string.achievement_circle_6_descr, 0, NUMBER, manager, LEVEL, REWARD, CLICKS_OVER_SWIPES_COUNT, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (areDependenciesFulfilled()
                        && mGameData.getValue(KEY_CIRCLE_DIVIDED_BY_CLICK, 0L) > mGameData.getValue(KEY_CIRCLE_DIVIDED_BY_MOVE, 0L)) {
                    achieveDelta(1);
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeClaimedAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, Achievement5.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency circle Achievement5 not created.");
            }
            dep = TestSubject.getInstance().makeProductPurchasedDependency(SortimentHolder.ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, 0);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency shop circle divide by move not created.");
            }
        }
    }

    //I'm in a hurry
    public static class Achievement7 extends GameAchievement {
        public static final int NUMBER = 7;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        public static final int FINISH_WITHIN_DURATION = 17000; //ms

        public Achievement7(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_7_name, R.string.achievement_circle_7_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, FINISH_WITHIN_DURATION / 1000);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE
                    && mGameData.isSolved()) {
                if (areDependenciesFulfilled()
                        && mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L) <= FINISH_WITHIN_DURATION) {
                    achieve();
                }
            }
        }
    }

    //still in a hurry
    public static class Achievement8 extends GameAchievement {
        public static final int NUMBER = 8;
        public static final int LEVEL = 2;
        public static final int REWARD = 150;
        public static final boolean DISCOVERED = true;
        private static final String KEY_TIMER_SOLVED = Types.Circle.NAME + NUMBER + "_solved"; // timed data key
        private static final int SOLVED_COUNT = 5;
        private static final int SOLVED_MAX_TIME = 120000; //ms

        public Achievement8(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_8_name, R.string.achievement_circle_8_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
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
                if (areDependenciesFulfilled()) {
                    // game data closed, game is solved, mark this event
                    mTimerData.onTimeKeeperUpdate(KEY_TIMER_SOLVED, mGameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, 0L));
                }
            }
            if (event.getChangedData() == mTimerData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_TIMER_SOLVED)) {
                AchievementDataTimer.TimeKeeper keeper = mTimerData.getTimeKeeper(KEY_TIMER_SOLVED);
                if (keeper != null && keeper.getTimesCount() == SOLVED_COUNT) {
                    long duration = keeper.getTotalTimeConsumed();
                    if (duration > 0L && duration <= SOLVED_MAX_TIME) {
                        achieve();
                    }
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, Achievement7.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency circle Achievement7 not created.");
            }
        }
    }

    //All we need is love
    public static class Achievement9 extends GameAchievement {
        public static final int NUMBER = 9;
        public static final int LEVEL = 0;
        public static final int REWARD = PracticalRiddleType.CIRCLE_INSTANCE.getBaseScore() * 7;
        public static final boolean DISCOVERED = true;
        private static final int MIN_RUBBISH_LETTERS = 18;
        private static final long TALK_POLITE = 1L;
        private static final long TALK_IMPOLITE = 2L;
        private AchievementPropertiesMapped<String> mMiscData;

        public Achievement9(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_9_name, R.string.achievement_circle_9_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
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
            mMiscData.removeListener(this);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            long talkValue = 0L;
            if (mMiscData == event.getChangedData() && event.hasChangedKey(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT)
                    && mGameData.getValue(KEY_CIRCLE_COUNT, 0L) == 1L
                    && mGameData.getState() == AchievementDataRiddleGame.STATE_OPENED) {
                String talk = mMiscData.getMappedValue(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT);
                if (talk != null
                        && (checkTalk(talk, "hi") || checkTalk(talk, "hello") || checkTalk(talk, "hallo") || checkTalk(talk, "whatup")
                            || checkTalk(talk, "servus") || checkTalk(talk, "moin") || checkTalk(talk, "hey") || checkTalk(talk, "bonjour"))) {
                    talkValue = TALK_POLITE;
                } else if (talk != null &&
                        (talk.length() >= MIN_RUBBISH_LETTERS)) {
                    talkValue = TALK_IMPOLITE;
                }
            }
            if (talkValue != 0L && areDependenciesFulfilled()) {
                achieve();
                claimReward();
                mGameData.putValue(Achievement10.KEY_ACHIEVE_ME, talkValue, AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
        }

        private boolean checkTalk(String talk, String hello) {
            return hello != null && hello.equalsIgnoreCase(talk);
        }
    }

    //All you get is work
    public static class Achievement10 extends GameAchievement {
        public static final int NUMBER = 10;
        public static final int LEVEL = 0;
        public static final int REWARD = PracticalRiddleType.CIRCLE_INSTANCE.getBaseScore() * 3;
        public static final boolean DISCOVERED = true;
        public static final String KEY_ACHIEVE_ME = NUMBER + "achieve_me";
        private AchievementPropertiesMapped<String> mMiscData;

        public Achievement10(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_10_name, R.string.achievement_circle_10_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getRewardDescription(Resources res) {
            if (isAchieved()) {
                return super.getRewardDescription(res);
            }
            return res.getString(R.string.achievement_reward_hidden);
        }
        @Override
        public CharSequence getDescription(Resources res) {
            if (areDependenciesFulfilled() && !isAchieved()) {
                return res.getString(R.string.achievement_circle_10_descr_unachieved);
            } else {
                return super.getDescription(res);
            }
        }

        private void waitForSorry() {
            mMiscData = TestSubject.getInstance().getAchievementHolder().getMiscData();
            mMiscData.addListener(this);
        }

        @Override
        public void onInit() {
            super.onInit();
            if (areDependenciesFulfilled()) {
                waitForSorry();
            }
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
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && event.hasChangedKey(KEY_ACHIEVE_ME)) {
                long talkStyle = mGameData.getValue(KEY_ACHIEVE_ME, 0L);
                if (talkStyle == Achievement9.TALK_POLITE) {
                    achieveAfterDependencyCheck();
                } else if (talkStyle == Achievement9.TALK_IMPOLITE) {
                    waitForSorry();
                }
            }
            if (event.getChangedData() == mMiscData && event.hasChangedKey(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT)
                    && mGameData.getValue(KEY_CIRCLE_COUNT, 0L) == 1L) {
                String sorry = mMiscData.getMappedValue(MiscAchievementHolder.KEY_SOLUTION_INPUT_CURRENT_TEXT);
                if (sorry != null && (sorry.equalsIgnoreCase("sorry") || sorry.equalsIgnoreCase("bigsorry")
                        || sorry.equalsIgnoreCase("sri") || sorry.equalsIgnoreCase("sry")
                        || sorry.equalsIgnoreCase("mybad") || sorry.equalsIgnoreCase("ups") || sorry.equalsIgnoreCase("pardon") || sorry.equalsIgnoreCase("tschuldigung")
                        || sorry.equalsIgnoreCase("entschuldigung") || sorry.equalsIgnoreCase("tschuldige"))) {
                    achieve();
                }
            }
        }

        @Override
        public void setDependencies() {
            super.setDependencies();
            Dependency dep = TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, Achievement9.NUMBER);
            if (dep != null) {
                mDependencies.add(dep);
            } else {
                Log.e("Achievement", "Dependency circle Achievement9 not created.");
            }
        }
    }

    //It's over 9000
    public static class Achievement11 extends GameAchievement {
        public static final int NUMBER = 11;
        public static final int LEVEL = 0;
        public static final int REWARD = 200;
        public static final boolean DISCOVERED = true;
        public static final int CIRCLE_TO_CREATE = 34746;
        public static final int CIRCLE_NOT_TO_CREATE = 34745;
        public static final int CIRCLE_NOT_TO_CREATE_TOO_MANY = 34747;

        public Achievement11(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_11_name, R.string.achievement_circle_11_descr, 0, NUMBER, manager, LEVEL, REWARD, CIRCLE_TO_CREATE, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            return res.getString(mDescrResId, getValue(), CIRCLE_TO_CREATE, CIRCLE_NOT_TO_CREATE, CIRCLE_NOT_TO_CREATE_TOO_MANY);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_UPDATE
                    && (event.hasChangedKey(KEY_CIRCLE_DIVIDED_BY_CLICK) || event.hasChangedKey(KEY_CIRCLE_DIVIDED_BY_MOVE))) {
                if (areDependenciesFulfilled()) {
                    achieveDelta(1);
                }
            }
        }
    }

    //Big brother is watching you
    public static class Achievement12 extends GameAchievement {
        public static final int NUMBER = 12;
        public static final int LEVEL = 0;
        public static final int REWARD = 30;
        public static final boolean DISCOVERED = true;
        private static final long TIME_TO_BE_FAST = 200L;

        public Achievement12(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_12_name, R.string.achievement_circle_12_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (event.getChangedData() == mGameData && event.hasChangedKey(KEY_CIRCLE_COUNT)) {
                if (areDependenciesFulfilled() && mGameData.getValue(KEY_CIRCLE_COUNT, 0L) == 4L
                        && (System.currentTimeMillis() - mGameData.getValue(AchievementDataRiddleGame.KEY_LAST_OPENED, 0L)) <= TIME_TO_BE_FAST) {
                    achieveAfterDependencyCheck();
                }
            }
        }
    }

    public static class Achievement13 extends GameAchievement {
        public static final int NUMBER = 13;
        public static final int LEVEL = 3;
        public static final int REWARD = 0;
        public static final boolean DISCOVERED = false;
        public static final int PERFECT_REWARD = 250;
        public static final int MAX_REWARD = 200;
        public static final int MIN_REWARD = 5;
        public static final long MAX_REWARD_TIME = 10L * 1000L;
        public static final long MIN_REWARD_TIME = 60L * 1000L;

        public Achievement13(AchievementManager manager, PracticalRiddleType type) {
            super(type, R.string.achievement_circle_13_name, R.string.achievement_circle_13_descr, 0, NUMBER, manager, LEVEL, REWARD, 1, DISCOVERED);
        }

        @Override
        public CharSequence getDescription(Resources res) {
            long bestTime = PracticalRiddleType.CIRCLE_INSTANCE.getAchievementData(mManager)
                    .getValue(AchievementDataRiddleType.KEY_BEST_SOLVED_TIME, Long.MAX_VALUE);
            if (bestTime < MAX_REWARD_TIME) {
                return res.getString(R.string.achievement_circle_13_descr_perfect, bestTime / 1000L);
            } else if (isAchieved() && !isRewardClaimable()) {
                return res.getString(R.string.achievement_circle_13_descr_claimed, bestTime / 1000L);
            } else if (bestTime != Long.MAX_VALUE) {
                return res.getString(mDescrResId, bestTime / 1000L);
            } else {
                return super.getDescription(res);
            }
        }

        @Override
        public int getScoreReward() {
            long bestTime = PracticalRiddleType.CIRCLE_INSTANCE.getAchievementData(mManager)
                    .getValue(AchievementDataRiddleType.KEY_BEST_SOLVED_TIME, Long.MAX_VALUE);
            if (bestTime > MIN_REWARD_TIME) {
                return 0;
            } else if (bestTime <= MAX_REWARD_TIME) {
                return PERFECT_REWARD;
            } else {
                //"linearly" (well, the time is scaled logarithmically) interpolate bestTime for points (MAX_REWARD_TIME, MAX_REWARD) , (MIN_REWARD_TIME, MIN_REWARD)
                return (int) ((MIN_REWARD - MAX_REWARD) / (Math.log10(MIN_REWARD_TIME) - Math.log10(MAX_REWARD_TIME)) * (Math.log10(bestTime) - Math.log10(MAX_REWARD_TIME)) + MAX_REWARD);
            }
        }

        @Override
        public int getMaxScoreReward() {
            return PERFECT_REWARD;
        }

        @Override
        public void onDataEvent(AchievementDataEvent event) {
            if (!isDiscovered() && event.getChangedData() == mGameData && mGameData.isSolved()
                    && event.getEventType() == AchievementDataEvent.EVENT_TYPE_DATA_CLOSE) {
                if (areDependenciesFulfilled()) {
                    achieve();
                } else if (!isDiscovered()) {
                    discover();
                }
            }
        }
    }
}
