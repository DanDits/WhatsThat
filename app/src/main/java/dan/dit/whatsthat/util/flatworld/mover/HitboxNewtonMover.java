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
 * Created by daniel on 31.05.15.
 */
public class HitboxNewtonMover extends HitboxMover {
    private static final int STATE_NOT_MOVING = 0;
    private static final int STATE_MOVING = 1;
    private static final int STATE_MOVING_ACCELERATING = 2;
    float mSpeedX;
    float mSpeedY;
    float mAccelX;
    float mAccelY;

    public HitboxNewtonMover() {
    }

    public HitboxNewtonMover(float speedX, float speedY) {
        setSpeed(speedX, speedY);
    }

    /**
     * Creates a new newton mover that moves the given delta with the given speed.
     * The speed is in units per second! Take care that this will not stop the mover after
     * exceeding the given delta, if this is wanted stop it after timeout defined by distance
     * and the given speed.
     * @param deltaX The x delta defining the x speed together with the given total speed.
     * @param deltaY The y delta defining the y speed together with the given total speed.
     * @param speed The total speed of the mover.
     */
    public HitboxNewtonMover(float deltaX, float deltaY, float speed) {
        double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float time = (float) (dist / speed);
        setSpeed(deltaX / time, deltaY / time);
    }

    public void setSpeed(float speedX, float speedY) {
        mSpeedX = speedX;
        mSpeedY = speedY;
    }

    public void setAcceleration(float accelX, float accelY) {
        mAccelX = accelX;
        mAccelY = accelY;
    }

    public void multiplySpeed(float multiX, float multiY) {
        mSpeedX *= multiX;
        mSpeedY *= multiY;
    }

    @Override
    public float getSpeed() {
        return (float) Math.sqrt(mSpeedX * mSpeedX + mSpeedY * mSpeedY);
    }

    @Override
    public float getAcceleration() {
        return (float) Math.sqrt(mAccelX * mAccelX + mAccelY * mAccelY);
    }

    @Override
    public int getState() {
        return (mSpeedX == mSpeedY && mSpeedX == 0.f) ? STATE_NOT_MOVING : (mAccelX == mAccelY && mAccelX == 0.f) ? STATE_MOVING : STATE_MOVING_ACCELERATING;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        float updateFraction = updatePeriod / ONE_SECOND;
        mSpeedX += mAccelX * updateFraction;
        mSpeedY += mAccelY * updateFraction;
        toMove.move(mSpeedX * updateFraction, mSpeedY * updateFraction);
        return false; // internal state changes that the client does not know about are highly unlikely (would be that acceleration stops movement to zero)
    }

    @Override
    public boolean isMoving() {
        return mSpeedX != 0 || mSpeedY != 0 || mAccelX != 0 || mAccelY != 0;
    }

    public float getSpeedX() {
        return mSpeedX;
    }

    public float getSpeedY() {
        return mSpeedY;
    }
}
