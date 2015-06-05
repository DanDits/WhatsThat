package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

import dan.dit.whatsthat.util.flatworld.HitboxFrames;

/**
 * Created by daniel on 30.05.15.
 */
public class HitboxRect extends Hitbox<HitboxRect> {
    private float mLeft;
    private float mTop;
    private float mBottom;
    private float mRight;

    public HitboxRect(final float left, final float top, final float width, final float height) {
        mLeft = left;
        mTop = top;
        mRight = left + width;
        mBottom = top + height;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    @Override
    public boolean checkCollision(HitboxRect other) {
        return mRight >= other.mLeft && mLeft <= other.mRight && mBottom >= other.mTop && mTop <= other.mBottom;
    }

    @Override
    public RectF getBoundingRect() {
        return mBoundingRect;
    }

    @Override
    public boolean isInside(float x, float y) {
        return x >= mLeft && x <= mRight && y >= mTop && y <= mBottom;
    }

    @Override
    public boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect) {
        float rx = getRandomFloatInRange(Math.max(pointInRect.left, mLeft), Math.min(pointInRect.right, mRight), rand);
        float ry = getRandomFloatInRange(Math.max(pointInRect.top, mTop), Math.min(pointInRect.bottom, mBottom), rand);
        return other.isInside(rx, ry);
    }

    @Override
    public void move(float x, float y) {
        mLeft += x;
        mRight += x;
        mTop += y;
        mBottom += y;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    @Override
    public void setTop(float newTop) {
        mBottom = newTop + mBottom - mTop;
        mTop = newTop;
        mBoundingRect.top = mTop;
        mBoundingRect.bottom = mBottom;
    }

    @Override
    public void setLeft(float newLeft) {
        mRight = newLeft + mRight - mLeft;
        mLeft = newLeft;
        mBoundingRect.right = mRight;
        mBoundingRect.left = mLeft;
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        float width = mRight - mLeft;
        float height = mBottom - mTop;
        mTop = centerY - height / 2.f;
        mLeft = centerX - width / 2.f;
        mRight = mLeft + width;
        mBottom = mTop + height;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    public static HitboxRect makeHitbox(HitboxFrames frames, float hitboxWidthFraction, float hitboxHeightFraction, float frameLeft, float frameTop) {
        float offsetX = -frames.getWidth() * (1 - hitboxWidthFraction) / 2.f;
        float offsetY = -frames.getHeight() * (1 - hitboxHeightFraction) / 2.f;
        frames.setOffset(offsetX, offsetY);
        return new HitboxRect(frameLeft -  offsetX, frameTop - offsetY, frames.getWidth() * hitboxWidthFraction, frames.getHeight() * hitboxHeightFraction);
    }
}
