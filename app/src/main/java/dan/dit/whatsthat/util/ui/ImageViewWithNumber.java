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

package dan.dit.whatsthat.util.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 01.05.15.
 */
public class ImageViewWithNumber extends ImageView implements ViewWithNumber{
    private String mNumber = "";
    private Paint mPaint = new Paint();
    private Rect mDummyRect = new Rect();
    private float mRelX = 0.5f;
    private float mRelY = 0.5f;

    public ImageViewWithNumber(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setAntiAlias(true);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ImageViewWithNumber,
                0, 0);
        try {
            mPaint.setColor(a.getColor(R.styleable.ImageViewWithNumber_numberColor, Color.BLACK));
            mPaint.setTextSize(a.getDimension(R.styleable.ImageViewWithNumber_numberSize, 20.f));
            mPaint.setFakeBoldText(a.getBoolean(R.styleable.ImageViewWithNumber_numberBold, false));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setNumber(int number) {
        mNumber = String.valueOf(number);
        invalidate();
        requestLayout();
    }

    @Override
    public void setNumberPosition(float relX, float relY) {
        mRelX = relX;
        mRelY = relY;
        invalidate();
        requestLayout();
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawTextCentered(canvas, mNumber, getWidth() * mRelX, getHeight() * mRelY);
    }

    private void drawTextCentered(Canvas canvas, String text, float x, float y) {
        float numberOffsetY = -((mPaint.descent() + mPaint.ascent()) / 2);
        mPaint.getTextBounds(text, 0, text.length(), mDummyRect);
        canvas.drawText(text, x - mDummyRect.exactCenterX(), y + numberOffsetY, mPaint);
    }
}
