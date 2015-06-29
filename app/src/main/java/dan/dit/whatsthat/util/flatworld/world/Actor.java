package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;

/**
 * Created by daniel on 31.05.15.
 */
public class Actor {
    protected Hitbox mHitbox;
    protected HitboxMover mMover;
    protected final Map<Integer, Look> mStateFrames = new HashMap<>();
    protected Look mCurrentLook;
    private boolean mActive;

    public Actor(Hitbox hitbox, HitboxMover mover, Look defaultLook) {
        mHitbox = hitbox;
        if (hitbox == null) {
            throw new IllegalArgumentException("No hitbox for actor!");
        }
        setMover(mover);
        mCurrentLook = defaultLook;
        if (defaultLook == null) {
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
            onUpdateChangedMoverState();
        }
        mCurrentLook.update(updatePeriod);
        return stateChange;
    }

    protected void onUpdateChangedMoverState() {
    }

    public Hitbox getHitbox() {
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


    public final void setStateFramesByMoverState() {
        Look newLook = mStateFrames.get(mMover.getState());
        if (newLook != null) {
            mCurrentLook = newLook;
        }
    }

    public void setStateFrames(int state) {
        Look newLook = mStateFrames.get(state);
        if (newLook != null) {
            mCurrentLook = newLook;
        }
    }

    public void putStateFrames(int state, Look look) {
        if (look == null) {
            return;
        }
        mStateFrames.put(state, look);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (mCurrentLook != null && mActive) {
            RectF bound = mHitbox.getBoundingRect();
            mCurrentLook.draw(canvas, bound.left, bound.top, paint);
        }
    }

    public void resetCurrentFrames() {
        mCurrentLook.reset();
    }

}
