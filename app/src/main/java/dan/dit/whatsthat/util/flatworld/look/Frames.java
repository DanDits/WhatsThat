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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import dan.dit.whatsthat.util.MathFunction;

/**
 * Created by daniel on 26.06.15.
 */
public class Frames extends Look {
    private Bitmap[] mFrames;
    int mFrameIndex;
    private long mFrameCounter;
    private long mFrameDuration;
    private long[] mFrameDurations;
    private boolean mBlendFrames;
    private Paint mBlendPaint;
    private MathFunction mBlendFunction;

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

    public Frames(Frames toCopy) {
        mFrames = new Bitmap[toCopy.mFrames.length];
        System.arraycopy(toCopy.mFrames, 0, mFrames, 0, mFrames.length);
        mFrameDurations = toCopy.mFrameDurations != null ? new long[toCopy.mFrameDurations
                .length] : null;
        if (toCopy.mFrameDurations != null) {
            System.arraycopy(toCopy.mFrameDurations, 0, mFrameDurations, 0, mFrameDurations.length);
        }
        mFrameCounter = 0;
        mFrameIndex = 0;
        mFrameDuration = toCopy.mFrameDuration;
        if (toCopy.mBlendFrames && toCopy.mBlendPaint != null) {
            mBlendPaint = new Paint(toCopy.mBlendPaint);
            mBlendFrames = true;
            mBlendFunction = toCopy.mBlendFunction;
        }
    }

    public static final int BLEND_MODE_LINEAR = 0;
    public static final int BLEND_MODE_QUADRATIC = 1;

    public Frames setBlendFrames(boolean blendFrames, int blendInterpolation) {
        mBlendFrames = blendFrames;
        if (mBlendFrames) {
            if (blendInterpolation == BLEND_MODE_LINEAR) {
                mBlendFunction = new MathFunction.LinearInterpolation(0., 255, 1.0, 0.);
            } else {
                mBlendFunction = new MathFunction.QuadraticInterpolation(0., 255., 1.0, 0.);
            }
            if (mBlendPaint == null) {
                mBlendPaint = new Paint();
                mBlendPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            }
        } else {
            mBlendPaint = null;
            mBlendFunction = null;
        }
        return this;
    }

    @Override
    public int getWidth() {
        return mFrames[0].getWidth();
    }

    public Frames setFrameDuration(int index, long duration) {
        if (mFrameDurations == null) {
            mFrameDurations = new long[mFrames.length];
        }
        mFrameDurations[index] = duration <= 0L ? 0L : duration;
        return this;
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
            long frameDuration = getCurrentFrameDuration();
            if (mFrameCounter > frameDuration) {
                mFrameCounter -= frameDuration;
                mFrameIndex++;
                mFrameIndex %= mFrames.length;
                return true;
            }
        }
        return false;
    }

    protected boolean performBlending(int frameIndex) {
        return mBlendFrames && mFrames.length > 1;
    }

    private long getCurrentFrameDuration() {
        if (mFrameDurations == null) {
            return mFrameDuration;
        }
        long duration = mFrameDurations[mFrameIndex];
        if (duration <= 0L) {
            return mFrameDuration;
        }
        return duration;
    }
    @Override
    public void draw(Canvas canvas, float x, float y, Paint paint) {
        if (mVisible) {
            int oldAlpha = 255;
            boolean performBlending = performBlending(mFrameIndex);
            int blendingAlpha = 255;
            Bitmap currFrame = mFrames[mFrameIndex];
            if (performBlending) {
                // there is another bitmap and we want to blend to it, depending on time left
                // till its visible
                paint = mBlendPaint;
                oldAlpha = paint.getAlpha();
                blendingAlpha = (int) (mBlendFunction.evaluate(Math.min(mFrameCounter / (double)
                        getCurrentFrameDuration(), 1.)));
                paint.setAlpha(blendingAlpha);
            }

            boolean currentDrawn = false;
            if (currFrame != null && (!performBlending || blendingAlpha < 128)) {
                currentDrawn = true;
                canvas.drawBitmap(currFrame, x - currFrame.getWidth() + getWidth() + mOffsetX, y - currFrame.getHeight() + getHeight() + mOffsetY, paint);
            }
            if (performBlending) {
                Bitmap nextFrame = mFrames[(mFrameIndex + 1) % mFrames.length];
                if (nextFrame != null) {
                    paint.setAlpha(255 - blendingAlpha);
                    canvas.drawBitmap(nextFrame, x - nextFrame.getWidth() + getWidth() + mOffsetX, y - nextFrame.getHeight() + getHeight() + mOffsetY, paint);

                    if (!currentDrawn && currFrame != null) {
                        paint.setAlpha(blendingAlpha);
                        canvas.drawBitmap(currFrame, x - currFrame.getWidth() + getWidth() + mOffsetX, y - currFrame.getHeight() + getHeight() + mOffsetY, paint);
                    }
                }
                paint.setAlpha(oldAlpha);
            }
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
