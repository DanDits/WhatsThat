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

package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

/**
 * Created by daniel on 31.05.15.
 */
public abstract class Hitbox {
    RectF mBoundingRect = new RectF();

    public abstract RectF getBoundingRect();


    public abstract boolean isInside(float x, float y);
    public abstract boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect);

    public abstract void move(float deltaX, float deltaY);

    /**
     * Sets the topmost point of the hitbox.
     * @param top The topmost point.
     */
    public abstract void setTop(float top);

    /**
     * Sets the leftmost point of the hitbox to the given value.
     * @param left The leftmost point of the hitbox.
     */
    public abstract void setLeft(float left);

    public abstract void setRight(float right);

    public abstract void setBottom(float bottom);

    /**
     * Sets the center point of the hitbox to the given value.
     * @param centerX The center x value.
     * @param centerY The center y value.
     */
    public abstract void setCenter(float centerX, float centerY);

    static float getRandomFloatInRange(float min, float max, Random random) {
        return min + random.nextFloat() * (max - min);
    }

    public abstract float getCenterX();
    public abstract float getCenterY();

    public abstract CollisionTester getCollisionTester();

    public abstract int accept(CollisionTester collisionTester);
}
