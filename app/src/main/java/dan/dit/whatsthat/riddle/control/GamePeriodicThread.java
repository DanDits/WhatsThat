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
    private volatile boolean mIsRunning;
    private volatile boolean mStopped;
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
