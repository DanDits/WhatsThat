package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 06.06.15.
 */
public class HitboxNewtonFrictionMover extends HitboxNewtonMover {
    private float mFriction;
    private float mFrictionForUpdatePeriod;
    private float mLastUpdatePeriod = -1L;

    public void setFriction(float friction) {
        mFriction = friction;
        updateFrictionForUpdatePeriod();
    }

    private void updateFrictionForUpdatePeriod() {
        mFrictionForUpdatePeriod = (float) Math.pow(1-mFriction, mLastUpdatePeriod / ONE_SECOND);
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        if (mLastUpdatePeriod != updatePeriod) {
            mLastUpdatePeriod = updatePeriod;
            updateFrictionForUpdatePeriod();
        }
        mAccelX *= mFrictionForUpdatePeriod;
        mAccelY *= mFrictionForUpdatePeriod;
        mSpeedX *= mFrictionForUpdatePeriod;
        mSpeedY *= mFrictionForUpdatePeriod;
        float updateFraction = updatePeriod / ONE_SECOND;
        mSpeedX += mAccelX * updateFraction;
        mSpeedY += mAccelY * updateFraction;
        toMove.move(mSpeedX * updateFraction, mSpeedY * updateFraction);
        return false; // internal state changes that the client does not know about are highly unlikely (would be that acceleration stops movement to zero)
    }

    public float getAccelerationX() {
        return mAccelX;
    }

    public float getAccelerationY() {
        return mAccelY;
    }
}
