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
 * Created by daniel on 30.05.15.
 */
public abstract class HitboxMover {
    static final float ONE_SECOND = 1000.f; //ms

    public abstract float getSpeed();
    public abstract float getAcceleration();
    public abstract int getState();

    /**
     * Periodially updates the mover, moving the given hitbox. The kind of moving
     * is defined by the mover, the impact by the fraction of the given update period.
     * @param toMove The hitbox to move.
     * @param updatePeriod The time in miliseconds that passed.
     * @return A hint if updating resulted in an internal state change of the mover. Else false.
     */
    public abstract boolean update(Hitbox toMove, long updatePeriod);
    public abstract boolean isMoving();

}
