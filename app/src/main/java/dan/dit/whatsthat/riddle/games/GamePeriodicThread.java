package dan.dit.whatsthat.riddle.games;

import android.util.Log;

/**
 * In case the game requires periodic updates, this is the used thread class. Time
 * slept is fetched form the RiddleController after each periodic event.
 * Created by daniel on 07.05.15.
 */
public class GamePeriodicThread extends Thread {
    private RiddleController mController;
    private boolean mIsRunning;

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
        long startTime;
        while (mIsRunning && !isInterrupted()) {
            startTime = System.currentTimeMillis();
            mController.onPeriodicEvent();
            long sleepTime = mController.getPeriodicEventPeriod() -(System.currentTimeMillis() - startTime);
            try {
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
        Log.d("Riddle","Periodic thread ended.");
    }

    /**
     * Starts the periodic event thread. One time thread, create new one after
     * stopping!
     */
    public void startPeriodicEvent() {
        mIsRunning = true;
        start();
    }

    /**
     * Stops the periodic event thread.
     */
    public void stopPeriodicEvent() {
        mIsRunning = false;
        interrupt();
    }
}
