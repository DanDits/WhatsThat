package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 30.05.15.
 */
public abstract class Look {
    protected boolean mVisible;
    protected float mOffsetX;
    protected float mOffsetY;

    public Look() {
        mVisible = true;
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract boolean update(long updatePeriod);

    public abstract void draw(Canvas canvas, float x, float y, Paint paint);

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
