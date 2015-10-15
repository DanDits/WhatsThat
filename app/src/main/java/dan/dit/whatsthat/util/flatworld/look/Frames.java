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

package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

/**
 * Created by daniel on 26.06.15.
 */
public class Frames extends Look {
    private Bitmap[] mFrames;
    int mFrameIndex;
    private long mFrameCounter;
    private long mFrameDuration;

    public Frames(Bitmap[] frames, long frameDuration) {
        mFrames = frames;
        mFrameCounter = 0;
        mFrameIndex = 0;
        mFrameDuration = frameDuration;
        if (mFrames == null || mFrames.length == 0) {
            throw new IllegalArgumentException("No frames given.");
        }
        if (frames.length > 1 && mFrameDuration <= 0L) {
            Log.e("Riddle", "Illegal frame duration set to 1000ms " + mFrameDuration);
            mFrameDuration = 1000L;
        }
    }

    @Override
    public int getWidth() {
        return mFrames[0].getWidth();
    }

    @Override
    public int getHeight() {
        return mFrames[0].getHeight();
    }

    public int getCount() {
        return mFrames.length;
    }

    @Override
    public boolean update(long updatePeriod) {
        if (mFrames.length > 1) {
            mFrameCounter += updatePeriod;
            if (mFrameCounter > mFrameDuration) {
                mFrameCounter -= mFrameDuration;
                mFrameIndex++;
                mFrameIndex %= mFrames.length;
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, Paint paint) {
        if (mVisible) {
            Bitmap currFrame = mFrames[mFrameIndex];
            canvas.drawBitmap(currFrame, x - currFrame.getWidth() + getWidth() + mOffsetX, y - currFrame.getHeight() + getHeight() + mOffsetY, paint);
        }
    }

    @Override
    public void reset() {
        mFrameIndex = 0;
        mFrameCounter = 0L;
    }

    public Bitmap[] getFrames() {
        return mFrames;
    }
}
