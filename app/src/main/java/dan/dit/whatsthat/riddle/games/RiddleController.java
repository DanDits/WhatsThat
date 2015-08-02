package dan.dit.whatsthat.riddle.games;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * A riddle controller is the class between the RiddleView and the RiddleGame. If closed the controller can
 * no longer be used, it is directly bound to the lifecycle of a RiddleGame.
 * Created by daniel on 05.04.15.
 */
public class RiddleController {
    private RiddleGame mRiddleGame;
    private Riddle mRiddle;
    private RiddleView mRiddleView;
    private GameRenderThread mRenderThread;
    private GamePeriodicThread mPeriodicThread;

    /**
     * Initializes the RiddleController with the RiddleGame that decorates the given Riddle.
     * @param riddleGame The game that decorates the Riddle parameter.
     * @param riddle The riddle decorated by the game.
     */
    protected RiddleController(@NonNull RiddleGame riddleGame, @NonNull Riddle riddle) {
        mRiddleGame = riddleGame;
        mRiddle = riddle;
    }

    /**
     * The controller is closing, close the riddle and make it save its state. After this method
     * returns the controller is invalid.
     * @param context A context object required for saving state to permanent storage.
     */
    public final void onCloseRiddle(@NonNull Context context) {
        pausePeriodicEvent();
        if (riddleAvailable()) {
            onPreRiddleClose();
            mRiddleGame.onClose();
            mRiddleGame = null;
            onRiddleClosed(context);
        }
        mRiddleView = null;
    }

    /**
     * Invoked on closure of the controller before the RiddleGame's onClose method is invoked.
     */
    protected void onPreRiddleClose() {
        RiddleManager.addToCache(mRiddle, mRiddleGame.makeSnapshot());
    }

    // this is overwritten if we don't want the manager to know of this riddle and dont want it saved
    /**
     * Invoked on closure of the controller after the RiddleGame's onClose method returned. The RiddleGame is not
     * a valid member anymore. By default saving achievement data and decorated riddle object.
     * @param context The context required to save to permanent storage.
     */
    protected void onRiddleClosed(final Context context) {
        if (mRiddle.isSolved()) {
            mRiddle.getType().getAchievementData(AchievementManager.getInstance()).onSolvedGame();
        }
        mRiddle.saveToDatabase(context);
        if (mRiddle.isSolved()) {
            RiddleInitializer.INSTANCE.getRiddleManager().onRiddleSolved(mRiddle);
        } else {
            RiddleInitializer.INSTANCE.getRiddleManager().onUnsolvedRiddle(mRiddle);
        }
    }

    /* ************* LAYOUT RELATED METHODS ********************************************************/

    /**
     * Draws the RiddleGame on the given canvas if there is a valid riddle.
     * @param canvas The canvas to draw onto.
     */
    public void draw(Canvas canvas) {
        if (riddleAvailable()) {
            mRiddleGame.draw(canvas);
        }
    }

    /* ************ INPUT RELATED METHODS *********************************************************/

    /**
     * Invoked if there happened some MotionEvent of any kind to the RiddleView.
     * If possible forwards the event to the RiddleGame, redrawing the game if onMotionEvent suggests so.
     * @param event The event to forward.
     */
    public void onMotionEvent(MotionEvent event) {
        if (riddleAvailable() && mRiddleGame.onMotionEvent(event) && mRiddleView != null) {
            mRiddleView.draw();
        }
    }

    /**
     * Invoked if there happened some OrientationEvent that changed the orientation of the device in the world's
     * coordinate system and orientation sensor is required.
     * If possible forwards the orientation event to the RiddleGame, redrawing the game if onOrientationEvent suggests so.
     * Given angles in radians, for specification see Wikipedia.
     * @param azimuth The new azimuth.
     * @param pitch The new pitch.
     * @param roll The new roll.
     */
    public void onOrientationEvent(float azimuth, float pitch, float roll) {
        if (riddleAvailable() && requiresOrientationSensor() && mRiddleGame.onOrientationEvent(azimuth, pitch, roll) && mRiddleView != null) {
            mRiddleView.draw();
        }
    }

    private boolean riddleAvailable() {
        return mRiddleGame != null && mRiddleGame.isNotClosed();
    }

    /**
     * Invoked when the RiddleView got visible and valid.
     * @param riddleView
     */
    public final void onRiddleVisible(RiddleView riddleView) {
        mRiddleView = riddleView;
        onRiddleGotVisible();
    }

    // this is overwritten if we don't want the manager to know of this riddle

    /**
     * The riddle just got visible, by default tell the manager this happened.
     */
    protected void onRiddleGotVisible() {
        RiddleInitializer.INSTANCE.getRiddleManager().onUnsolvedRiddle(mRiddle); // especially important that, if saving this riddle when finished excludes the image from the list since saving is async
    }

    /**
     * Get the id of the currently used riddle. Not necessarily a valid id for newly created riddles!
     * @return The riddle id. Can be an invalid id!
     */
    public long getRiddleId() {
        return mRiddle.getId();
    }

    /**
     * If any valid game, check if its type requires the orientation sensor.
     * @return If the orientation sensor is required.
     */
    public boolean requiresOrientationSensor() {
        return riddleAvailable() && mRiddle.getType().requiresOrientationSensor();
    }

    /**
     * Pause the periodic event, stopping future invocations and periodic renderings.
     */
    public void pausePeriodicEvent() {
        if (mRenderThread != null) {
            mRenderThread.stopRendering();
            mRenderThread = null;
        }
        if (mPeriodicThread != null) {
            mPeriodicThread.stopPeriodicEvent();
            mPeriodicThread = null;
        }
    }

    /**
     * If there is a valid riddle and a positive periodic event period, resume (or restart) the rendering and periodic threads.
     */
    public void resumePeriodicEventIfRequired() {
        if (riddleAvailable() && mRiddleView != null && mRiddleGame.getPeriodicEventPeriod() > 0L) {
            pausePeriodicEvent();
            mRenderThread = new GameRenderThread(mRiddleView);
            mRenderThread.setFramePeriod(mRiddleGame.getFramePeriod());
            mRenderThread.setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
            mRenderThread.startRendering();
            mPeriodicThread = new GamePeriodicThread(this);
            mPeriodicThread.setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
            mPeriodicThread.startPeriodicEvent();
        }
    }

    /**
     * If the type requires orientation sensor but the device does not supply (all) required sensors, the game is told so
     * and can enable an alternative to the orientation sensor if possible.
     */
    public void enableNoOrientationSensorAlternative() {
        if (riddleAvailable()) {
            mRiddleGame.enableNoOrientationSensorAlternative();
        }
    }

    /**
     * Returns the type of controller's Riddle.
     * @return
     */
    public PracticalRiddleType getRiddleType() {
        return mRiddle.getType();
    }

    /**
     * Returns the image's hash of the controller's Riddle ('s image).
     * @return
     */
    public String getImageHash() {
        return mRiddle.getImageHash();
    }

    /**
     * Only meaningful if there is a valid game initialized. Returns the periodic event period of the RiddleGame.
     * @return
     */
    public long getPeriodicEventPeriod() {
        return riddleAvailable() ? mRiddleGame.getPeriodicEventPeriod() : 1000;
    }

    /**
     * The periodic event happened, forward to the RiddleGame if possible.
     */
    protected void onPeriodicEvent() {
        if (riddleAvailable()) {
            mRiddleGame.onPeriodicEvent();
        }
    }

    public boolean hasRunningRenderThread() {
        return mRenderThread != null && mRenderThread.isRunning();
    }
}
