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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.github.johnpersano.supertoasts.SuperToast;
import com.plattysoft.leonids.ParticleSystem;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectToast;

/**
 * A riddle controller is the class between the RiddleView and the RiddleGame. If closed the controller can
 * no longer be used, it is directly bound to the lifecycle of a RiddleGame.
 * Created by daniel on 05.04.15.
 */
public class RiddleController implements RiddleAnimationController.OnAnimationCountChangedListener {
    private volatile RiddleGame mRiddleGame;
    private Riddle mRiddle;
    private ViewGroup mRiddleViewContainer;
    private volatile RiddleView mRiddleView;
    private GamePeriodicThread mPeriodicThread;
    private RiddleAnimationController mRiddleAnimationController;
    private Handler mMainHandler;
    private GameHandlerThread mGameThread;
    private final Runnable mDrawAction;
    private final Runnable mPeriodicAction;
    private volatile int mPeriodActionPostedCount;

    /**
     * Initializes the RiddleController with the RiddleGame that decorates the given Riddle.
     * @param riddleGame The game that decorates the Riddle parameter.
     * @param riddle The riddle decorated by the game.
     */
    RiddleController(@NonNull RiddleGame riddleGame, @NonNull Riddle riddle) {
        mRiddleGame = riddleGame;
        mRiddle = riddle;
        mRiddleAnimationController = new RiddleAnimationController(this);
        mDrawAction = new Runnable() {
            @Override
            public void run() {
                if (mRiddleView != null) {
                    mRiddleView.draw();
                }
            }
        };
        mPeriodicAction = new Runnable() {

            private long mMissingUpdateTime; // will only be zero at start for first game controlled
            @Override
            public void run() {
                long requiredDrawingTime = mRiddleView.performDrawRiddle();
                long updateTime = mMissingUpdateTime + requiredDrawingTime;
                long periodicEventStartTime = System.nanoTime();
                if (updateTime > 0) {
                    mRiddleGame.onPeriodicEvent(updateTime);
                    mRiddleAnimationController.update(updateTime);
                }
                mMissingUpdateTime = (System.nanoTime() - periodicEventStartTime) / 1000000;
                --mPeriodActionPostedCount;
            }
        };
    }

