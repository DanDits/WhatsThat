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

package dan.dit.whatsthat.util;

import android.os.Handler;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by daniel on 20.06.15.
 */
public class DelayedQueueProcessor<T> implements Runnable {
    private final Callback<T> mCallback;
    private Handler mHandler;
    private Queue<T> mObjects;
    private LinkedList<Long> mDelays;
    private boolean mIsRunning;

    public interface Callback<T> {
        long process(T toProcess);
    }

    public DelayedQueueProcessor(Callback<T> callback) {
        mHandler = new Handler();
        mObjects = new LinkedList<>();
        mDelays = new LinkedList<>();
        mCallback = callback;
        if (mCallback == null) {
            throw new IllegalArgumentException("No callback given.");
        }
    }

    public synchronized void start() {
        if (!mIsRunning) {
            mIsRunning = true;
            next(0L);
        }
    }

    private void stop() {
        mIsRunning = false;
    }

    private void next(long minDelay) {
        if (mDelays.isEmpty()) {
            stop();
            return;
        }
        long delay = Math.max(mDelays.poll(), minDelay);
        if (delay > 0L) {
            mHandler.postDelayed(this, delay);
        } else {
            mHandler.post(this);
        }
    }

    public void append(T toAppend, long delayToPrevious) {
        mObjects.add(toAppend);
        mDelays.add(delayToPrevious);
    }


    @Override
    public void run() {
        if (mObjects.isEmpty()) {
            Log.e("HomeStuff", "Run delayed queue with no objects: " + mDelays + " and " + mObjects);
            stop();
            return;
        }
        T current = mObjects.poll();

        //process current element, ensure the next element appears after this one is processed
        long timeToProcess = mCallback.process(current);
        next(timeToProcess);
    }
}
