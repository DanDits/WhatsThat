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
 * Created by daniel on 26.06.15.
 */
public class HitboxAttachedMover extends HitboxMover {

    private final Hitbox mAttachedTo;

    public HitboxAttachedMover(Hitbox attachTo) {
        mAttachedTo = attachTo;
        if (mAttachedTo == null) {
            throw new IllegalArgumentException("Cannot attack to null hitbox.");
        }
    }

    @Override
    public float getSpeed() {
        return 0.f;
    }

    @Override
    public float getAcceleration() {
        return 0.f;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        toMove.setCenter(mAttachedTo.getCenterX(), mAttachedTo.getCenterY());
        return false;
    }

    @Override
    public boolean isMoving() {
        return false;
    }
}