    private class GameHandlerThread extends HandlerThread {
        private Handler mHandler;
        public GameHandlerThread() {
            super("GameHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        public void onMotionEvent(MotionEvent event) {
            final MotionEvent eventCopy = MotionEvent.obtain(event);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRiddleGame != null && mRiddleGame.onMotionEvent(eventCopy)) {
                        mMainHandler.post(mDrawAction);
                    }
                }
            });
        }

        public void onOrientationEvent(final float azimuth, final float pitch, final float roll) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRiddleGame != null && mRiddleGame.onOrientationEvent(azimuth, pitch,
                            roll)) {
                        mMainHandler.post(mDrawAction);
                    }
                }
            });
        }

        public void onPeriodicEvent() {
            if (mPeriodActionPostedCount == 0) {
                ++mPeriodActionPostedCount;
                mHandler.post(mPeriodicAction);
            }
        }

        public void onCloseRiddle(final Context context) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //at this point we can be sure that the looper doesn't currently process a
                    // periodic event which could lead to concurrency issues
                    mGameThread.quit(); // do not process any more actions!
                    // stop periodic event in a safe way, as soon as it is stopped really close
                    // riddle in the main ui thread
                    stopPeriodicEvent(new Runnable() {
                        @Override
                        public void run() {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (riddleAvailable()) {
                                        onPreRiddleClose();
                                        mRiddleAnimationController.clear();
                                        mRiddleGame.close();
                                        mRiddleGame = null;
                                        onRiddleClosed(context);
                                    }
                                    mRiddleView = null;
                                    mRiddleViewContainer = null;
                                }
                            });
                        }
                    });
                }
            });
        }
    }
    /**
     * The controller is closing, close the riddle and make it save its state. After this method
     * returns the controller is invalid.
     * @param context A context object required for saving state to permanent storage.
     */
    public final void onCloseRiddle(@NonNull final Context context) {
        Log.d("Riddle", "On close riddle.");
        if (mGameThread != null) {
            mGameThread.onCloseRiddle(context);
        }
    }

    /**
     * Invoked on closure of the controller before the RiddleGame's onClose method is invoked.
     */
    void onPreRiddleClose() {
        RiddleManager.addToCache(mRiddle, mRiddleGame.makeSnapshot());
    }

    // this is overwritten if we don't want the manager to know of this riddle and dont want it saved
    /**
     * Invoked on closure of the controller after the RiddleGame's onClose method returned. The RiddleGame is not
     * a valid member anymore. By default saving achievement data and decorated riddle object.
     * @param context The context required to save to permanent storage.
     */
    void onRiddleClosed(final Context context) {
        Log.d("Riddle", "On riddle closed.");
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
            mRiddleAnimationController.draw(canvas, null, RiddleAnimationController
                    .LEVEL_GROUNDING);
            mRiddleAnimationController.draw(canvas, null, RiddleAnimationController
                    .LEVEL_BACKGROUND);
            mRiddleGame.draw(canvas);
            mRiddleAnimationController.draw(canvas, null, RiddleAnimationController
                    .LEVEL_ON_TOP);
        }
    }

    protected void addAnimation(@NonNull RiddleAnimation animation) {
        mRiddleAnimationController.addAnimation(animation);
    }

    protected void addAnimation(@NonNull RiddleAnimation animation, long delay) {
        mRiddleAnimationController.addAnimation(animation, delay);
    }

    /* ************ INPUT RELATED METHODS *********************************************************/

    /**
     * Invoked if there happened some MotionEvent of any kind to the RiddleView.
     * If possible forwards the event to the RiddleGame, redrawing the game if onMotionEvent suggests so.
     * @param event The event to forward.
     */
    public void onMotionEvent(MotionEvent event) {
        if (riddleAvailable()) {
            mGameThread.onMotionEvent(event);
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
        if (riddleAvailable() && requiresOrientationSensor()) {
            mGameThread.onOrientationEvent(azimuth, pitch, roll);
        }
    }

    private boolean riddleAvailable() {
        return mRiddleGame != null && mRiddleGame.isNotClosed();
    }

    /**
     * Invoked when the RiddleView got visible and valid. On the UI thread.
     * @param riddleViewContainer The container that contains the valid RiddleView
     */
    public final void onRiddleVisible(@NonNull ViewGroup riddleViewContainer) {
        mRiddleViewContainer = riddleViewContainer;
        mRiddleView = (RiddleView) mRiddleViewContainer.findViewById(R.id.riddle_view);
        mMainHandler = new Handler();
        mGameThread = new GameHandlerThread();
        mRiddleGame.onGotVisible();
        onRiddleGotVisible();
    }

    // this is overwritten if we don't want the manager to know of this riddle

    /**
     * The riddle just got visible, by default tell the manager this happened.
     */
    void onRiddleGotVisible() {
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
        return riddleAvailable() && mRiddleGame.requiresOrientationSensor();
    }

    /**
     * Pause the periodic event, stopping future invocations and periodic renderings.
     */
    private synchronized void stopPeriodicEvent(final Runnable toExecute) {
        if (mPeriodicThread != null && mPeriodicThread.isRunning()) {
            Log.d("Riddle", "Stopping periodic event that is running.");
            mPeriodicThread.stopPeriodicEvent(new Runnable() {
                @Override
                public void run() {
                    if (toExecute != null) {
                        toExecute.run();
                    }
                    onPeriodicThreadStopped();
                }
            });
        } else if (toExecute != null) {
            Log.d("Riddle", "Stopping periodic event that was not running.");
            toExecute.run();
        }
    }

    public synchronized void stopPeriodicEvent() {
        stopPeriodicEvent(null);
    }

    private void resumePeriodicEventExecute() {
        mPeriodicThread = new GamePeriodicThread(RiddleController.this);
        mPeriodicThread.setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
        mPeriodicThread.startPeriodicEvent();
    }

    // invoked on ui thread. periodic event stopped completely, any other code to execute after
    // stopping was executed, so check if there is still a riddle available before doing stuff to
    // riddle or periodic thread
    private synchronized void onPeriodicThreadStopped() {
        onAnimationCountChanged(); // check again if we need to resume the periodic thread, can
        // be relevant when the thread is about to be stopped when another animation is added
    }

    /**
     * If there is a valid riddle and a positive periodic event period, resume (or restart) the rendering and periodic threads.
     */
    public synchronized void resumePeriodicEventIfRequired() {
        if (mRiddleGame != null && mRiddleGame.requiresPeriodicEvent()) {
            resumePeriodicEvent();
        }
    }

    private synchronized void resumePeriodicEvent() {
        if (riddleAvailable() && mRiddleView != null) {
            if (mPeriodicThread == null || !mPeriodicThread.isRunning()) {
                // if thread is not running yet or not anymore, (re)start.
                // use runnable that is posted by previous running thread, if any, to the ui
                // thread to ensure that no concurrency issues can appear
                stopPeriodicEvent(new Runnable() {
                    @Override
                    public void run() {
                        resumePeriodicEventExecute();
                    }
                });
            }
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
     * @return The type of the current riddle.
     */
    public PracticalRiddleType getRiddleType() {
        return mRiddle.getType();
    }

    /**
     * Returns the image's hash of the controller's Riddle ('s image).
     * @return The hash of the current riddle.
     */
    public String getImageHash() {
        return mRiddle.getImageHash();
    }

    /**
     * The periodic event happened, forward to the RiddleGame if possible.
     */
    void onPeriodicEvent() {
        if (riddleAvailable()) {
            mGameThread.onPeriodicEvent();
        }
    }

    public boolean hasRunningPeriodicThread() {
        return mPeriodicThread != null && mPeriodicThread.isRunning();
    }

    public void checkParty(@NonNull Resources res, @NonNull RiddleView.PartyCallback callback) {
        TestSubjectToast toast = new TestSubjectToast(Gravity.CENTER, 0, 0, mRiddle.getType().getIconResId(), 0, SuperToast.Duration.MEDIUM);
        toast.mAnimations = SuperToast.Animations.POPUP;
        toast.mBackgroundColor = res.getColor(R.color.main_background);

        String[] candies = res.getStringArray(TestSubject.getInstance().getRiddleSolvedResIds());
        RiddleScore riddleScore = mRiddleGame.calculateGainedScore();
        int score = riddleScore.getTotalScore();
        int party = riddleScore.getBonus();

        StringBuilder builder = new StringBuilder();
        if (candies != null && candies.length > 0) {
            builder.append(candies[(int) (Math.random() * candies.length)]);
        }
        if (score > 0) {
            builder.append(" +")
                    .append(score);
        }
        // for each multiplier add an exclamation mark
        for (int i = 0; i < riddleScore.getMultiplicator() - 1; i++) {
            builder.append("!");
        }
        toast.mText = builder.toString();
        toast.mTextSize = 40;
        callback.giveCandy(toast);
        if (party > 0) {
            callback.doParty(party);
        }
    }

    @Override
    public void onAnimationCountChanged() {
        if (!riddleAvailable() || mRiddleGame.requiresPeriodicEvent()) {
            return;
        }
        int count = mRiddleAnimationController.getActiveAnimationsCount();
        // ensure the following actions take place on ui thread

        if (count == 0) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopPeriodicEvent(new Runnable() {
                        @Override
                        public void run() {
                            if (mRiddleView != null) {
                                mRiddleView.draw();
                            }
                        }
                    });
                }
            });
        } else if (count > 0) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    resumePeriodicEvent();
                }
            });
        }
    }

    public void emitParticles(@NonNull ParticleSystem particleSystem, int emitterX, int emitterY,
                              int particlesPerSecond, int emittingTime) {
        if (mRiddleView != null) {
            ViewGroup parent = (ViewGroup) mRiddleView.getParent();
            particleSystem.setParentViewGroup(parent);
            particleSystem.setIgnorePositionInParent();
            particleSystem.emitHandled(emitterX, emitterY, particlesPerSecond,
                    emittingTime, mMainHandler);
        }
    }
}
