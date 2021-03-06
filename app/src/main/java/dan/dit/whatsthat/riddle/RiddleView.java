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

package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.plattysoft.leonids.Particle;
import com.plattysoft.leonids.ParticleField;
import com.plattysoft.leonids.ParticleFieldController;
import com.plattysoft.leonids.ParticleSystem;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.control.RiddleController;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.system.NoPanicDialog;
import dan.dit.whatsthat.testsubject.TestSubjectToast;

/**
 * Created by daniel on 31.03.15.
 */
public class RiddleView extends SurfaceView implements SensorEventListener, ParticleField {
    public static final int BACKGROUND_COLOR_RESOURCE_ID = R.color.main_background;
    private RiddleController mRiddleCtr;
    private SensorManager mSensorManager;
    private Sensor mMagnetometer;
    private Sensor mAccelerometer;
    private boolean mIsResumed;
    private int mBackgroundColor;

    private volatile List<List<Particle>> mParticles;
    private ParticleController mController;

    public RiddleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mController = new ParticleController();
        mParticles = new ArrayList<>();
        setFocusable(true);
        setVisibility(View.INVISIBLE);
        mBackgroundColor = getResources().getColor(BACKGROUND_COLOR_RESOURCE_ID);
        //setZOrderOnTop(true);    // necessary if there is no background color drawn to clear
                                    // but simply a clearing paint, because else the background
                                    // is black
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (hasController()) {
                    mRiddleCtr.onMotionEvent(event);
                }
                return true;
            }
        });
    }

    public void onPause() {
        if (!mIsResumed) {
            return; // already paused
        }
        Log.d("Riddle", "Pausing riddle view, has controller: " + hasController());
        mIsResumed = false;
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (hasController()) {
            mRiddleCtr.stopPeriodicEvent();
        }
    }

    public void onResume() {
        if (mIsResumed) {
            return; // already resumed
        }
        mIsResumed = true;
        if (hasController() && mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (hasController()) {
            mRiddleCtr.resumePeriodicEventIfRequired();
            draw();
        }
    }

    public synchronized void removeController() {
        ensureHasController();
        RiddleController ctr = mRiddleCtr;
        mRiddleCtr = null; // so any input and drawing will no longer be done
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
            mAccelerometer = null;
            mMagnetometer = null;
            mAccelerometerValues = null;
            mGeomagneticValues = null;
        }
        ctr.onCloseRiddle(getContext());
    }

    public long performDrawRiddle() {
        long startTime = System.nanoTime();

        SurfaceHolder holder = getHolder();
        if (holder != null && holder.getSurface() != null && holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                // clear previously drawn artifacts (view is triple buffered), very important if there is any pixel with alpha drawn by the riddle
                canvas.drawColor(mBackgroundColor);
                if (hasController()) {
                    mRiddleCtr.draw(canvas);
                }
                drawParticles(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }
        return (System.nanoTime() - startTime) / 1000000;
    }

    private void drawParticles(Canvas canvas) {
        for (final List<Particle> particles : mParticles) {
            // Draw all the particles
            if (particles != null && !particles.isEmpty()) {
                synchronized (particles) {
                    for (int i = 0; i < particles.size(); i++) {
                        particles.get(i).draw(canvas);
                    }
                }
            }
        }
    }

    public void draw() {
        if (mRiddleCtr != null && !mRiddleCtr.hasRunningPeriodicThread()) {
            performDrawRiddle();
        }
    }

    public synchronized void setController(@NonNull RiddleController controller) {
        if (mRiddleCtr != null) {
            throw new IllegalStateException("Already initialized a controller!");
        }
        mRiddleCtr = controller;
        mRiddleCtr.onRiddleVisible((ViewGroup) getParent());
        mController.clear();
        if (mRiddleCtr.requiresOrientationSensor()) {
            mSensorManager = (SensorManager) getContext().getSystemService(Activity.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (mAccelerometer == null || mMagnetometer == null) {
                Log.d("Riddle", "Missing sensor(s): " + mAccelerometer + " / " + mMagnetometer);
                mSensorManager = null;
                mAccelerometer = null;
                mMagnetometer = null;
                mRiddleCtr.enableNoOrientationSensorAlternative();
            } else {
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
                mAccelerometerValues = new float[3];
                mGeomagneticValues = new float[3];
            }
        }
        if (mIsResumed) {
            mRiddleCtr.resumePeriodicEventIfRequired();
        } else {
            onPause();
        }
        setVisibility(View.VISIBLE);
        draw();
    }

    public synchronized boolean hasController() {
        return mRiddleCtr != null;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();

        // pick the smaller of each to make it square
        if (widthWithoutPadding > heightWithoutPadding) {
            widthWithoutPadding = heightWithoutPadding;
        } else {
            heightWithoutPadding = widthWithoutPadding;
        }

        // add padding and ensure we are >= suggested dimensions
        int widthWithPadding = widthWithoutPadding + getPaddingLeft() + getPaddingRight();
        int heightWithPadding = heightWithoutPadding + getPaddingBottom() + getPaddingTop();
        if (widthWithPadding < getSuggestedMinimumWidth()) {
            widthWithPadding = getSuggestedMinimumWidth();
        }
        if (heightWithPadding < getSuggestedMinimumHeight()) {
            heightWithPadding = getSuggestedMinimumHeight();
        }
        // return the dimensions we want
        setMeasuredDimension(widthWithPadding, heightWithPadding);
    }

    public long getRiddleId() {
        return mRiddleCtr.getRiddleId();
    }

    private float[] mAccelerometerValues;
    private float[] mGeomagneticValues;
    private float[] mR = new float[9];
    private float[] mI = new float[9];
    private float[] mOrientation = new float[3];
    private float[] mROut = new float[mR.length];

    public void onSensorChanged(SensorEvent event) {
        if (mAccelerometerValues != null && mGeomagneticValues != null) {
            switch(event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(event.values, 0, mAccelerometerValues, 0, 3);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(event.values, 0, mGeomagneticValues, 0, 3);
                    break;
            }
            boolean success = SensorManager.getRotationMatrix(mR, mI, mAccelerometerValues, mGeomagneticValues);
            if (success) {
                /*// transform the normal vector g=(0 0 1) into new coordinate space by calculating R^-1 * g = R'*g
                float[] gravity = new float[3];
                for (int i = 0; i < gravity.length; i++) {
                    gravity[i]  = R[6 + i];
                }
                mRiddleCtr.onOrientationEvent(gravity);
                Log.d("Riddle", "Gravity normal vector: " + Arrays.toString(gravity));*/

                // to get azimuth, pitch and roll:
                SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X, SensorManager.AXIS_Y, mROut);

                SensorManager.getOrientation(mROut, mOrientation);
                mRiddleCtr.onOrientationEvent(mOrientation[0], mOrientation[1], mOrientation[2]);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void supplyNoPanicParams(Bundle args) {
        if (!hasController()) {
            return;
        }
        args.putString(NoPanicDialog.KEY_TYPE, mRiddleCtr.getRiddleType().getFullName());
        args.putString(NoPanicDialog.KEY_IMAGE, mRiddleCtr.getImageHash());
    }

    public PracticalRiddleType getRiddleType() {
        ensureHasController();
        return mRiddleCtr.getRiddleType();
    }

    @Override
    public ParticleFieldController getParticleController() {
        return mController;
    }

    @Override
    public void setParticles(List<Particle> particles) {
        mParticles.add(particles);
    }

    private boolean removeParticles(List<Particle> particles) {
        for (int i = 0; i < mParticles.size(); i++) {
            if (mParticles.get(i) == particles) {
                mParticles.remove(i);
                return true;
            }
        }
        return false;
    }

    public Riddle getRiddle() {
        ensureHasController();
        return mRiddleCtr.getRiddle();
    }

    public void forbidRiddleBonusScore() {
        ensureHasController();
        mRiddleCtr.forbidRiddleBonusScore();
    }

    private void ensureHasController() {
        if (!hasController()) {
            throw new IllegalStateException("No controller initialized.");
        }
    }

    public boolean isPaused() {
        return !mIsResumed;
    }

    private class ParticleController implements ParticleFieldController {


        private void clear() {
            mParticles.clear();
        }

        @Override
        public int getPositionInParentX() {
            return 0;
        }

        @Override
        public int getPositionInParentY() {
            return 0;
        }

        @Override
        public void prepareEmitting(List<Particle> particles) {
            if (!removeParticles(particles)) {
                setParticles(particles);
                RiddleController ctr = mRiddleCtr;
                if (ctr != null) {
                    ctr.onParticleSystemCountChanged();
                }
            }
        }

        @Override
        public void onUpdate() {
        }

        @Override
        public void onCleanup(ParticleSystem toClean) {
            if (removeParticles(toClean.getActiveParticles())) {
                RiddleController ctr = mRiddleCtr;
                if (ctr != null) {
                    ctr.onParticleSystemCountChanged();
                }
            }
        }
    }

    public int getActiveParticleSystemsCount() {
        return mParticles.size();
    }

    public interface PartyCallback {
        void doParty(int partyParam);
        void giveCandy(TestSubjectToast toast);
        void showMoneyEarned(int moneyEarned);
    }

    public void checkParty(@NonNull Resources res, @NonNull PartyCallback callback) {
        ensureHasController();
        mRiddleCtr.checkParty(res, callback);
    }
}
