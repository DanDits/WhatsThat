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

/**
 * Created by daniel on 29.06.15.
 */
public class CircleLook extends Look {
    private static final Paint CIRCLE_PAINT = new Paint();
    static {
        CIRCLE_PAINT.setStyle(Paint.Style.FILL);
        CIRCLE_PAINT.setAntiAlias(true);
    }
    private float mRadius;
    private int mColor;
    private float mOffsetX;
    private float mOffsetY;

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
        canvas.drawCircle(x + mOffsetX + mRadius, y + mOffsetY + mRadius, mRadius, paint);
        if (paint != CIRCLE_PAINT) {
            paint.setColor(oldColor);
            paint.setStyle(oldStyle);
        }


    }

    @Override
    public void reset() {
    }

    public void setRadius(float radius) {
        mRadius = radius;
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public void setColor(int color) {
        mColor = color;
    }
}
