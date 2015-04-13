package dan.dit.whatsthat.solution;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputView extends View {

    private static final float SWIPE_MAX_OFF_PATH = 50;
    private static final float SWIPE_MIN_DISTANCE = 10;
    private static final float SWIPE_THRESHOLD_VELOCITY = 100;
    private SolutionInput mInput;
    private GestureDetector mGestures;

    public SolutionInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestures = new GestureDetector(getContext(), new MyGestureDetector());
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return mGestures.onTouchEvent(event);
            }
        });
    }

    public void setSolutionInput(@Nullable SolutionInput solutionInput, SolutionInputListener listener) {
        clearListener();
        mInput = solutionInput;
        if (mInput != null) {
            clearAnimation();
            mInput.setListener(listener);
            mInput.calculateLayout(getWidth(), getHeight(), getContext().getResources().getDisplayMetrics());
        }
        invalidate();
        requestLayout();
    }

    public void clearListener() {
        if (mInput != null) {
            mInput.setListener(null);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mInput != null) {
            mInput.draw(canvas);
        }
    }


    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            // right to left swipe
            if(Math.abs(e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (mInput != null && mInput.onFling(e1, e2, velocityX, velocityY)) {
                    invalidate ();
                    requestLayout();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN && mInput != null && mInput.onUserTouchDown(e.getX(), e.getY())) {
                invalidate ();
                requestLayout();
            }
            return true;
        }
    }
}


