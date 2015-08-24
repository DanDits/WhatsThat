package dan.dit.whatsthat.system;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 24.08.15.
 */
public class HintsView extends FrameLayout {
    private final static float MIN_SWIPE_OFFSET_X = 30.f;
    private PracticalRiddleType mType;
    private ImageView mLeftIndicator;
    private ImageView mRightIndicator;
    private ViewFlipper mHints;
    private TestSubject mTestSubject;
    private float mLastX;

    public HintsView(Context context) {
        super(context);
    }

    public HintsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HintsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setType(PracticalRiddleType type) {
        if (!TestSubject.isInitialized()) {
            return;
        }
        mType = type;
        mTestSubject = TestSubject.getInstance();
        mLeftIndicator = (ImageView) findViewById(R.id.hints_indicator_left);
        mRightIndicator = (ImageView) findViewById(R.id.hints_indicator_right);
        mHints = (ViewFlipper) findViewById(R.id.hints_content);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHints.removeAllViews();
        for (int i = 0; i < mType.getTotalAvailableHintsCount(); i++) {
            if (mTestSubject.hasAvailableHint(mType, i)) {
                TextView hint = (TextView) inflater.inflate(R.layout.hints_view_hint, null);
                hint.setText(mType.getRiddleHint(getResources(), i));
                mHints.addView(hint);
            }
        }
        int current = mTestSubject.getCurrentRiddleHintNumber(mType);
        current = current >= mHints.getChildCount() ? mHints.getChildCount() - 1 : current;
        if (current >= 0) {
            mHints.setDisplayedChild(current);
            updateCurrentRiddleHintNumber();
        }
        mHints.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return hintsTouched(event);
            }
        });
        updateIndicators();
    }

    public boolean hintsTouched(MotionEvent touchevent) {
        boolean consumed = false;
        switch (touchevent.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                consumed = true;
                mLastX = touchevent.getX();
                break;
            case MotionEvent.ACTION_UP:
                float currentX = touchevent.getX();
                Log.d("HomeStuff", "DiffX " + (currentX - mLastX) + " hasLeft: " + hasLeft() + " hasRight: " + hasRight() + " children: " + mHints.getChildCount());
                // Handling left to right screen swap.
                if (mLastX < currentX - MIN_SWIPE_OFFSET_X) {

                    // If there aren't any other children, just break.
                    if (!hasLeft()) {
                        break;
                    }
                    consumed = true;
                    Log.d("HomeStuff", "Showing previous hint, hasLeft and currenlty displayed is " + mHints.getDisplayedChild() + " and current hint is " + mTestSubject.getCurrentRiddleHintNumber(mType));
                    // Next screen comes in from left.
                    mHints.setInAnimation(getContext(), R.anim.slide_in_left);
                    // Current screen goes out from right. 
                    mHints.setOutAnimation(getContext(), R.anim.slide_out_right);

                    mHints.showPrevious();
                    updateIndicators();
                }

                // Handling right to left screen swap.
                if (mLastX > currentX + MIN_SWIPE_OFFSET_X) {

                    if (!hasRight()) {
                        break;
                    }
                    consumed = true;
                    Log.d("HomeStuff", "Showing next hint, hasRight and currenlty displayed is " + mHints.getDisplayedChild() + " and current hint is " + mTestSubject.getCurrentRiddleHintNumber(mType));
                    // Next screen comes in from right.
                    mHints.setInAnimation(getContext(), R.anim.slide_in_right);
                    // Current screen goes out from left. 
                    mHints.setOutAnimation(getContext(), R.anim.slide_out_left);

                    mHints.showNext();
                    updateCurrentRiddleHintNumber();
                    updateIndicators();
                }
                break;
        }
        return consumed;
    }

    private void updateCurrentRiddleHintNumber() {
        if (mHints.getDisplayedChild() >= mTestSubject.getCurrentRiddleHintNumber(mType)) {
            mTestSubject.increaseRiddleHintsDisplayed(mType);
        }
    }

    private boolean hasLeft() {
        return mHints.getDisplayedChild() > 0;
    }

    private boolean hasRight() {
        return mTestSubject.hasAvailableHint(mType, mHints.getDisplayedChild() + 1);
    }

    private void updateIndicators() {
        if (mLeftIndicator != null) {
            mLeftIndicator.setVisibility(hasLeft() ? View.VISIBLE : View.INVISIBLE);
        }
        if (mRightIndicator != null) {
            mRightIndicator.setVisibility(hasRight() ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
