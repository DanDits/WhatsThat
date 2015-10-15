/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;

/**
 * Created by daniel on 31.05.15.
 */
public class Actor {
    private Hitbox mHitbox;
    private HitboxMover mMover;
    private final SparseArray<Look> mStateFrames = new SparseArray<>();
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

    protected void setMover(HitboxMover mover) {
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

    public boolean setStateFrames(int state) {
        Look newLook = mStateFrames.get(state);
        if (newLook != null) {
            mCurrentLook = newLook;
            return true;
        }
        return false;
    }

    public void onLeaveWorld() {}

    public boolean onCollision(Actor collider) {return false;}

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

    public void onReachedEndOfWorld() {

    }
}
