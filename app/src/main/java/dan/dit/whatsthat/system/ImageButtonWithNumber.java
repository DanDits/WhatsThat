package dan.dit.whatsthat.system;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by daniel on 01.05.15.
 */
public class ImageButtonWithNumber extends ImageButton {
    private String mNumber = "";
    private Paint mPaint = new Paint();
    private Rect mDummyRect = new Rect();

    public ImageButtonWithNumber(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setNumber(int number) {
        mNumber = String.valueOf(number);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTextCentered(canvas, mNumber, getWidth() / 2.f, getHeight() - getPaddingBottom());
    }

    private void drawTextCentered(Canvas canvas, String text, float x, float y) {
        float numberOffsetY = -((mPaint.descent() + mPaint.ascent()) / 2);
        mPaint.getTextBounds(text, 0, text.length(), mDummyRect);
        canvas.drawText(text, x - mDummyRect.exactCenterX(), y + numberOffsetY, mPaint);
    }
}
