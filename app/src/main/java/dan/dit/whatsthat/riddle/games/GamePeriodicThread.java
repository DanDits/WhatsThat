package dan.dit.whatsthat.riddle.games;

import android.util.Log;

/**
 * In case the game requires periodic updates, this is the used thread class. Time
 * slept is fetched form the RiddleController after each periodic event.
 * Stuffing drawing queue, see http://source.android.com/devices/graphics/architecture.html
 * Created by daniel on 07.05.15.
 */
public class GamePeriodicThread extends Thread {
    private RiddleController mController;
    private boolean mIsRunning;
    private boolean mStopped;

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
        while (mIsRunning && !isInterrupted()) {
            mController.onPeriodicEvent();
        }
        mStopped = true;
        Log.d("Riddle", "Periodic thread ended.");
    }

    /**
     * Starts the periodic event thread. One time thread, create new one after
     * stopping!
     */
    public void startPeriodicEvent() {
        mIsRunning = true;
        mStopped = false;
        start();
    }

    /**
     * Stops the periodic event thread.
     */
    public void stopPeriodicEventAndWaitForStop() {
        mIsRunning = false;
        interrupt();
        while (!mStopped) {
            // wait till last periodic cycle finished, will not take long
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // continue
            }
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }
}
