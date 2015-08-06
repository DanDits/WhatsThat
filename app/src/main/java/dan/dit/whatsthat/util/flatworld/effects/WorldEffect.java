package dan.dit.whatsthat.util.flatworld.effects;

import android.graphics.Canvas;
import android.graphics.Paint;

import dan.dit.whatsthat.util.flatworld.look.Look;

/**
 * Created by daniel on 26.06.15.
 */
public abstract class WorldEffect {
    public static final int STATE_TIMEOUT = 0;
    private static final int STATE_RUNNING = 1;
    private static final long DURATION_INFINITE = Long.MAX_VALUE;

    Look mLook;
    private long mDuration;

    int mFadeFrom;
    int mFadeTo;
    long mFadeOffsetDuration;
    long mFadeTime;
    long mFadeTimeTotal;

    WorldEffect(Look look) {
        mLook = look;
        if (mLook == null) {
            throw new IllegalArgumentException("Null frames given to WorldEffect.");
        }
        mDuration = DURATION_INFINITE;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public int update(long updatePeriod) {
        if (mDuration <= 0L) {
            return STATE_TIMEOUT;
        }
        mDuration -= updatePeriod;
        if (mFadeOffsetDuration > 0L) {
            mFadeOffsetDuration -= updatePeriod;
        } else {
            mFadeTime += updatePeriod;
        }
        mLook.update(updatePeriod);
        return getState();
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public void startFade(int fadeFrom, int fadeTo, long fadeTime, long startOffset) {
        mFadeFrom = fadeFrom;
        mFadeTo = fadeTo;
        mFadeTime = 0;
        mFadeTimeTotal = fadeTime;
        mFadeOffsetDuration = startOffset;

    }

    public int getState() {
        return mDuration > 0 ? STATE_RUNNING : STATE_TIMEOUT;
    }
}
