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
 * Created by daniel on 02.10.15.
 */
public class ImageViewWithText extends ImageView {
    private String mText = "";
    private Paint mPaint = new Paint();
    private Rect mDummyRect = new Rect();
    private float mRelX = 0.5f;
    private float mRelY = 0.5f;

    public ImageViewWithText(Context context) {
        super(context);
        init(context, null);
    }
    public ImageViewWithText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public ImageViewWithText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint.setAntiAlias(true);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ImageViewWithText,
                    0, 0);
            try {
                mText = a.getString(R.styleable.ImageViewWithText_text);
                mText = mText == null ? "" : mText;
                mPaint.setColor(a.getColor(R.styleable.ImageViewWithText_textColor, Color.BLACK));
                mPaint.setTextSize(a.getDimension(R.styleable.ImageViewWithText_textSize, 20.f));
                mPaint.setFakeBoldText(a.getBoolean(R.styleable.ImageViewWithText_textBold, false));
            } finally {
                a.recycle();
            }
        }
    }

    public void setText(String text) {
        mText = text;
        invalidate();
        requestLayout();
    }

    public String getText() {
        return mText;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawTextCentered(canvas, mText, getWidth() * mRelX, getHeight() * mRelY);
    }

    private void drawTextCentered(Canvas canvas, String text, float x, float y) {
        float numberOffsetY = -((mPaint.descent() + mPaint.ascent()) / 2);
        mPaint.getTextBounds(text, 0, text.length(), mDummyRect);
        canvas.drawText(text, x - mDummyRect.exactCenterX(), y + numberOffsetY, mPaint);
    }
}
