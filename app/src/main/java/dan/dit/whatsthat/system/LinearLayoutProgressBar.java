package dan.dit.whatsthat.system;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 01.05.15.
 */
public class LinearLayoutProgressBar extends LinearLayout implements PercentProgressListener {
    private static final int PROGRESS_COLOR = 0xff05f700;
    private int mProgress;
    private Paint mGradientPaint = new Paint();
    private Path mPath = new Path();

    private void init() {
        setWillNotDraw(false);
    }
    public LinearLayoutProgressBar(Context context) {
        super(context);
        init();
    }

    public LinearLayoutProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public LinearLayoutProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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
        mPath.moveTo(0, 0);
        mPath.addRect(0f, 0f, mProgress * canvas.getWidth() / ((float) PercentProgressListener.PROGRESS_COMPLETE), (float) canvas.getHeight(), Path.Direction.CW);
        mGradientPaint.setShader(new LinearGradient(0, 0, mProgress * canvas.getWidth() / ((float) PercentProgressListener.PROGRESS_COMPLETE), canvas.getHeight(), Color.TRANSPARENT, PROGRESS_COLOR, Shader.TileMode.CLAMP));
        canvas.drawPath(mPath, mGradientPaint);

        super.onDraw(canvas);
    }
}
