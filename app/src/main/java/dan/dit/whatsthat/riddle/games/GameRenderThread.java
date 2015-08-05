package dan.dit.whatsthat.riddle.games;

import android.util.Log;

import dan.dit.whatsthat.riddle.RiddleView;

/**
 * Like the GamePeriodicThread this thread is performing rendering, especially drawing
 * of bitmaps on the final view canvas. The drawing is done as fast as possible, limited
 * by the drawing of the riddle, so it is slowed down by the rendering queue, no explicit
 * frame rate is used (see http://stackoverflow.com/questions/21838523/annoying-lags-stutters-in-an-android-game/22387533#22387533)
 * Created by daniel on 07.05.15.
 */
public class GameRenderThread extends Thread {


    private final RiddleView mRiddleView;
    private boolean mIsRunning = false;

    /**
     * Creates a new GameRenderThread that will periodically draw the riddle
     * after the thread has been started.
     * @param view
     */
    public GameRenderThread(RiddleView view) {
        mRiddleView = view;
    }

    @Override
    public void run() {
        //long startTime;
        while (mIsRunning && !isInterrupted()) {
            //startTime = System.nanoTime();
            mRiddleView.performDrawRiddle();

            /*//restart periodic event after period
            long sleepTime = mFramePeriod -(System.nanoTime() - startTime);

            try {
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                interrupt();
            }*/
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
