package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

/**
 * Created by daniel on 26.06.15.
 */
public class HitboxGhostPoint extends Hitbox {

    private float mX;
    private float mY;
    private RectF mBounds;

    public HitboxGhostPoint(float x, float y) {
        mX = x;
        mY = y;
        mBounds = new RectF(mX, mY, mX, mY);
    }

    @Override
    public RectF getBoundingRect() {
        return mBounds;
    }

    @Override
    public boolean isInside(float x, float y) {
        return mX == x && mY == y;
    }

    @Override
    public boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect) {
        return other.isInside(mX, mY);
    }

    private void updateBounds() {
        mBounds.left = mX;
        mBounds.top = mY;
        mBounds.right = mX;
        mBounds.bottom = mY;
    }

    @Override
    public void move(float deltaX, float deltaY) {
        mX += deltaX;
        mY += deltaY;
        updateBounds();
    }

    @Override
    public void setTop(float top) {
        mY = top;
        updateBounds();
    }

    @Override
    public void setLeft(float left) {
        mX = left;
        updateBounds();
    }

    @Override
    public void setRight(float right) {
        mX = right;
        updateBounds();
    }

    @Override
    public void setBottom(float bottom) {
        mY = bottom;
        updateBounds();
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        mX = centerX;
        mY = centerY;
        updateBounds();
    }

    @Override
    public float getCenterX() {
        return mX;
    }

    @Override
    public float getCenterY() {
        return mY;
    }

    @Override
    public CollisionTester getCollisionTester() {
        return CollisionTester.NO_COLLISION;
    }

    @Override
    public int accept(CollisionTester tester) {
        return tester.collisionTest(this);
    }

}
