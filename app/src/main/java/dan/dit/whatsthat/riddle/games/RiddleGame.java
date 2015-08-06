package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import dan.dit.whatsthat.achievement.AchievementData;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.preferences.Language;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.solution.SolutionInput;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputManager;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

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
     * The default score awarded for solving a RiddleGame.
     */
    public static final int DEFAULT_SCORE = 1;

    /**
     * The width for a snapshot bitmap for unsolved RiddleGame's that get closed.
     */
    public static final int SNAPSHOT_WIDTH = 64;

    /**
     * The height for a snapshot bitmap.
     */
    public static final int SNAPSHOT_HEIGHT = 64;

    private Riddle mRiddle; // should be hidden
    private RiddleController mRiddleController;
    protected Image mImage; // image with hash of mRiddle.mCore.imageHash
    protected SolutionInput mSolutionInput; // input keyboard used, most likely choosing letters by clicking on them
    protected Bitmap mBitmap; // the correctly scaled bitmap of the image to work with
    // note: full galaxy s2 display 480x800 pixel, hdpi (scaling of 1.5 by default)
    protected RiddleConfig mConfig;

    /**
     * Creates a new RiddleGame, decorating the given riddle, using the given bitmap loaded from the riddle's image.
     * Will invoke the initBitmap(), initSolutionInput() and initAchievementData() hooks in this order.
     * @param riddle The riddle to decorate.
     * @param image The image associated to the riddle.
     * @param bitmap The image's bitmap.
     * @param res A resources object to load assets.
     * @param config The config to use or describe the riddle.
     * @param listener The listener to inform about progress (important if loading takes some time).
     */
    public RiddleGame(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
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
     * @return A new RiddleController for this RiddleGame and Riddle.
     */
    protected RiddleController makeController() {
        if (mConfig == null || mConfig.mControllerFactory == null) {
            return RiddleControllerFactory.INSTANCE.makeController(this, mRiddle);
        } else {
            return mConfig.mControllerFactory.makeController(this, mRiddle);
        }
    }

    /**
     * If this game is not yet closed, this is when the loaded bitmap object is still valid.
     * @return If the game was not yet closed.
     */
    protected boolean isNotClosed() {
        return mBitmap != null;
    }

    /**
     * Returns the current state associated with the riddle (probably loaded after restarting the app).
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
    protected void initSolutionInput() {
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
     * @return The riddle's image.
     */
    protected Image getImage() {
        return mImage;
    }

    public void onClose() {
        Log.d("Riddle", "On close of riddle game.");
        int solved = mSolutionInput.estimateSolvedValue();
        int score = 0;
        if (solved == Solution.SOLVED_COMPLETELY) {
            score = Math.max(calculateGainedScore(), 0); // no negative score gains
        }
        String currentState = null;
        if (solved < Solution.SOLVED_COMPLETELY) {
            currentState = compactCurrentState();
        }
        String solutionData;
        if (solved < Solution.SOLVED_COMPLETELY) {
            solutionData = mSolutionInput.compact();
        } else {
            solutionData = mSolutionInput.getCurrentUserSolution().compact();
        }
        mRiddle.getType().getAchievementDataGame().closeGame(solved);
        AchievementData achievementDataRaw = mConfig.mAchievementGameData;
        String achievementData = achievementDataRaw == null ? null : achievementDataRaw.compact();

        mRiddle.onClose(solved, score, currentState, achievementData, solutionData);

        mBitmap = null;
    }

    /**
     * Makes a new snapshot of this RiddleGame. By default none is made.
     * @return null
     */
    protected Bitmap makeSnapshot() {
        return null;
    }

    private void initAchievement() {
        String achievementData = mRiddle.getAchievementData();
        Compacter achievementCmp = TextUtils.isEmpty(achievementData) ? null : new Compacter(achievementData);
        mRiddle.getType().getAchievementDataGame().loadGame(achievementCmp);
        initAchievementData();
    }

    protected abstract void initAchievementData();

    protected abstract int calculateGainedScore();

    public abstract void draw(Canvas canvas);


    public boolean onOrientationEvent(float azimuth, float pitch, float roll) {
        return false;
    }
    public void enableNoOrientationSensorAlternative() {}

    public boolean requiresPeriodicEvent() {
        return false;
    }

    public boolean onPeriodicEvent(long updateTime) { return false;}

    protected abstract void initBitmap(Resources res, PercentProgressListener listener);
    public abstract boolean onMotionEvent(MotionEvent event);

    /**
     * Each riddle should try its best to preserve its state in a compact form that can be restored if
     * the user reopens the riddle from list of unsolved or simply after closing the app. Restarting
     * everything will lead to frustration.
     * @return Data to restore the exact current state of the riddle.
     */
    protected abstract @NonNull
    String compactCurrentState();

    public void initViews(RiddleView riddleView, SolutionInputView solutionView, SolutionInputListener listener) {
        riddleView.setController(mRiddleController);
        solutionView.setSolutionInput(mSolutionInput, listener);
    }

}
