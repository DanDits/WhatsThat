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

/**
 * Created by daniel on 26.08.15.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 24.08.15.
 */
public abstract class ViewFlipperManager extends FrameLayout {
    private final static float MIN_SWIPE_OFFSET_X = 30.f;
    private ImageView mLeftIndicator;
    private ImageView mRightIndicator;
    private ViewFlipper mFlipper;
    private float mLastX;

    public ViewFlipperManager(Context context) {
        super(context);
    }

    public ViewFlipperManager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewFlipperManager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getDisplayedChild() {
        return mFlipper != null ? mFlipper.getDisplayedChild() : -1;
    }

    public View getDisplayedChildView() {
        return mFlipper != null ? mFlipper.getChildAt(mFlipper.getDisplayedChild()) : null;
    }

    protected void init(int selectChild) {
        mLeftIndicator = (ImageView) findViewById(R.id.flip_indicator_left);
        if (mLeftIndicator != null) {
            mLeftIndicator.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flipToPrevious();
                }
            });
        }
        mRightIndicator = (ImageView) findViewById(R.id.flip_indicator_right);
        if (mRightIndicator != null) {
            mRightIndicator.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flipToNext();
                }
            });
        }
        mFlipper = (ViewFlipper) findViewById(R.id.flip_content);
        mFlipper.removeAllViews();
        addViews((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE), mFlipper);

        int current = selectChild;
        current = current >= mFlipper.getChildCount() ? mFlipper.getChildCount() - 1 : current;
        if (current >= 0) {
            mFlipper.setDisplayedChild(current);
            onDisplayedChildChanged();
        }
        mFlipper.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onContentTouched(event);
            }
        });
        updateIndicators();
    }

    protected abstract void addViews(LayoutInflater inflater, ViewFlipper flipper);

    protected int getPreviousInAnimation() {
        return R.anim.slide_in_left;
    }

    protected int getPreviousOutAnimation() {
        return R.anim.slide_out_right;
    }

    private void flipToPrevious() {
        if (!hasLeft()) {
            return;
        }
        // Next screen comes in from left.
        mFlipper.setInAnimation(getContext(), getPreviousInAnimation());
        // Current screen goes out from right.
        mFlipper.setOutAnimation(getContext(), getPreviousOutAnimation());

        mFlipper.showPrevious();
        onDisplayedChildChanged();
        updateIndicators();
    }

    protected int getNextInAnimation() {
        return R.anim.slide_in_right;
    }

    protected int getNextOutAnimation() {
        return R.anim.slide_out_left;
    }

    private void flipToNext() {
        if (!hasRight()) {
            return;
        }
        // Next screen comes in from right.
        mFlipper.setInAnimation(getContext(), getNextInAnimation());
        // Current screen goes out from left.
        mFlipper.setOutAnimation(getContext(), getNextOutAnimation());

        mFlipper.showNext();
        onDisplayedChildChanged();
        updateIndicators();
    }

    private void onDisplayedChildChanged() {
        int selected = getDisplayedChild();
        if (selected >= 0 && selected < mFlipper.getChildCount()) {
            onDisplayedChildChanged(mFlipper.getChildAt(selected));
        }
    }

    public abstract void onDisplayedChildChanged(View displayed);

    protected boolean onContentTouched(MotionEvent touchevent) {
        boolean consumed = false;
        switch (touchevent.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                consumed = true;
                mLastX = touchevent.getX();
                break;
            case MotionEvent.ACTION_UP:
                float currentX = touchevent.getX();
                // Handling left to right screen swap.
                if (mLastX < currentX - MIN_SWIPE_OFFSET_X) {

                    // If there aren't any other children, just break.
                    if (!hasLeft()) {
                        break;
                    }
                    consumed = true;
                    flipToPrevious();
                }

                // Handling right to left screen swap.
                if (mLastX > currentX + MIN_SWIPE_OFFSET_X) {

                    if (!hasRight()) {
                        break;
                    }
                    consumed = true;
                    flipToNext();
                }
                break;
        }
        return consumed;
    }

    protected boolean hasLeft() {
        return mFlipper.getDisplayedChild() > 0;
    }

    protected boolean hasRight() {
        return mFlipper.getDisplayedChild() < mFlipper.getChildCount() - 1;
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
