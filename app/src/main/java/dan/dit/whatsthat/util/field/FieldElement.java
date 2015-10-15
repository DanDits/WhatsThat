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

package dan.dit.whatsthat.util.field;

import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * FieldElements are elements inside A Field2D. The elements are organised in a
 * grid with the (0,0) position being the top left corner. By default FieldElements
 * are equal if they are at the same grid position. Subclass the FieldElement and use it
 * as a generic for the field to customize data hold by each element.
 * Created by daniel on 29.05.15.
 */
public class FieldElement {

    public enum Neighbor {
        LEFT(-1, 0), TOP(0, -1), RIGHT(1, 0), BOTTOM(0, 1), TOP_LEFT(-1, -1), TOP_RIGHT(1, -1), BOTTOM_LEFT(-1,1), BOTTOM_RIGHT(1, 1), SELF(0, 0);
        int mXDelta;
        int mYDelta;

        Neighbor(int xDelta, int yDelta) {
            mXDelta = xDelta;
            mYDelta = yDelta;
        }

        public float getXDelta() {
            return mXDelta;
        }

        public float getYDelta() {
            return mYDelta;
        }
    }

    public static final Neighbor[] DIRECT_NEIGHBORS = new Neighbor[] {Neighbor.LEFT, Neighbor.TOP, Neighbor.RIGHT, Neighbor.BOTTOM};
    public static final Neighbor[] DIRECT_AND_DIAGONAL_NEIGHBORS = new Neighbor[] {Neighbor.LEFT, Neighbor.TOP, Neighbor.RIGHT, Neighbor.BOTTOM,
        Neighbor.TOP_LEFT, Neighbor.TOP_RIGHT, Neighbor.BOTTOM_LEFT, Neighbor.BOTTOM_RIGHT};

    int mPathfindingValue;
    protected int mX;
    protected int mY;

    public boolean isBlocked() {
        return false;
    }

    public void draw(Canvas canvas, Rect fieldRect) {
    }

    public static boolean areNeighbors(FieldElement field1, FieldElement field2, Neighbor[] neighborTypes) {
        int xDelta = field2.mX - field1.mX;
        int yDelta = field2.mY - field1.mY;
        for (Neighbor n : neighborTypes) {
            if (xDelta == n.mXDelta && yDelta == n.mYDelta) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof  FieldElement) {
            return mX == ((FieldElement) other).mX && mY == ((FieldElement) other).mY;
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return mX + (mX + mY) * (mX + mY + 1) / 2; // cantor's bijection
    }
}
