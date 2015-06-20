package dan.dit.whatsthat.util.flatworld.frames;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

/**
 * Created by daniel on 30.05.15.
 */
public class HitboxFrames {
    private Bitmap[] mFrames;
    protected int mFrameIndex;
    private long mFrameCounter;
    private long mFrameDuration;
    protected boolean mVisible;
    protected float mOffsetX;
    protected float mOffsetY;

    public HitboxFrames(Bitmap[] frames, long frameDuration) {
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
        mVisible = true;
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public int getWidth() {
        return mFrames[0].getWidth();
    }

    public int getHeight() {
        return mFrames[0].getHeight();
    }

    public int getCount() {
        return mFrames.length;
    }

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

    public void draw(Canvas canvas, float x, float y, Paint paint) {
        if (mVisible) {
            Bitmap currFrame = mFrames[mFrameIndex];
            canvas.drawBitmap(currFrame, x - currFrame.getWidth() + getWidth() + mOffsetX, y - currFrame.getHeight() + getHeight() + mOffsetY, paint);
        }
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public void reset() {
        mFrameIndex = 0;
        mFrameCounter = 0L;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public Bitmap[] getFrames() {
        return mFrames;
    }
}
