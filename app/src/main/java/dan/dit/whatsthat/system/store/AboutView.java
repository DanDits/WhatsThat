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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;

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
        mTextPaint.setTextSize(20.f);
        mTextPaint.setAntiAlias(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mDiskAngle = 30.f;
        mTextBounds = new Rect();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN && mRadius > 0.f && mAngleDelta > 0.f) {
                    float angle = 180.f * (float) (Math.atan2(motionEvent.getY() - mCenterY, motionEvent.getX() - mCenterX) / Math.PI);
                    angle -= mDiskAngle;
                    if (angle < 0.f) {
                        angle += 360.f;
                    }
                    int index = (int) (angle / mAngleDelta);
                    startFeedback(index);
                }
                return false;
            }
        });
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
            mTextPaint.getTextBounds(s, 0, s.length(), mTextBounds);
            final float textStartOffset = (mRadius - mTextBounds.width()) / 2.f;
            final float textAngleRad = (float) ((currAngle + mAngleDelta / 2.f)/ 180.f * Math.PI);
            final float angleCos = (float) Math.cos(textAngleRad);
            final float angleSin = (float) Math.sin(textAngleRad);
            if (angleCos < 0.f) {
                mTextPath.moveTo(mCenterX + (mRadius - textStartOffset) * angleCos, mCenterY + (mRadius - textStartOffset) * angleSin);
                mTextPath.lineTo(mCenterX, mCenterY);
            } else {
                mTextPath.moveTo(mCenterX + textStartOffset * angleCos, mCenterY + textStartOffset * angleSin);
                mTextPath.lineTo(mCenterX + mRadius * angleCos, mCenterY + mRadius * angleSin);
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
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        titleBackButton.setText(R.string.store_category_about);
        requestLayout();
        invalidate();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {

    }

    @Override
    public View getView() {
        return getRootView();
    }
}
