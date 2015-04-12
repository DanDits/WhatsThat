package dan.dit.whatsthat.solution;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by daniel on 12.04.15.
 */
public class SolutionInputView extends View {

    private SolutionInput mInput;

    public SolutionInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (mInput != null && mInput.performClick(event.getX(), event.getY())) {
                        invalidate ();
                        requestLayout();
                    }
                }
                return false;
            }
        });
    }

    public void setSolutionInput(@Nullable SolutionInput solutionInput) {
        mInput = solutionInput;
        if (mInput != null) {
            mInput.calculateLayout(getWidth(), getHeight(), getContext().getResources().getDisplayMetrics());
        }
        invalidate();
        requestLayout();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mInput != null) {
            mInput.draw(canvas);
        }
    }

}
