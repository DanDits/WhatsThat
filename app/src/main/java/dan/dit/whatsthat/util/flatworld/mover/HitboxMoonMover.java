package dan.dit.whatsthat.util.flatworld.mover;

import android.graphics.RectF;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 18.06.15.
 */
public class HitboxMoonMover extends  HitboxMover {
    private static final float TWOPI = (float) Math.PI * 2.f;
    public static final float ANGLE_RIGHT = 0.f;
    public static final float ANGLE_TOP = (float) Math.PI / 2.f;
    public static final float ANGLE_LEFT = (float) Math.PI;
    public static final float ANGLE_BOTTOM = (float) Math.PI * 3.f / 2.f;
    public static final int STATE_NOT_MOVING = 0;
    public static final int STATE_MOVING = 1;

    private Hitbox mPlanet;
    private long mMoonYear;
    private float mAngleSpeed;
    private float mCurrAngle; // 0 is right, mathematically positive direction (CCW)
    private float mDistanceFromCenter;
    private boolean mClockwise;
    private float mOffset;

    public HitboxMoonMover(Hitbox planet, long moonYear, float offset) {
        mPlanet = planet;
        mOffset = offset;
        setMoonYear(moonYear);
        updateDistanceFromCenter(null);
    }

    private void updateDistanceFromCenter(Hitbox toMove) {
        RectF bounds = mPlanet.getBoundingRect();
        mDistanceFromCenter = Math.max(bounds.width(), bounds.height()) / 2.f;
        mDistanceFromCenter += mOffset;
        if (toMove != null) {
            bounds = toMove.getBoundingRect();
            mDistanceFromCenter += Math.max(bounds.width(), bounds.height()) / 2.f;
        }
    }

    public void setMoonYear(long moonYear) {
        mMoonYear = moonYear;
        mAngleSpeed = TWOPI / moonYear;
    }

    public long getMoonYear() {
        return mMoonYear;
    }

    @Override
    public float getSpeed() {
        return mAngleSpeed * mDistanceFromCenter;
    }

    @Override
    public float getAcceleration() {
        return 0;
    }

    @Override
    public int getState() {
        return mAngleSpeed != 0 ? STATE_MOVING : STATE_NOT_MOVING;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        updateDistanceFromCenter(toMove);
        mCurrAngle += (mClockwise ? 1.f : -1.f) * mAngleSpeed * updatePeriod;
        toMove.setCenter(mPlanet.getCenterX() + mDistanceFromCenter * (float) Math.cos(mCurrAngle), mPlanet.getCenterY() + mDistanceFromCenter * (float) Math.sin(mCurrAngle));

        return false;
    }

    @Override
    public boolean isMoving() {
        return mAngleSpeed == STATE_MOVING;
    }

    public void invertDirection() {
        mClockwise = !mClockwise;
    }

    public void setAngle(float angle) {
        this.mCurrAngle = angle;
    }
}
