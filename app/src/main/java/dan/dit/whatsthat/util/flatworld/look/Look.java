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

package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;

/**
 * A look is some visual representation of an Actor in a FlatWorld. Usually it matches
 * the actor's hitbox. The look can be offset, set visible and be reset. The behavior for these methods
 * is up to the implementation. The look will be updated periodically with a small update period and required
 * to draw itself on the given canvas to accurately match the actor's position in the world. By default a look
 * will be visible.
 * Created by daniel on 30.05.15.
 */
public abstract class Look {
    boolean mVisible;
    float mOffsetX;
    float mOffsetY;

    protected Look() {
        mVisible = true;
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract boolean update(long updatePeriod);

    /**
     * Draws this look on the given canvas. It is up to the look to decide
     * what and if to draw if the look is invisible.
     * @param canvas The canvas to draw onto.
     * @param x The x coordinate of the top left corner.
     * @param y The y coordinate of the top left corner.
     * @param paint Suggested paint to use, can be null.
     */
    public abstract void draw(Canvas canvas, float x, float y, @Nullable Paint paint);

    /**
     * Sets the visibility of the look. The behavior for drawing is implementation dependant.
     * @param visible If the look is set to be visible.
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public abstract void reset();

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }
}
