package dan.dit.whatsthat.riddle;

import android.content.Context;
import android.util.AttributeSet;
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
import dan.dit.whatsthat.util.ui.ViewFlipperManager;

/**
 * Created by daniel on 24.08.15.
 */
public class HintsView extends ViewFlipperManager {
    private PracticalRiddleType mType;
    private TestSubject mTestSubject;

    public HintsView(Context context) {
        super(context);
    }

    public HintsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HintsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void addViews(LayoutInflater inflater, ViewFlipper flipper) {
        for (int i = 0; i < mType.getTotalAvailableHintsCount(); i++) {
            if (mTestSubject.hasAvailableHint(mType, i)) {
                TextView hint = (TextView) inflater.inflate(R.layout.hints_view_hint, null);
                hint.setText(mType.getRiddleHint(getResources(), i));
                flipper.addView(hint);
            }
        }
    }


    public void setType(PracticalRiddleType type) {
        if (!TestSubject.isInitialized()) {
            return;
        }
        mType = type;
        mTestSubject = TestSubject.getInstance();


        int current = mTestSubject.getCurrentRiddleHintNumber(mType);
        init(current);
    }

    @Override
    public void onDisplayedChildChanged(View displayed) {
        if (getDisplayedChild() >= mTestSubject.getCurrentRiddleHintNumber(mType)) {
            mTestSubject.increaseRiddleHintsDisplayed(mType);
        }
    }

    @Override
    protected boolean hasRight() {
        return mTestSubject.hasAvailableHint(mType, getDisplayedChild() + 1);
    }
}
