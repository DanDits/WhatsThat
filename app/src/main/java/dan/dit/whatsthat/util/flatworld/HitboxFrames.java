package dan.dit.whatsthat.util.flatworld;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 30.05.15.
 */
public class HitboxFrames {
    private Bitmap[] mFrames;
    private int mFrameIndex;
    private long mFrameCounter;
    private long mFrameDuration;
    private boolean mVisible;
    private float mOffsetX;
    private float mOffsetY;

    public HitboxFrames(Bitmap[] frames, long frameDuration) {
        mFrames = frames;
        mFrameCounter = 0;
        mFrameIndex = 0;
        mFrameDuration = frameDuration;
        if (mFrames == null || mFrames.length == 0) {
            throw new IllegalArgumentException("No frames given.");
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
            canvas.drawBitmap(mFrames[mFrameIndex], x + mOffsetX, y + mOffsetY, paint);
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
}
