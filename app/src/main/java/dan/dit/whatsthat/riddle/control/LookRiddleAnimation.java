package dan.dit.whatsthat.riddle.control;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.BoolRes;
import android.util.Log;

import dan.dit.whatsthat.util.flatworld.look.Look;

/**
 * Created by daniel on 07.11.15.
 */
public class LookRiddleAnimation extends RiddleAnimation {
    protected final Look mLook;
    private final int mX;
    private final int mY;
    protected long mLifeTime;
    public LookRiddleAnimation(Look look, int x, int y, long lifeTime) {
        super(RiddleAnimationController.LEVEL_ON_TOP);
        mLook = look;
        if (mLook == null) {
            throw new IllegalArgumentException("No look given.");
        }
        mX = x;
        mY = y;
        mLifeTime = lifeTime;
        if (mLifeTime < 0L) {
            mLifeTime = -1;
        }
    }

    @Override
    boolean isAlive() {
        return mLifeTime > 0L || mLifeTime == -1;
    }

    @Override
    public void onMurdered() {
        mLifeTime = 0L;
    }

    @Override
    protected void update(long updatePeriod) {
        mLook.update(updatePeriod);
        if (mLifeTime > 0L) {
            mLifeTime -= updatePeriod;
            if (mLifeTime == -1) {// if we accidentially set lifetime to -1 (life forever)
            // correct it
                mLifeTime = -2;
            }
        }
    }

    @Override
    public void onBorn() {
        super.onBorn();
        Log.d("Riddle", "Look animation born.");
    }

    @Override
    public void onKilled(boolean murdered) {
        super.onKilled(murdered);
        Log.d("Riddle", "Look animation died: " + murdered);
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        mLook.draw(canvas, mX, mY, paint);
    }
}
