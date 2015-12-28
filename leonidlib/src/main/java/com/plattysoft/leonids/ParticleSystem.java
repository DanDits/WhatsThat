package com.plattysoft.leonids;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.plattysoft.leonids.initializers.AccelerationInitializer;
import com.plattysoft.leonids.initializers.ParticleInitializer;
import com.plattysoft.leonids.initializers.RotationInitiazer;
import com.plattysoft.leonids.initializers.RotationSpeedInitializer;
import com.plattysoft.leonids.initializers.ScaleInitializer;
import com.plattysoft.leonids.initializers.SpeeddByComponentsInitializer;
import com.plattysoft.leonids.initializers.SpeeddModuleAndRangeInitializer;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.modifiers.ParticleModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class ParticleSystem {

    private static final long TIMMERTASK_INTERVAL = 50;
    private int mMaxParticles;
    private Random mRandom;


    private ArrayList<Particle> mParticles;
    private final ArrayList<Particle> mActiveParticles = new ArrayList<>();
    private long mTimeToLive;
    private long mCurrentTime = 0;

    private float mParticlesPerMilisecond;
    private int mActivatedParticles;
    private long mEmitingTime;

    private List<ParticleModifier> mModifiers;
    private List<ParticleInitializer> mInitializers;
    private ValueAnimator mAnimator;
    private Timer mTimer;

    private float mDpToPxScale;

    private int mEmiterXMin;
    private int mEmiterXMax;
    private int mEmiterYMin;
    private int mEmiterYMax;
    private boolean mIgnorePositionInParent;
    private OnAnimationEndedListener mListener;
    private ParticleField mField;

    public ParticleSystem(ParticleField field, Resources res, int maxParticles, long timeToLive) {
        mRandom = new Random();
        setParticleField(field);

        mModifiers = new ArrayList<>();
        mInitializers = new ArrayList<>();

        mMaxParticles = maxParticles;
        // Create the particles
        mParticles = new ArrayList<>();
        mTimeToLive = timeToLive;

        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        mDpToPxScale = (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private ParticleSystem(Resources res, int maxParticles, long timeToLive, ViewGroup parentView) {
        this(new ParticleFieldView(parentView.getContext()).initController(parentView), res, maxParticles,
                timeToLive);
    }

    private void setParticleField(ParticleField field) {
        mField = field;
    }


    /**
     * Creates a particle system with the given parameters
     *
     * @param maxParticles  The maximum number of particles
     * @param drawableRedId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     * @param parentView  The parent ViewGroup to add the particle field to.
     */
    public ParticleSystem(Resources res, int maxParticles, int drawableRedId, long timeToLive,
                          ViewGroup parentView) {
        this(res, maxParticles, res.getDrawable(drawableRedId), timeToLive,
                parentView);
    }


    /**
     * Utility constructor that receives a Drawable. A valid Resources object is required and a
     * valid parent ViewGroup.
     *
     * @param res          A resources object associated with the relevant context.
     * @param maxParticles The maximum number of particles
     * @param drawable     The drawable to use as particle (supports Bitmaps and Animations)
     * @param timeToLive   The time to live for the particles
     * @param parentView The parent ViewGroup to add the ParticleField to.
     */
    public ParticleSystem(Resources res, int maxParticles, Drawable drawable, long
            timeToLive, ViewGroup parentView) {
        this(res, maxParticles, timeToLive, parentView);
        initParticles(drawable);
    }

    public void initParticles(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            for (int i = 0; i < mMaxParticles; i++) {
                mParticles.add(new Particle(bitmap));
            }
        } else if (drawable instanceof AnimationDrawable) {
            AnimationDrawable animation = (AnimationDrawable) drawable;
            for (int i = 0; i < mMaxParticles; i++) {
                mParticles.add(new AnimatedParticle(animation));
            }
        }
//		else {
        // Not supported, no particles are being created
//		}
    }

    public float dpToPx(float dp) {
        return dp * mDpToPxScale;
    }

    /**
     * Adds a modifier to the Particle system, it will be executed on each update.
     *
     * @param modifier modifier to be added to the ParticleSystem
     */
    public ParticleSystem addModifier(ParticleModifier modifier) {
        mModifiers.add(modifier);
        return this;
    }

    public ParticleSystem setSpeedRange(float speedMin, float speedMax) {
        mInitializers.add(new SpeeddModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), 0, 360));
        return this;
    }

    /**
     * Initializes the speed range and angle range of emitted particles. Angles are in degrees
     * and non negative:
     * 0 meaning to the right, 90 to the bottom,... in clockwise orientation. Speed is non
     * negative and is described in pixels per millisecond.
     *
     * @param speedMin The minimum speed to emit particles.
     * @param speedMax The maximum speed to emit particles.
     * @param minAngle The minimum angle to emit particles in degrees.
     * @param maxAngle The maximum angle to emit particles in degrees.
     * @return This.
     */
    public ParticleSystem setSpeedModuleAndAngleRange(float speedMin, float speedMax, int minAngle, int maxAngle) {
        // else emitting from top (270°) to bottom (90°) range would not be possible if someone
        // entered minAngle = 270 and maxAngle=90 since the module would swap the values
        while (maxAngle < minAngle) {
            maxAngle += 360;
        }
        mInitializers.add(new SpeeddModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the speed components ranges that particles will be emitted. Speeds are
     * measured in density pixels per millisecond.
     *
     * @param speedMinX The minimum speed in x direction.
     * @param speedMaxX The maximum speed in x direction.
     * @param speedMinY The minimum speed in y direction.
     * @param speedMaxY The maximum speed in y direction.
     * @return This.
     */
    public ParticleSystem setSpeedByComponentsRange(float speedMinX, float speedMaxX, float speedMinY, float speedMaxY) {
        mInitializers.add(new SpeeddByComponentsInitializer(dpToPx(speedMinX), dpToPx(speedMaxX),
                dpToPx(speedMinY), dpToPx(speedMaxY)));
        return this;
    }

    /**
     * Initializes the initial rotation range of emitted particles. The rotation angle is
     * measured in degrees with 0° being no rotation at all and 90° tilting the image to the right.
     *
     * @param minAngle The minimum tilt angle.
     * @param maxAngle The maximum tilt angle.
     * @return This.
     */
    public ParticleSystem setInitialRotationRange(int minAngle, int maxAngle) {
        mInitializers.add(new RotationInitiazer(minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the scale range of emitted particles. Will scale the images around their
     * center multiplied with the given scaling factor.
     *
     * @param minScale The minimum scaling factor
     * @param maxScale The maximum scaling factor.
     * @return This.
     */
    public ParticleSystem setScaleRange(float minScale, float maxScale) {
        mInitializers.add(new ScaleInitializer(minScale, maxScale));
        return this;
    }

    /**
     * Initializes the rotation speed of emitted particles. Rotation speed is measured in degrees
     * per second.
     *
     * @param rotationSpeed The rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeed(float rotationSpeed) {
        mInitializers.add(new RotationSpeedInitializer(rotationSpeed, rotationSpeed));
        return this;
    }

    /**
     * Initializes the rotation speed range for emitted particles. The rotation speed is measured
     * in degrees per second and can be positive or negative.
     *
     * @param minRotationSpeed The minimum rotation speed.
     * @param maxRotationSpeed The maximum rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeedRange(float minRotationSpeed, float maxRotationSpeed) {
        mInitializers.add(new RotationSpeedInitializer(minRotationSpeed, maxRotationSpeed));
        return this;
    }

    /**
     * Initializes the acceleration range and angle range of emitted particles. The acceleration
     * components in x and y direction are controlled by the acceleration angle. The acceleration
     * is measured in density pixels per square millisecond. The angle is measured in degrees
     * with 0° pointing to the right and going clockwise.
     *
     * @param minAcceleration
     * @param maxAcceleration
     * @param minAngle
     * @param maxAngle
     * @return
     */
    public ParticleSystem setAccelerationModuleAndAndAngleRange(float minAcceleration, float maxAcceleration, int minAngle, int maxAngle) {
        mInitializers.add(new AccelerationInitializer(dpToPx(minAcceleration), dpToPx(maxAcceleration),
                minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the acceleration for emitted particles with the given angle. Acceleration is
     * measured in pixels per square millisecond. The angle is measured in degrees with 0°
     * meaning to the right and orientation being clockwise. The angle controls the acceleration
     * direction.
     *
     * @param acceleration The acceleration.
     * @param angle        The acceleration direction.
     * @return This.
     */
    public ParticleSystem setAcceleration(float acceleration, int angle) {
        mInitializers.add(new AccelerationInitializer(acceleration, acceleration, angle, angle));
        return this;
    }

    public ParticleSystem setStartTime(int time) {
        mCurrentTime = time;
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param milisecondsBeforeEnd fade out duration in milliseconds
     * @param interpolator         the interpolator for the fade out (default is linear)
     */
    public ParticleSystem setFadeOut(long milisecondsBeforeEnd, Interpolator interpolator) {
        mModifiers.add(new AlphaModifier(255, 0, mTimeToLive - milisecondsBeforeEnd, mTimeToLive, interpolator));
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param duration fade out duration in milliseconds
     */
    public ParticleSystem setFadeOut(long duration) {
        return setFadeOut(duration, new LinearInterpolator());
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emitingTime        time the emiter will be emiting particles
     */
    public void emitWithGravity(View emiter, int gravity, int particlesPerSecond, int emitingTime) {
        // Setup emiter
        configureEmiter(emiter, gravity);
        startEmitting(particlesPerSecond, emitingTime);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emitingTime        time the emiter will be emiting particles
     */
    public void emit(View emiter, int particlesPerSecond, int emitingTime) {
        emitWithGravity(emiter, Gravity.CENTER, particlesPerSecond, emitingTime);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emit(View emiter, int particlesPerSecond) {
        // Setup emiter
        emitWithGravity(emiter, Gravity.CENTER, particlesPerSecond);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emitWithGravity(View emiter, int gravity, int particlesPerSecond) {
        // Setup emiter
        configureEmiter(emiter, gravity);
        startEmitting(particlesPerSecond);
    }

    private void startEmitting(int particlesPerSecond) {
        mActivatedParticles = 0;
        mParticlesPerMilisecond = particlesPerSecond / 1000f;
        mField.getParticleController().prepareEmitting(mActiveParticles);
        mEmitingTime = -1; // Meaning infinite
        updateParticlesBeforeStartTime(particlesPerSecond);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onUpdate(mCurrentTime);
                mCurrentTime += TIMMERTASK_INTERVAL;
            }
        }, 0, TIMMERTASK_INTERVAL);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond, long emittingTime) {
        configureEmiter(emitterX, emitterY);
        startEmitting(particlesPerSecond, emittingTime);
    }

    private void configureEmiter(int emitterX, int emitterY) {
        // We configure the emitter based on the window location to fix the offset of action bar
        // if present
        mEmiterXMin = mIgnorePositionInParent ? emitterX : emitterX - mField.getParticleController().getPositionInParentX();
        mEmiterXMax = mEmiterXMin;
        mEmiterYMin = mIgnorePositionInParent ? emitterY : emitterY - mField.getParticleController().getPositionInParentY();
        mEmiterYMax = mEmiterYMin;
    }


    private void startEmitting(int particlesPerSecond, long emittingTime) {
        mActivatedParticles = 0;
        mParticlesPerMilisecond = particlesPerSecond / 1000f;

        mField.getParticleController().prepareEmitting(mActiveParticles);

        updateParticlesBeforeStartTime(particlesPerSecond);
        mEmitingTime = emittingTime;
        startAnimator(new LinearInterpolator(), emittingTime + mTimeToLive);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond) {
        configureEmiter(emitterX, emitterY);
        startEmitting(particlesPerSecond);
    }


    public void updateEmitPoint(int emitterX, int emitterY) {
        configureEmiter(emitterX, emitterY);
    }

    /**
     * Launches particles in one Shot
     *
     * @param emiter       View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     */
    public void oneShot(View emiter, int numParticles) {
        oneShot(emiter, numParticles, new LinearInterpolator());
    }

    /**
     * Launches particles in one Shot using a special Interpolator
     *
     * @param emiter       View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     * @param interpolator the interpolator for the time
     */
    public void oneShot(View emiter, int numParticles, Interpolator interpolator) {
        configureEmiter(emiter, Gravity.CENTER);
        mActivatedParticles = 0;
        mEmitingTime = mTimeToLive;
        // We create particles based in the parameters
        for (int i = 0; i < numParticles && i < mMaxParticles; i++) {
            activateParticle(0);
        }
        mField.getParticleController().prepareEmitting(mActiveParticles);

        // We start a property animator that will call us to do the update
        // Animate from 0 to timeToLiveMax
        startAnimator(interpolator, mTimeToLive);
    }

    private void startAnimator(Interpolator interpolator, long animnationTime) {
        mAnimator = ValueAnimator.ofInt(0, (int) animnationTime);
        mAnimator.setDuration(animnationTime);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int miliseconds = (Integer) animation.getAnimatedValue();
                onUpdate(miliseconds);
            }
        });
        mAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cleanupAnimation();
                onAnimationEnded(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cleanupAnimation();
                onAnimationEnded(true);
            }
        });
        mAnimator.setInterpolator(interpolator);
        mAnimator.start();
    }

    public void onAnimationEnded(boolean cancelled) {
        if (mListener != null) {
            mListener.onParticleAnimationEnded(this, cancelled);
        }
    }

    public void clearInitializers() {
        mInitializers.clear();
    }

    public List<Particle> getActiveParticles() {
        return mActiveParticles;
    }

    public interface OnAnimationEndedListener {
        void onParticleAnimationEnded(ParticleSystem system, boolean cancelled);
    }

    public void setOnAnimationEndedListener(OnAnimationEndedListener listener) {
        mListener = listener;
    }

    private void configureEmiter(View emiter, int gravity) {
        // It works with an emision range
        int[] location = new int[2];
        emiter.getLocationInWindow(location);

        int parentX = mField.getParticleController().getPositionInParentX();
        int parentY = mField.getParticleController().getPositionInParentY();

        // Check horizontal gravity and set range
        if (hasGravity(gravity, Gravity.LEFT)) {
            mEmiterXMin = location[0] - parentX;
            mEmiterXMax = mEmiterXMin;
        } else if (hasGravity(gravity, Gravity.RIGHT)) {
            mEmiterXMin = location[0] + emiter.getWidth() - parentX;
            mEmiterXMax = mEmiterXMin;
        } else if (hasGravity(gravity, Gravity.CENTER_HORIZONTAL)) {
            mEmiterXMin = location[0] + emiter.getWidth() / 2 - parentX;
            mEmiterXMax = mEmiterXMin;
        } else {
            // All the range
            mEmiterXMin = location[0] - parentX;
            mEmiterXMax = location[0] + emiter.getWidth() - parentX;
        }

        // Now, vertical gravity and range
        if (hasGravity(gravity, Gravity.TOP)) {
            mEmiterYMin = location[1] - parentY;
            mEmiterYMax = mEmiterYMin;
        } else if (hasGravity(gravity, Gravity.BOTTOM)) {
            mEmiterYMin = location[1] + emiter.getHeight() - parentY;
            mEmiterYMax = mEmiterYMin;
        } else if (hasGravity(gravity, Gravity.CENTER_VERTICAL)) {
            mEmiterYMin = location[1] + emiter.getHeight() / 2 - parentY;
            mEmiterYMax = mEmiterYMin;
        } else {
            // All the range
            mEmiterYMin = location[1] - parentY;
            mEmiterYMax = location[1] + emiter.getHeight() - parentY;
        }
    }

    private boolean hasGravity(int gravity, int gravityToCheck) {
        return (gravity & gravityToCheck) == gravityToCheck;
    }

    private void activateParticle(long delay) {
        Particle p = mParticles.remove(0);
        p.init();
        // Initialization goes before configuration, scale is required before can be configured properly
        for (int i = 0; i < mInitializers.size(); i++) {
            mInitializers.get(i).initParticle(p, mRandom);
        }
        int particleX = getFromRange(mEmiterXMin, mEmiterXMax);
        int particleY = getFromRange(mEmiterYMin, mEmiterYMax);
        p.configure(mTimeToLive, particleX, particleY);
        p.activate(delay, mModifiers);
        mActiveParticles.add(p);
        mActivatedParticles++;
    }

    private int getFromRange(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        return mRandom.nextInt(maxValue - minValue) + minValue;
    }

    private void onUpdate(long miliseconds) {
        while (((mEmitingTime > 0 && miliseconds < mEmitingTime) || mEmitingTime == -1) && // This point should emit
                !mParticles.isEmpty() && // We have particles in the pool
                mActivatedParticles < mParticlesPerMilisecond * miliseconds) { // and we are under the number of particles that should be launched
            // Activate a new particle
            activateParticle(miliseconds);
        }
        synchronized (mActiveParticles) {
            for (int i = 0; i < mActiveParticles.size(); i++) {
                boolean active = mActiveParticles.get(i).update(miliseconds);
                if (!active) {
                    Particle p = mActiveParticles.remove(i);
                    i--; // Needed to keep the index at the right position
                    mParticles.add(p);
                }
            }
        }
        mField.getParticleController().onUpdate();
    }

    private void cleanupAnimation() {
        mField.getParticleController().onCleanup(this);
        mParticles.addAll(mActiveParticles);
    }

    /**
     * Stops emitting new particles, but will draw the existing ones until their timeToLive expire
     * For an cancellation and stop drawing of the particles, use cancel instead.
     */
    public void stopEmitting() {
        // The time to be emiting is the current time (as if it was a time-limited emiter
        mEmitingTime = mCurrentTime;
    }

    /**
     * Cancels the particle system and all the animations.
     * To stop emitting but animate until the end, use stopEmitting instead.
     */
    public void cancel() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            cleanupAnimation();
        }
    }

    private void updateParticlesBeforeStartTime(int particlesPerSecond) {
        if (particlesPerSecond == 0) {
            return;
        }
        long currentTimeInMs = mCurrentTime / 1000;
        long framesCount = currentTimeInMs / particlesPerSecond;
        if (framesCount == 0) {
            return;
        }
        long frameTimeInMs = mCurrentTime / framesCount;
        for (int i = 1; i <= framesCount; i++) {
            onUpdate(frameTimeInMs * i + 1);
        }
    }

    /**
     * Allows to emit at exact coordinates within the parent container without offsetting by the
     * coordinates of the containers view. Useful if the emitting is done in the container's
     * coordinate system and the offset is not known.
     * Will not ignore position in parent when emitting from a certain view.
     */
    public void setIgnorePositionInParent() {
        mIgnorePositionInParent = true;
    }
}
