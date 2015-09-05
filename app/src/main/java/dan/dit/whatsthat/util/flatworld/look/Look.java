package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;

/**
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
