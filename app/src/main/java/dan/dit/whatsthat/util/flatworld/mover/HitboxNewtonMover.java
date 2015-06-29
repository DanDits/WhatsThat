package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 31.05.15.
 */
public class HitboxNewtonMover extends HitboxMover {
    public static final int STATE_NOT_MOVING = 0;
    public static final int STATE_MOVING = 1;
    public static final int STATE_MOVING_ACCELERATING = 2;
    protected float mSpeedX;
    protected float mSpeedY;
    protected float mAccelX;
    protected float mAccelY;

    public HitboxNewtonMover() {
    }

    public HitboxNewtonMover(float speedX, float speedY) {
        setSpeed(speedX, speedY);
    }

    public void setSpeed(float speedX, float speedY) {
        mSpeedX = speedX;
        mSpeedY = speedY;
    }

    public void setAcceleration(float accelX, float accelY) {
        mAccelX = accelX;
        mAccelY = accelY;
    }

    public void multiplySpeed(float multiX, float multiY) {
        mSpeedX *= multiX;
        mSpeedY *= multiY;
    }

    @Override
    public float getSpeed() {
        return (float) Math.sqrt(mSpeedX * mSpeedX + mSpeedY * mSpeedY);
    }

    @Override
    public float getAcceleration() {
        return (float) Math.sqrt(mAccelX * mAccelX + mAccelY * mAccelY);
    }

    @Override
    public int getState() {
        return (mSpeedX == mSpeedY && mSpeedX == 0.f) ? STATE_NOT_MOVING : (mAccelX == mAccelY && mAccelX == 0.f) ? STATE_MOVING : STATE_MOVING_ACCELERATING;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        float updateFraction = updatePeriod / ONE_SECOND;
        mSpeedX += mAccelX * updateFraction;
        mSpeedY += mAccelY * updateFraction;
        toMove.move(mSpeedX * updateFraction, mSpeedY * updateFraction);
        return false; // internal state changes that the client does not know about are highly unlikely (would be that acceleration stops movement to zero)
    }

    @Override
    public boolean isMoving() {
        return mSpeedX != 0 || mSpeedY != 0 || mAccelX != 0 || mAccelY != 0;
    }

    public float getSpeedX() {
        return mSpeedX;
    }

    public float getSpeedY() {
        return mSpeedY;
    }
}
