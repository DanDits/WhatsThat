package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
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

import dan.dit.whatsthat.riddle.games.RiddleController;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.system.NoPanicDialog;

/**
 * Created by daniel on 31.03.15.
 */
public class RiddleView extends SurfaceView implements SensorEventListener {
    private RiddleController mRiddleCtr;
    private SensorManager mSensorManager;
    private Sensor mMagnetometer;
    private Sensor mAccelerometer;
    private boolean mIsResumed;

    public RiddleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setZOrderOnTop(true);    // necessary
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
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
        mIsResumed = false;
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (hasController()) {
            mRiddleCtr.pausePeriodicEvent();
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
        if (!hasController()) {
            throw new IllegalStateException("No controller initialized.");
        }
        mIsResumed = false;
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
        ctr.pausePeriodicEvent();
        ctr.onCloseRiddle(getContext());
    }

    public void performDrawRiddle() {
        SurfaceHolder holder = getHolder();
        if (holder != null && holder.getSurface() != null && holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                if (hasController()) {
                    mRiddleCtr.draw(canvas);
                }
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void draw() {
        if (mRiddleCtr == null || !mRiddleCtr.hasRunningRenderThread()) {
            performDrawRiddle();
        }
    }

    public void setController(@NonNull RiddleController controller) {
        if (mRiddleCtr != null) {
            throw new IllegalStateException("Already initialized a controller!");
        }
        mIsResumed = true;
        mRiddleCtr = controller;
        mRiddleCtr.onRiddleVisible(this);
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
        draw();
        mRiddleCtr.resumePeriodicEventIfRequired();
    }

    public synchronized boolean hasController() {
        return mRiddleCtr != null;
    }

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
    public void onSensorChanged(SensorEvent event) {
        if (mAccelerometerValues != null && mGeomagneticValues != null) {
            switch(event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    for(int i =0; i < 3; i++){
                        mAccelerometerValues[i] = event.values[i];
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    for(int i =0; i < 3; i++){
                        mGeomagneticValues[i] = event.values[i];
                    }
                    break;
            }
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometerValues, mGeomagneticValues);
            if (success) {
                /*// transform the normal vector g=(0 0 1) into new coordinate space by calculating R^-1 * g = R'*g
                float[] gravity = new float[3];
                for (int i = 0; i < gravity.length; i++) {
                    gravity[i]  = R[6 + i];
                }
                mRiddleCtr.onOrientationEvent(gravity);
                Log.d("Riddle", "Gravity normal vector: " + Arrays.toString(gravity));*/

                // to get azimuth, pitch and roll:
                float orientation[] = new float[3];
                float ROut[] = new float[R.length];
                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Y, ROut);

                SensorManager.getOrientation(ROut, orientation);
                mRiddleCtr.onOrientationEvent(orientation[0], orientation[1], orientation[2]);
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
        if (!hasController()) {
            throw new IllegalStateException("No controller initialized.");
        }
        return mRiddleCtr.getRiddleType();
    }
}