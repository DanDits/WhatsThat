package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 29.06.15.
 */
public class CircleLook extends Look {
    private static final Paint CIRCLE_PAINT = new Paint();
    static {
        CIRCLE_PAINT.setStyle(Paint.Style.FILL);
        CIRCLE_PAINT.setAntiAlias(true);
    }
    private final float mRadius;
    private int mColor;

    public CircleLook(float radius, int color) {
        mRadius = radius;
        mColor = color;
    }

    @Override
    public int getWidth() {
        return (int) (2 * mRadius);
    }

    @Override
    public int getHeight() {
        return (int) (2 * mRadius);
    }

    @Override
    public boolean update(long updatePeriod) {
        return false;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, Paint paint) {
        int oldColor = 0;
        Paint.Style oldStyle = null;
        if (paint != null) {
            oldColor = paint.getColor();
            oldStyle = paint.getStyle();
        } else {
            paint = CIRCLE_PAINT;
        }
        paint.setColor(mColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, mRadius, paint);
        if (paint != CIRCLE_PAINT && paint != null) {
            paint.setColor(oldColor);
            paint.setStyle(oldStyle);
        }


    }

    @Override
    public void reset() {
    }
}
