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

package dan.dit.whatsthat.riddle.control;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import com.plattysoft.leonids.ParticleSystem;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.achievement.AchievementData;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.preferences.Language;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.solution.SolutionInput;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputManager;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * The "important" base type for the things that can be actually played! Decorates a Riddle object
 * and makes it playable, keeping the required data like bitmaps and precalculations that can take a heavy
 * bit of available RAM. A RiddleGame needs to be closed as soon as it is not used anymore and should free any resources.
 * Though users are encouraged to no longer hold onto a RiddleGame object as it is visible.<br>
 *     RiddleGames should be able to fully reconstruct their state to offer an optimal experience, though some abbreviations
 *     might be required as there might by a lot of input data.
 * Created by daniel on 29.04.15.
 */
public abstract class RiddleGame {

    /**
     * The dimension for a snapshot bitmap for unsolved RiddleGames that get closed.
     */
    protected static final Dimension SNAPSHOT_DIMENSION = new Dimension(32, 32);
    /**
     * Maximum time from creating a new riddle till solving that results in using the multiplier for score calculation
     */
    private static final long SCORE_BONUS_MAX_RIDDLE_TIME = 30 * 60 * 1000; //30 minutes in ms
    public static final int BASE_SCORE_MULTIPLIER = 1; //should not change
    private static final int MAX_SCORE_MULTIPLIER = 3; // > BASE_SCORE_MULTIPLIER
    private static final int SCORES_MULTIPLIED_PER_DAY_COUNT = 10;

    private final Riddle mRiddle; // should be hidden
    private final RiddleController mRiddleController;
    protected final Image mImage; // image with hash of mRiddle.mCore.imageHash
    private SolutionInput mSolutionInput; // input keyboard used, most likely choosing letters by clicking on them
    protected Bitmap mBitmap; // the correctly scaled bitmap of the image to work with
    // note: full galaxy s2 display 480x800 pixel, hdpi (scaling of 1.5 by default)
    protected final RiddleConfig mConfig;

    /**
     * Creates a new RiddleGame, decorating the given riddle, using the given bitmap loaded from the riddle's image.
     * Will invoke the initBitmap(), initSolutionInput() and initAchievementData() hooks in this order.
     *
     * @param riddle   The riddle to decorate.
     * @param image    The image associated to the riddle.
     * @param bitmap   The image's bitmap.
     * @param res      A resources object to load assets.
     * @param config   The config to use or describe the riddle.
     * @param listener The listener to inform about progress (important if loading takes some time).
     */
    protected RiddleGame(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig
            config, PercentProgressListener listener) {
        if (riddle == null || bitmap == null || res == null || listener == null || config == null || image == null) {
            throw new IllegalArgumentException("Null argument for InitializedRiddle given.");
        }
        mConfig = config;
        mImage = image;
        mRiddle = riddle;
        mRiddleController = makeController();
        mBitmap = bitmap;
        initBitmap(res, listener);
        initSolutionInput();
        initAchievement();
    }

    /**
     * Creates the RiddleController using the RiddleConfig's controller factory or a
     * default controller if none supplied.
     *
     * @return A new RiddleController for this RiddleGame and Riddle.
     */
    private RiddleController makeController() {
        if (mConfig == null || mConfig.mControllerFactory == null) {
            return RiddleControllerFactory.INSTANCE.makeController(this, mRiddle);
        } else {
            return mConfig.mControllerFactory.makeController(this, mRiddle);
        }
    }

    /**
     * If this game is not yet closed, this is when the loaded bitmap object is still valid.
     *
     * @return If the game was not yet closed.
     */
    protected boolean isNotClosed() {
        return mBitmap != null;
    }

    /**
     * Returns the current state associated with the riddle (probably loaded after restarting the app).
     *
     * @return A compacter holding the current state or null if the RiddleGame has no state saved (probably new riddle).
     */
    protected Compacter getCurrentState() {
        String state = mRiddle.getCurrentState();
        if (TextUtils.isEmpty(state)) {
            return null;
        } else {
            return new Compacter(state);
        }
    }

