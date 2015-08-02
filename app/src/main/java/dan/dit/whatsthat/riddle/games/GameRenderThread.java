package dan.dit.whatsthat.riddle.games;

import android.util.Log;

import dan.dit.whatsthat.riddle.RiddleView;

/**
 * Like the GamePeriodicThread this thread is performing rendering, especially drawing
 * of bitmaps on the final view canvas. The frame period can be changed, but this is not really
 * required or recommended.
 * Created by daniel on 07.05.15.
 */
public class GameRenderThread extends Thread {
    protected static final long FRAME_PERIOD = 12L; // about 80 frames per second


    private final RiddleView mRiddleView;
    private long mFramePeriod = FRAME_PERIOD;
    private boolean mIsRunning = false;

    /**
     * Creates a new GameRenderThread that will periodically draw the riddle
     * after the thread has been started.
     * @param view
     */
    public GameRenderThread(RiddleView view) {
        mRiddleView = view;
    }

    /**
     * Sets the frame period to the given period if greater than zero, else to default value.
     * @param period The new frame period.
     */
    public void setFramePeriod(long period) {
        mFramePeriod = period;
        if (mFramePeriod <= 0L) {
            mFramePeriod = FRAME_PERIOD;
        }
    }

    @Override
    public void run() {
        long startTime;
        while (mIsRunning && !isInterrupted()) {
            startTime = System.currentTimeMillis();
            mRiddleView.performDrawRiddle();

            //restart periodic event after period
            long sleepTime = mFramePeriod -(System.currentTimeMillis() - startTime);

            try {
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
        Log.d("Riddle", "Render thread ended.");
    }

    /**
     * Starts the rendering thread. Cannot be restarted when stopped once.
     */
    public void startRendering() {
        mIsRunning = true;
        start();
    }

    /**
     * Stops the rendering thread, no more drawing will be done after the current drawing finishes.
     */
    public void stopRendering() {
        mIsRunning = false;
        interrupt();
    }

    public boolean isRunning() {
        return mIsRunning;
    }
}
