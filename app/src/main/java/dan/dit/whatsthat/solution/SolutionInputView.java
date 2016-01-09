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

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import dan.dit.whatsthat.system.NoPanicDialog;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputView extends View {

    private static final float SWIPE_MAX_OFF_PATH = 60;
    private static final float SWIPE_MIN_DISTANCE = 10;
    private static final float SWIPE_THRESHOLD_VELOCITY = 100;
    private SolutionInput mInput;
    private GestureDetector mGestures;

    public SolutionInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestures = new GestureDetector(getContext(), new MyGestureDetector());
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return mGestures.onTouchEvent(event);
            }
        });
    }

    public void setSolutionInput(@Nullable SolutionInput solutionInput, SolutionInputListener listener) {
        clearListener();
        mInput = solutionInput;
        if (solutionInput != null) {
            clearAnimation();
            solutionInput.setListener(listener);
            solutionInput.getLayout().calculateLayout(getWidth(), getHeight(), getContext()
                    .getResources()
                    .getDisplayMetrics());
        }
        invalidate();
        requestLayout();
    }

    public void clearListener() {
        SolutionInput input = mInput;
        if (input != null) {
            input.setListener(null);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        SolutionInput input = mInput;
        if (input != null) {
            input.getLayout().draw(canvas);
        }
    }

    public void supplyNoPanicParams(Bundle args) {
        if (mInput == null || args == null) {
            return;
        }
        SolutionInput resettedCopy;
        try {
            resettedCopy = SolutionInputManager.reconstruct(new Compacter(mInput
                    .compact()));
            resettedCopy.reset();
        } catch (CompactedDataCorruptException e) {
            Log.e("Riddle", "Data corrupt when supplying no panic params: " + e);
            resettedCopy = mInput; // use standard, showing everything the user entered
        }
        args.putString(NoPanicDialog.KEY_SOLUTION_INPUT_DATA, resettedCopy.compact());
    }

    public void provideHint(int hintLevel) {
        if (mInput == null) {
            return;
        }
        if (mInput.provideHint(hintLevel)) {
            invalidate();
            requestLayout();
        }
    }

    public int getProvidedHintLevel() {
        return mInput.getProvidedHintLevel();
    }


    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            // right to left swipe
            if(Math.abs(e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                SolutionInput input = mInput;
                if (input != null && input.onFling(e1, e2, velocityX, velocityY)) {
                    invalidate ();
                    requestLayout();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN && mInput != null && mInput.onUserTouchDown(e.getX(), e.getY())) {
                invalidate ();
                requestLayout();
            }
            return true;
        }
    }
}


