package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.frames.HitboxFrames;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;

/**
 * Created by daniel on 31.05.15.
 */
public class Actor<HB extends Hitbox> {
    protected HB mHitbox;
    protected HitboxMover mMover;
    protected final Map<Integer, HitboxFrames> mStateFrames = new HashMap<>();
    protected HitboxFrames mCurrentFrames;
    private boolean mActive;

    public Actor(HB hitbox, HitboxMover mover, HitboxFrames defaultFrames) {
        mHitbox = hitbox;
        if (hitbox == null) {
            throw new IllegalArgumentException("No hitbox for actor!");
        }
        setMover(mover);
        mCurrentFrames = defaultFrames;
        if (defaultFrames == null) {
            throw new IllegalArgumentException("No frames given.");
        }
    }

    public void setMover(HitboxMover mover) {
        mMover = mover;
        if (mover == null) {
            throw new IllegalArgumentException("Illegal mover!");
        }
    }

    public boolean update(long updatePeriod) {
        boolean stateChange = mMover.update(mHitbox, updatePeriod);
        if (stateChange) {
            setStateFramesByMoverState();
        }
        mCurrentFrames.update(updatePeriod);
        return stateChange;
    }

    public HB getHitbox() {
        return mHitbox;
    }

    public final boolean isActive() {
        return mActive;
    }

    public final boolean setActive(boolean active) {
        boolean oldState = mActive;
        mActive = active;
        return oldState != mActive;
    }


    public void setStateFramesByMoverState() {
        HitboxFrames oldFrames = mCurrentFrames;
        mCurrentFrames = mStateFrames.get(mMover.getState());
        if (mCurrentFrames == null) {
            mCurrentFrames = oldFrames;
        }
    }

    public void setStateFrames(int state) {
        HitboxFrames oldFrames = mCurrentFrames;
        mCurrentFrames = mStateFrames.get(state);
        if (mCurrentFrames == null) {
            mCurrentFrames = oldFrames;
        }
    }

    public void putStateFrames(int state, HitboxFrames frames) {
        if (frames == null) {
            return;
        }
        mStateFrames.put(state, frames);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (mCurrentFrames != null && mActive) {
            RectF bound = mHitbox.getBoundingRect();
            mCurrentFrames.draw(canvas, bound.left, bound.top, paint);
        }
    }

    public void resetCurrentFrames() {
        mCurrentFrames.reset();
    }
}
