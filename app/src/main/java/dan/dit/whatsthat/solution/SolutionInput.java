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

package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 30.03.15.
 */
public abstract class SolutionInput implements Compactable {
    Solution mSolution;
    SolutionInputListener mListener;



    SolutionInput(Compacter compacted) throws CompactedDataCorruptException {
        unloadData(compacted);
    }

    SolutionInput(Solution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("No solution given.");
        }
        initSolution(solution);
    }

    synchronized void setListener(SolutionInputListener listener) {
        mListener = listener;
    }

    public abstract void reset();
    public abstract int estimateSolvedValue();
    protected abstract void initSolution(@NonNull Solution solution);
    public abstract void draw(Canvas canvas);
    public abstract boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY);
    public abstract boolean onUserTouchDown(float x, float y);
    public abstract @NonNull Solution getCurrentUserSolution();

    public abstract void calculateLayout(float width, float height, DisplayMetrics displayMetrics);
}