    private String getSolutionData() {
        return mRiddle.getSolutionData();
    }

    /**
     * Initializes the solution input. This should only be invoked on unsolved riddles. By default
     * this creates a new SolutionInput for the type using the solution entered to solve the riddle.
     * If the riddle is not yet solved this reconstructs the previous SolutionInput.
     */
    private void initSolutionInput() {
        if (mRiddle.isSolved()) {
            Solution sol;
            try {
                sol = new Solution(new Compacter(getSolutionData()));
            } catch (CompactedDataCorruptException e) {
                Log.e("Riddle", "Could not load solution " + e);
                sol = null;
            }
            if (sol != null) {
                mSolutionInput = SolutionInputManager.getSolutionInput(mRiddle.getType(), sol);
            }
        } else {
            String solutionData = getSolutionData();
            if (!TextUtils.isEmpty(solutionData)) {
                try {
                    mSolutionInput = SolutionInputManager.reconstruct(new Compacter(solutionData));
                } catch (CompactedDataCorruptException e) {
                    Log.e("Riddle", "Could not load solution input " + e);
                    mSolutionInput = null;
                }
            }
            if (mSolutionInput == null) {
                mSolutionInput = SolutionInputManager.getSolutionInput(mRiddle.getType(), mImage.getSolution(Language.getInstance().getTongue()));
            }
        }

    }

    /**
     * Returns the image used.
     *
     * @return The riddle's image.
     */
    protected Image getImage() {
        return mImage;
    }

    public void addAnimation(@NonNull RiddleAnimation animation) {
        mRiddleController.addAnimation(animation);
    }

    public void addAnimation(@NonNull RiddleAnimation animation, long delay) {
        mRiddleController.addAnimation(animation, delay);
    }

    public final synchronized void close() {
        int solved = mSolutionInput.estimateSolvedValue();
        int score = 0;
        String currentState = null;
        String solutionData;
        if (solved >= Solution.SOLVED_COMPLETELY) {
            RiddleScore riddleScore = calculateGainedScore();
            score = riddleScore.getTotalScore();
            solutionData = mSolutionInput.getCurrentUserSolution().compact();
        } else {
            currentState = compactCurrentState();
            solutionData = mSolutionInput.compact();
        }

        AchievementDataRiddleGame gameData = mRiddle.getType().getAchievementDataGame();
        gameData.closeGame(solved);
        AchievementDataRiddleType typeData = mRiddle.getType().getAchievementData(null);
        if (typeData != null && solved >= Solution.SOLVED_COMPLETELY) {
            typeData.putValue(AchievementDataRiddleType.KEY_BEST_SOLVED_TIME, gameData.getValue(AchievementDataRiddleGame.KEY_PLAYED_TIME, Long.MAX_VALUE), -1);
        }
        AchievementData achievementDataRaw = mConfig.mAchievementGameData;
        if (BuildConfig.DEBUG && achievementDataRaw != gameData) {
            throw new IllegalStateException("Config and type's achievement data not the same!");
        }
        String achievementData = achievementDataRaw == null ? null : achievementDataRaw.compact();

        mRiddle.onClose(solved, score, currentState, achievementData, solutionData);

        ImageUtil.CACHE.makeReusable(mBitmap);
        mBitmap = null;
        onClose();
    }

    protected void onClose() {
    }

    /**
     * Makes a new snapshot of this RiddleGame. By default none is made.
     *
     * @return null
     */
    protected Bitmap makeSnapshot() {
        return null;
    }

