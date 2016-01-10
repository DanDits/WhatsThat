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

package dan.dit.whatsthat.system.store;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 13.06.15.
 */
public class AboutView extends View implements StoreContainer {
    private static final int[] COLORS = new int[] {0xFFFF0000, 0xFFfa7600, 0xFFffba00, 0xFF0dd70d, 0xFF0d4fd7, 0xFF9f00b5, 0xFF921126, 0xFFe5dd00, 0xFF00c3d4};
    private static final int AREA_51_INDEX = 5;
    private static final int AREA_51_HACK = 137;
    private float mDiskAngle;
    private Paint mDiskPaint;
    private RectF mDiskBound;
    private Path mTextPath;
    private Paint mTextPaint;
    private Rect mTextBounds;
    private float mCenterX;
    private float mCenterY;
    private float mRadius;
    private float mAngleDelta;
    private int mNopeCount;
    private float mTextPaintDefaultSize;
    private VelocityTracker mSpinTracker;
    private float mSpinDownAngleRad;
    private float mSpinDownDiskAngle;
    private ValueAnimator mSpinAnimator;


    public AboutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        init();
    }

    private void init() {
        mDiskPaint = new Paint();
        mDiskPaint.setAntiAlias(true);
        mDiskPaint.setStyle(Paint.Style.FILL);
        mDiskBound = new RectF();
        mTextPath = new Path();
        mTextPaint = new Paint();
        mTextPaintDefaultSize = ImageUtil.convertDpToPixel(18.f, getResources().getDisplayMetrics
                ().densityDpi);
        mTextPaint.setTextSize(mTextPaintDefaultSize);
        mTextPaint.setAntiAlias(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setDiskAngleGrad(30.f);
        mTextBounds = new Rect();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final float yDelta = motionEvent.getY() - mCenterY;
                final float xDelta = motionEvent.getX() - mCenterX;
                boolean isInsideCircle = mRadius >= 0.f && mAngleDelta > 0.f
                        && xDelta * xDelta + yDelta * yDelta <= mRadius * mRadius;
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (mSpinTracker != null) {
                        mSpinTracker.addMovement(motionEvent);
                    }
                    startSpinning(motionEvent.getX(), motionEvent.getY());
                    if (isInsideCircle && mSpinAnimator == null) {
                        float angle = getDiskGradAngleBetweenPoints(0, 0, xDelta, yDelta);
                        int index = (int) (angle / mAngleDelta);
                        startFeedback(index);
                    }
                } else if (isInsideCircle && motionEvent.getActionMasked() == MotionEvent
                        .ACTION_DOWN) {
                    // pressing down inside circle, pressing down outside doesnt influence anything
                    cancelSpinning();
                    if (mSpinTracker != null) {
                        mSpinTracker.recycle();
                    }
                    mSpinTracker = VelocityTracker.obtain();
                    mSpinTracker.addMovement(motionEvent);
                    mSpinDownAngleRad = getAngleBetweenPoints(0, 0, xDelta, yDelta, true);
                    mSpinDownDiskAngle = mDiskAngle;
                    return true; // else no ACTION_UP will be delivered
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    if (mSpinTracker != null) {
                        mSpinTracker.addMovement(motionEvent);
                    }
                    if (!isInsideCircle) {
                        startSpinning(motionEvent.getX(), motionEvent.getY());
                    } else {
                        cancelSpinning();

                        float currAngle = getAngleBetweenPoints(0, 0, xDelta, yDelta, true);
                        float angleDiff = currAngle - mSpinDownAngleRad; // how much difference
                        // between original down action and now in angle radians

                        setDiskAngleGrad(mSpinDownDiskAngle + (float) (angleDiff / Math.PI * 180));
                        invalidate();
                    }

                }
                return false;
            }
        });
    }

    private void cancelSpinning() {
        if (mSpinAnimator != null) {
            Log.d("Riddle", "Cancel spinning.");
            mSpinAnimator.cancel();
        }
        mSpinAnimator = null;
    }

    private void startSpinning(float x, float y) {
        if (mSpinTracker == null) {
            return;
        }
        cancelSpinning();
        // use gram schmidt orthogonalization to obtain orthogonal part of velocity to current
        // position x/y on disk
        float rx = x - mCenterX;
        float ry = y - mCenterY;
        mSpinTracker.computeCurrentVelocity(1); // pixels per millisecond
        float vx = mSpinTracker.getXVelocity();
        float vy = mSpinTracker.getYVelocity();

        float factor = (rx * vx + ry * vy) / (rx * rx + ry * ry);
        float spinX = vx - factor * rx;
        float spinY = vy - factor * ry;
        // spin is now orthogonal to r, that is spinX * rx + spinY * ry = 0

        // now only direction of spinning (CW, CCW) is missing and obtained by angle of spinning
        // direction. But be ware that the atan angle function only returns angles between -pi
        // and pi and results are equal for left and right half of disk
        float direction = getAngleBetweenPoints(spinX + rx, spinY + ry, rx, ry, false);
        float signum = direction >= 0f ? -1f : 1f;
        if (rx <= 0) {
            signum = -signum; // invert for left half of disk
        }
        // animate that angle speed to zero and spin the disk
        float startAngleSpeed = signum * (float) (Math.sqrt(spinX * spinX + spinY * spinY));
        if (Math.abs(startAngleSpeed) > 0) {

            mSpinAnimator = ValueAnimator.ofFloat(startAngleSpeed, 0);
            mSpinAnimator.setInterpolator(new DecelerateInterpolator());
            mSpinAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public long mLastPlayTime;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    long playTimeDelta = animation.getCurrentPlayTime() - mLastPlayTime;
                    mLastPlayTime = animation.getCurrentPlayTime();
                    setDiskAngleGrad(mDiskAngle + playTimeDelta * (Float) mSpinAnimator
                            .getAnimatedValue());
                    invalidate();
                }
            });
            Log.d("HomeStuff", "Starting animation with start angle speed: " + startAngleSpeed);
            mSpinAnimator.setDuration(Math.max(100L, (long) Math.abs(startAngleSpeed * 10000)));
            mSpinAnimator.start();
            if (TestSubject.isInitialized()) {
                TestSubject.getInstance().getAchievementHolder().getMiscData().putValue
                        (MiscAchievementHolder.KEY_SPIN_WHEEL_START_ANGLE_SPEED,
                                (long) (startAngleSpeed * 1000), AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
        }
        mSpinTracker.recycle();
        mSpinTracker = null;
    }

    private void setDiskAngleGrad(float angleGrad) {
        mDiskAngle = angleGrad;
        if (mDiskAngle < 0f) {
            mDiskAngle += 360f;
        } else if (mDiskAngle >= 360f) {
            mDiskAngle -= 360f;
        }
    }

    private float getDiskGradAngleBetweenPoints(float x1, float y1, float x2, float y2) {
        float angle = 180.f * (float) (Math.atan2(y2 - y1, x2 - x1) /
                Math.PI);
        angle -= mDiskAngle;
        if (angle < 0.f) {
            angle += 360.f;
        }
        return angle;
    }

    private float getAngleBetweenPoints(float x1, float y1, float x2, float y2, boolean normalize) {
        float angle = (float) (Math.atan2(y2 - y1, x2 - x1));
        if (normalize && angle < 0.f) {
            angle += 2 * Math.PI;
        }
        return angle;
    }



    private void startFeedback(int index) {
        if (index == AREA_51_INDEX) {
            mNopeCount++;
            if (mNopeCount >= AREA_51_HACK) {
                Toast.makeText(getContext(), "Clicking " + AREA_51_HACK + " times resolved bug!", Toast.LENGTH_LONG).show();
                mNopeCount = 0;
            } else {
                Toast.makeText(getContext(), "Bug", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String[] emailSubject = getResources().getStringArray(R.array.contact_mail_subject);
        String[] emailBody = getResources().getStringArray(R.array.contact_mail_body);
        if (index >= 0 && index < emailSubject.length) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{TestSubject.EMAIL_FEEDBACK});
                intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject[index]);
                if (index < emailBody.length) {
                    intent.putExtra(Intent.EXTRA_TEXT, emailBody[index]);
                }
                getContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(), "Nothing to send.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCenterX = canvas.getWidth() / 2.f;
        mCenterY = canvas.getHeight() / 2.f;
        float border = 2.f;
        mRadius = Math.min(mCenterX, mCenterY) - border;
        mDiskBound.set(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        mDiskPaint.setColor(Color.BLACK);
        canvas.drawCircle(mCenterX, mCenterY, mRadius + border, mDiskPaint);
        String[] contactTexts = getResources().getStringArray(R.array.contact_headings);
        float currAngle = mDiskAngle;
        mAngleDelta = 360.f / (float) contactTexts.length;
        int index = 0;
        for (String s : contactTexts) {
            int color = COLORS[index % COLORS.length];
            mDiskPaint.setColor(color);
            canvas.drawArc(mDiskBound, currAngle, mAngleDelta, true, mDiskPaint);
            mTextPath.rewind();
            mTextPaint.setTextSize(mTextPaintDefaultSize);
            final float maxTextLengthFactor = 0.70f;
            do {
                mTextPaint.getTextBounds(s, 0, s.length(), mTextBounds);
                if (mTextBounds.width() > mRadius * maxTextLengthFactor) {
                    mTextPaint.setTextSize(mTextPaint.getTextSize() - 1.f); // linear search for
                    // fitting size, not too important to optimize this to logarithmic
                }
            } while (mTextBounds.width() > mRadius * maxTextLengthFactor && mTextPaint.getTextSize() > 1.f);
            final float textStartOffset = (mRadius - mTextBounds.width()) / 2.f;
            final float textAngleRad = (float) ((currAngle + mAngleDelta / 2.f)/ 180.f * Math.PI);
            final float angleCos = (float) Math.cos(textAngleRad);
            final float angleSin = (float) Math.sin(textAngleRad);
            if (angleCos < 0.f) {
                float pathOffsetX = angleSin * mTextBounds.height() / 3;
                float pathOffsetY = -angleCos * mTextBounds.height() / 3;
                // in left half we want text to go from outer to inner
                mTextPath.moveTo(mCenterX + (mRadius - textStartOffset) * angleCos + pathOffsetX,
                        mCenterY + (mRadius - textStartOffset) * angleSin + pathOffsetY);
                mTextPath.lineTo(mCenterX + pathOffsetX, mCenterY + pathOffsetY);
            } else {
                //offset so that approximately the baseline of the text is in on the path
                // dividing the cake piece
                float pathOffsetX = -angleSin * mTextBounds.height() / 3;
                float pathOffsetY = angleCos * mTextBounds.height() / 3;
                // in right half we want text to go from inner to outer
                mTextPath.moveTo(mCenterX + textStartOffset * angleCos + pathOffsetX,
                        mCenterY + textStartOffset * angleSin + pathOffsetY);
                mTextPath.lineTo(mCenterX + mRadius * angleCos + pathOffsetX,
                        mCenterY + mRadius * angleSin + pathOffsetY);
            }
            mTextPaint.setColor(getInverseColor(color));
            canvas.drawTextOnPath(s, mTextPath, 0.f, 0.f, mTextPaint);
            currAngle += mAngleDelta;
            index++;
        }
    }

    private int getInverseColor(int fromColor) {
        if (ColorAnalysisUtil.getBrightnessNoAlpha(fromColor) >= 0.3) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    @Override
    public void refresh(StoreActivity activity, FrameLayout titleBackContainer) {
        requestLayout();
        invalidate();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
        if (mSpinAnimator != null) {
            mSpinAnimator.cancel();
            mSpinAnimator = null;
        }
    }

    @Override
    public View getView() {
        return getRootView();
    }
}
