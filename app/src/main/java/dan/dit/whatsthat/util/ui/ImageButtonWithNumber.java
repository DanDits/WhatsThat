package dan.dit.whatsthat.util.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageButton;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 01.05.15.
 */
public class ImageButtonWithNumber extends ImageButton implements ViewWithNumber {
    private String mNumber = "";
    private Paint mPaint = new Paint();
    private Rect mDummyRect = new Rect();
    private float mRelX = 0.5f;
    private float mRelY = 0.5f;

    public ImageButtonWithNumber(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setAntiAlias(true);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ImageButtonWithNumber,
                0, 0);
        try {
            mPaint.setColor(a.getColor(R.styleable.ImageButtonWithNumber_numberColor, Color.BLACK));
            mPaint.setTextSize(a.getDimension(R.styleable.ImageButtonWithNumber_numberSize, 20.f));
            mPaint.setFakeBoldText(a.getBoolean(R.styleable.ImageButtonWithNumber_numberBold, false));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setNumber(int number) {
        mNumber = String.valueOf(number);
        invalidate();
    }

    @Override
    public void setNumberPosition(float relX, float relY) {
        mRelX = relX;
        mRelY = relY;
        invalidate();
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