    private void initAchievement() {
        String achievementData = mRiddle.getAchievementData();
        Compacter achievementCmp = TextUtils.isEmpty(achievementData) ? null : new Compacter(achievementData);
        mRiddle.getType().getAchievementDataGame().loadGame(achievementCmp);
        Long customValue = Image.ORIGIN_IS_THE_APP.equalsIgnoreCase(mRiddle.getOrigin()) ? 0L : 1L;
        mRiddle.getType().getAchievementDataGame().putValue(AchievementDataRiddleGame.KEY_CUSTOM, customValue, AchievementProperties.UPDATE_POLICY_ALWAYS);
        AchievementDataRiddleType dataType = mRiddle.getType().getAchievementData(null);
        if (dataType != null) {
            dataType.putValue(AchievementDataRiddleGame.KEY_CUSTOM, customValue, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }

        initAchievementData();
    }

    protected abstract void initAchievementData();

    private static final double SCORE_EXP_FACTOR = Math.log(MAX_SCORE_MULTIPLIER - BASE_SCORE_MULTIPLIER) / SCORES_MULTIPLIED_PER_DAY_COUNT;

    /**
     * Calculates the gained score. Can but generally should not depend on the
     * way the riddle was solved and the game was played.
     * Calculations should be robust and produce the same result if nothing major happens to the game.
     *
     * @return A valid RiddleScore.
     */
    protected synchronized
    @NonNull
    RiddleScore calculateGainedScore() {
        int base = mRiddle.getType().getBaseScore();
        if (isCustom()) {
            if (mRiddle.isRemade()) {
                return new RiddleScore.SimpleNoBonus();
            }
            return RiddleScore.NullRiddleScore.INSTANCE;
        }
        int scoreMultiplicator = BASE_SCORE_MULTIPLIER;
        long now = System.currentTimeMillis();
        if (mConfig.mAchievementGameData != null && (now - mRiddle.getTimestamp()) < SCORE_BONUS_MAX_RIDDLE_TIME && TestSubject.isInitialized()) {
            int bonusCount = TestSubject.getInstance().getAndIncrementTodaysScoreBonusCount();
            scoreMultiplicator = (int) ((MAX_SCORE_MULTIPLIER - BASE_SCORE_MULTIPLIER) * Math.exp(-SCORE_EXP_FACTOR * bonusCount) + BASE_SCORE_MULTIPLIER);
            scoreMultiplicator = Math.max(1, scoreMultiplicator); // to be sure score will never be zero or negativly multiplied (which cannot happen for exp(x) but this might change)
        }
        return new RiddleScore(base, scoreMultiplicator);
    }

    protected final boolean isCustom() {
        return !Image.ORIGIN_IS_THE_APP.equalsIgnoreCase(mRiddle.getOrigin());
    }

    public abstract void draw(Canvas canvas);


    public boolean onOrientationEvent(float azimuth, float pitch, float roll) {
        return false;
    }

    public void enableNoOrientationSensorAlternative() {
    }

    public boolean requiresPeriodicEvent() {
        return false;
    }

    public void onPeriodicEvent(long updateTime) {
    }

    protected abstract void initBitmap(Resources res, PercentProgressListener listener);

    public abstract boolean onMotionEvent(MotionEvent event);

    /**
     * Each riddle should try its best to preserve its state in a compact form that can be restored if
     * the user reopens the riddle from list of unsolved or simply after closing the app. Restarting
     * everything will lead to frustration.
     *
     * @return Data to restore the exact current state of the riddle.
     */
    protected abstract
    @NonNull
    String compactCurrentState();

    public void initViews(RiddleView riddleView, SolutionInputView solutionView, SolutionInputListener listener) {
        riddleView.setController(mRiddleController);
        solutionView.setSolutionInput(mSolutionInput, listener);
    }

    public boolean requiresOrientationSensor() {
        return false;
    }

    public void onGotVisible() {

    }


    public ParticleSystem makeParticleSystem(Resources res, int maxParticles, int drawableResId,
                                             long timeToLive) {
        return mRiddleController.makeParticleSystem(res, maxParticles, drawableResId, timeToLive);
    }

    public int getRemadeCount() {
        return mRiddle.getRemadeCount();
    }
}
