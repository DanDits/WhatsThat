package dan.dit.whatsthat.util.flatworld.mover;

import android.util.Log;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 01.06.15.
 */
public class HitboxJumpMover extends HitboxMover {
    public static final int STATE_NOT_MOVING = 0;
    public static final int STATE_JUMP_ASCENDING = 1;
    public static final int STATE_JUMP_FALLING = 2;
    public static final int STATE_LANDED = 3;

    private int mState = STATE_NOT_MOVING;

    private float mSpeedY;
    private float mRemainingAscend;
    private float mRemainingDescend;
    private long mAscendDuration;
    private long mJumpInProgressDuration;
    private long mTotalDuration;
    private float mAccelY;

    /**
     * Initializes a jump in the y direction that will last the given duration. Does nothing
     * if the distance to move is zero. All direction signums can be changed to move in the opposite direction.
     * Speed is constant, no acceleration is done.
     * @param ascendY Ascending movement, that means a positive value will move negative y direction.
     * @param descendY Descending movement after the ascending movement. A positive value will move in the positive y direction.
     * @param duration The positive duration of the total jump. The fraction of ascending distance to total distance will be the time
     *                 taken for ascension.
     */
    public void initJump(float ascendY, float descendY, long duration) {
        float distance = (Math.abs(ascendY) + Math.abs(descendY));
        if (distance == 0.f) {
            return;
        }
        if (duration <= 0L) {
            throw new IllegalArgumentException("Jump duration illegal: " + duration);
        }

        mJumpInProgressDuration = 0L;
        mTotalDuration = duration;
        mRemainingAscend = ascendY;
        mRemainingDescend = descendY;
        mAscendDuration = (long) (Math.abs(ascendY / distance) * duration);
        mSpeedY = -2 * ascendY * ONE_SECOND / mAscendDuration;
        mAccelY = Math.signum(ascendY) * Math.abs(mSpeedY) * ONE_SECOND / mAscendDuration;
        Log.d("Riddle", "Ascend duration " + mAscendDuration + " speedY " + mSpeedY + " total duration " + mTotalDuration + " accelY " + mAccelY + " distance " + ascendY + "+" + descendY);
        mState = STATE_JUMP_ASCENDING;
    }

    @Override
    public float getSpeed() {
        return Math.abs(mSpeedY);
    }

    @Override
    public float getAcceleration() {
        return Math.abs(mAccelY);
    }

    public long getJumpDuration() {
        return mJumpInProgressDuration;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        boolean stateChange = false;
        float dy;
        switch (mState) {
            case STATE_JUMP_ASCENDING:
                mJumpInProgressDuration += updatePeriod;
                mSpeedY += mAccelY * updatePeriod / ONE_SECOND;
                dy = mSpeedY * updatePeriod / ONE_SECOND;
                dy = (mRemainingAscend + dy >= 0.f) ? dy : -mRemainingAscend;
                mRemainingAscend += dy;
                toMove.move(0.f, dy);
                if (mJumpInProgressDuration >= mAscendDuration) {
                    toMove.move(0.f, -mRemainingAscend);
                    mRemainingAscend = 0.f;
                    mState = STATE_JUMP_FALLING;
                    mSpeedY = 0.f;
                    float descendTime = (mTotalDuration - mAscendDuration) / ONE_SECOND;
                    mAccelY = mRemainingDescend * 2 / (descendTime * descendTime);
                    stateChange = true;
                }
                break;
            case STATE_JUMP_FALLING:
                mJumpInProgressDuration += updatePeriod;
                mSpeedY += mAccelY * updatePeriod / ONE_SECOND;
                dy = mSpeedY * updatePeriod / ONE_SECOND;
                dy = (mRemainingDescend - dy >= 0.f) ? dy : mRemainingDescend;
                mRemainingDescend -= dy;
                toMove.move(0.f, dy);
                if (mJumpInProgressDuration >= mTotalDuration) {
                    toMove.move(0.f, mRemainingDescend);
                    mRemainingDescend = 0.f;
                    mState = STATE_LANDED;
                    mSpeedY = 0.f;
                    stateChange = true;
                }
                break;
        }
        return stateChange;
    }

    @Override
    public boolean isMoving() {
        return mState == STATE_JUMP_ASCENDING || mState == STATE_JUMP_FALLING;
    }

    public void stop() {
        mState = STATE_NOT_MOVING;
    }
}
