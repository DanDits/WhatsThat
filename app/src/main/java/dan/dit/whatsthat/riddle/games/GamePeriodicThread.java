package dan.dit.whatsthat.riddle.games;

import android.os.Handler;
import android.util.Log;

/**
 * In case the game requires periodic updates, this is the used thread class. Time
 * slept is fetched form the RiddleController after each periodic event.
 * Stuffing drawing queue, see http://source.android.com/devices/graphics/architecture.html
 * Created by daniel on 07.05.15.
 */
class GamePeriodicThread extends Thread {
    private RiddleController mController;
    private transient boolean mIsRunning;
    private transient boolean mStopped;
    private Handler mStoppedNotifier;
    private Runnable mOnStopCallback;

    /**
     * Creates a new GamePeriodicThread which can be started once. It will not be running
     * by default.
     * @param ctr The RiddleController required to perform the periodic event and supply the sleep time.
     */
    public GamePeriodicThread(RiddleController ctr) {
        mController = ctr;
    }

    @Override
    public void run() {
        Log.d("Riddle", "Periodic thread started.");
        while (mIsRunning && !mStopped && !isInterrupted()) {
            mController.onPeriodicEvent();
        }
        mStopped = true;
        if (mStoppedNotifier != null && mOnStopCallback != null) {
            mStoppedNotifier.post(mOnStopCallback);
        }
        Log.d("Riddle", "Periodic thread ended.");
    }

    /**
     * Starts the periodic event thread. One time thread, create new one after
     * stopping!
     */
    public synchronized void startPeriodicEvent() {
        mIsRunning = true;
        mStopped = false;
        start();
    }

    /**
     * Stops the periodic event thread.
     */
    public synchronized void stopPeriodicEvent(Runnable onStopCallback) {
        if (!isRunning()) {
            return;
        }
        mIsRunning = false;
        if (onStopCallback != null) {
            mStoppedNotifier = new Handler();
            mOnStopCallback = onStopCallback;
        }
    }

    public boolean isRunning() {
        return mIsRunning || !mStopped;
    }
}
