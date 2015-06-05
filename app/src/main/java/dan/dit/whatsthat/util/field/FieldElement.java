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
        protected int mXDelta;
        protected int mYDelta;

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

    protected int mPathfindingValue;
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
