package dan.dit.whatsthat.util.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 01.05.15.
 */
public class LinearLayoutProgressBar extends LinearLayout implements PercentProgressListener {
    private static final int DEFAULT_START_COLOR = 0xff05f700;
    private static final int DEFAULT_END_COLOR = Color.TRANSPARENT;
    private int mStartColor = DEFAULT_START_COLOR;
    private int mEndColor = DEFAULT_END_COLOR;
    private int mProgress;
    private Paint mGradientPaint = new Paint();
    private Path mPath = new Path();

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ImageViewWithNumber,
                    0, 0);
            try {
                mStartColor = a.getColor(R.styleable.LinearLayoutProgressBar_startColor, DEFAULT_START_COLOR);
                mEndColor = a.getColor(R.styleable.LinearLayoutProgressBar_endColor, DEFAULT_END_COLOR);
            } finally {
                a.recycle();
            }
        }
    }

    public LinearLayoutProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public LinearLayoutProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public LinearLayoutProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public void onProgressUpdate(int progress) {
        mProgress = progress;
        invalidate();
        requestLayout();
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPath.reset();
        if (getOrientation() == LinearLayout.HORIZONTAL) {
            mPath.addRect(0f, 0f, mProgress * canvas.getWidth() / ((float) PercentProgressListener.PROGRESS_COMPLETE), (float) canvas.getHeight(), Path.Direction.CW);
            mGradientPaint.setShader(new LinearGradient(0, 0, mProgress * canvas.getWidth() / ((float) PercentProgressListener.PROGRESS_COMPLETE), canvas.getHeight(), mEndColor, mStartColor, Shader.TileMode.CLAMP));
        } else {
            float relH = (PercentProgressListener.PROGRESS_COMPLETE - mProgress) * canvas.getHeight() / ((float) PercentProgressListener.PROGRESS_COMPLETE);
            mPath.addRect(0f, relH, canvas.getWidth(), canvas.getHeight(), Path.Direction.CW);
            mGradientPaint.setShader(new LinearGradient(0, relH, 0, canvas.getHeight(), mStartColor, mEndColor, Shader.TileMode.CLAMP));
        }
        canvas.drawPath(mPath, mGradientPaint);
        super.onDraw(canvas);
    }

    public int getStartColor() {
        return mStartColor;
    }

    public int getEndColor() {
        return mEndColor;
    }
}
