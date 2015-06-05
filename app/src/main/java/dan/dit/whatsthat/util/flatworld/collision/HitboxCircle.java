package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

/**
 * Created by daniel on 01.06.15.
 */
public class HitboxCircle extends Hitbox<HitboxCircle> {
    private static final float PI2 = (float) (Math.PI * 2.);
    private float mRadius;
    private float mCenterX;
    private float mCenterY;

    public HitboxCircle(float centerX, float centerY, float radius) {
        mRadius = radius;
        mCenterX = centerX;
        mCenterY = centerY;
        updateBoundingRect();
    }

    private void updateBoundingRect() {
        float halfRadius = mRadius / 2.f;
        mBoundingRect.set(mCenterX - halfRadius, mCenterY - halfRadius, mCenterX + halfRadius, mCenterY + halfRadius);
    }

    public void setRadius(float radius) {
        mRadius = radius;
        updateBoundingRect();
    }

    @Override
    public RectF getBoundingRect() {
        return mBoundingRect;
    }

    @Override
    public boolean isInside(float x, float y) {
        float xD = x - mCenterX;
        float yD = y - mCenterY;
        return xD * xD + yD * yD <= mRadius * mRadius;
    }

    @Override
    public boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect) {
        float rx = getRandomFloatInRange(pointInRect.left, pointInRect.right, rand);
        float y2 = mRadius * mRadius - (rx - mCenterX) * (rx - mCenterX);
        if (y2 < 0.f) {
            return false; //rx outside of bounding rect
        }
        float y = (float) Math.sqrt(y2);
        float yMin = Math.max(mCenterY - y,  pointInRect.top);
        float yMax = Math.min(mCenterY + y, pointInRect.bottom);
        float ry = getRandomFloatInRange(yMin, yMax, rand);
        return other.isInside(rx, ry);
    }

    @Override
    public void move(float deltaX, float deltaY) {
        mCenterX += deltaX;
        mCenterY += deltaY;
        updateBoundingRect();
    }

    @Override
    public void setTop(float top) {
        mCenterY = top + mRadius / 2.f;
        updateBoundingRect();
    }

    @Override
    public void setLeft(float left) {
        mCenterX = left + mRadius / 2.f;
        updateBoundingRect();
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        mCenterX = centerX;
        mCenterY = centerY;
        updateBoundingRect();
    }

    @Override
    public boolean checkCollision(HitboxCircle with) {
        float dx = mCenterX - with.mCenterX;
        float dy = mCenterY - with.mCenterY;
        float r = mRadius + with.mRadius;
        return dx * dx + dy * dy <= r * r;
    }
}
