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
