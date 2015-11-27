package dan.dit.whatsthat.riddle.control;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.util.general.MathFunction;

/**
 * Created by daniel on 05.11.15.
 */
public class RiddleCanvasAnimation extends RiddleAnimation {
    private List<CanvasAnimation> mAnimations = new ArrayList<>();;
    private RiddleAnimation mWrapped;

    protected RiddleCanvasAnimation() {
        super(RiddleAnimationController.LEVEL_GROUNDING);
    }

    protected RiddleCanvasAnimation(@NonNull RiddleAnimation animation) {
        super(RiddleAnimationController.LEVEL_ON_TOP);
        mWrapped = animation;
    }

    @Override
    boolean isAlive() {
        return mAnimations.size() > 0 && (mWrapped == null || mWrapped
                .isAlive());
    }

    protected void addAnimation(CanvasAnimation animation) {
        mAnimations.add(animation);
    }

    @Override
    protected void update(long updatePeriod) {
        for (int i = 0; i < mAnimations.size(); i++) {
            CanvasAnimation curr = mAnimations.get(i);
            curr.update(updatePeriod);
            if (!curr.isAlive()) {
                mAnimations.remove(i);
                i--;
            }
        }
        if (mWrapped != null) {
            mWrapped.update(updatePeriod);
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        boolean saveAndRestore = mWrapped != null;
        if (saveAndRestore) {
            canvas.save();
        }
        for (int i = 0; i < mAnimations.size(); i++) {
            CanvasAnimation curr = mAnimations.get(i);
            curr.apply(canvas);
        }
        if (mWrapped != null) {
            mWrapped.draw(canvas, paint);
        }
        if (saveAndRestore) {
            canvas.restore();
        }
    }

    @Override
    public void onBorn() {
        super.onBorn();
        Log.d("Riddle", "Riddle view animation is born!");
    }

    @Override
    public void onKilled(boolean murdered) {
        super.onKilled(murdered);
        Log.d("Riddle", "Riddle view animation is killed: " + murdered);
    }

    public static class Builder {
        private RiddleCanvasAnimation mAnim;
        private MathFunction mInterpolator;
        private int mLives = 1;
        private int mNextLifeMode = CanvasAnimation.DEFAULT_NEXT_LIFE_MODE;

        public Builder() {
            mAnim = new RiddleCanvasAnimation();
        }

        public Builder(@NonNull RiddleAnimation toWrap) {
            mAnim = new RiddleCanvasAnimation(toWrap);
        }
        public Builder setInterpolator(MathFunction interpolator) {
            mInterpolator = interpolator;
            return this;
        }

        public Builder setLives(int lives) {
            mLives = lives;
            return this;
        }

        public Builder setNextLifeMode(int nextLifeMode) {
            mNextLifeMode = nextLifeMode;
            return this;
        }

        public Builder addRotate(float rotateDeltaDegrees, float rotateCenterX, float
                rotateCenterY) {
            mAnim.addAnimation(new Rotate(rotateDeltaDegrees, rotateCenterX, rotateCenterY,
                    0L).setLives(CanvasAnimation.LIVES_LIFE_FOREVER));
            return this;
        }
        public Builder addRotate(float rotateDeltaDegrees, float rotateCenterX, float
                rotateCenterY, long duration) {
            mAnim.addAnimation(new Rotate(rotateDeltaDegrees, rotateCenterX, rotateCenterY,
                    duration).setInterpolator(mInterpolator).setLives(mLives).setNextLifeMode(mNextLifeMode));
            return this;
        }

        public Builder addTranslate(float translateDeltaX, float translateDeltaY) {
            mAnim.addAnimation(new Translate(translateDeltaX, translateDeltaY,
                    0L).setLives(CanvasAnimation.LIVES_LIFE_FOREVER));
            return this;
        }

        public Builder addTranslate(float translateDeltaX, float translateDeltaY, long duration) {
            mAnim.addAnimation(new Translate(translateDeltaX, translateDeltaY,
                    duration).setInterpolator(mInterpolator).setLives(mLives).setNextLifeMode(mNextLifeMode));
            return this;
        }

        public Builder addScale(float scaleDeltaX, float scaleDeltaY,
                                float scaleCenterX, float scaleCenterY, long duration) {
            mAnim.addAnimation(new Scale(scaleDeltaX, scaleDeltaY, scaleCenterX, scaleCenterY,
                    duration).setInterpolator(mInterpolator).setLives(mLives).setNextLifeMode(mNextLifeMode));
            return this;
        }

        public RiddleCanvasAnimation build() {
            return mAnim;
        }
    }

    public static abstract class CanvasAnimation {
        public static final MathFunction INTERPOLATOR_LINEAR = new MathFunction
                .LinearInterpolation(0., 0., 1., 1.);
        public static final MathFunction INTERPOLATOR_ACCELERATE = new MathFunction
                .QuadraticInterpolation(0., 0., 1., 1.);
        public static final MathFunction INTERPOLATOR_DECELERATE = new MathFunction
                .QuadraticInterpolation(1., 1., 0., 0.);

        /**
         * If set to lives, the animation ill never die. Remember to remove animation at some time!
         */
        public static final int LIVES_LIFE_FOREVER = -1;

        /**
         * Cycles from 0 to 1, then repeats. One cycle requires 1 life.
         */
        public static final int NEXT_LIFE_MODE_REPEAT = 0;
        /**
         * Cycles from 0 to 1, 1 to 0, then repeats. One cycle requires 2 lives.
         */
        public static final int NEXT_LIFE_MODE_REVERSE = 1;

        /**
         * Cycles from 0 to 1, 1 to 0, 0 to -1, -1 to 0, then repeats. One cycle requires 4 lives.
         */
        public static final int NEXT_LIFE_MODE_REVERSE_INVERT = 2;

        public static final int DEFAULT_NEXT_LIFE_MODE = NEXT_LIFE_MODE_REPEAT;

        protected long mLifeTime;
        private final long mTotalLifeTime;
        private MathFunction mInterpolator; // evaluated from 0. to 1.
        private int mLives; // dies if lives is zero
        private int mNextLifeMode;

        private int mCurrModeState; //handled by NextLifeMode
        private boolean mLifeIsIncreasing;

        protected CanvasAnimation(long totalLifeTime) {
            mTotalLifeTime = Math.max(totalLifeTime, 0L);
            mLifeTime = 0L;
            mLifeIsIncreasing = true;
            setLives(1);
            setNextLifeMode(DEFAULT_NEXT_LIFE_MODE);
        }

        public CanvasAnimation setInterpolator(MathFunction interpolator) {
            mInterpolator = interpolator;
            return this;
        }

        public CanvasAnimation setLives(int lives) {
            mLives = lives == 0 ? 1 : lives;
            return this;
        }

        public CanvasAnimation setNextLifeMode(int mode) {
            mNextLifeMode = mode;
            mCurrModeState = 0;
            return this;
        }

        protected double getInterpolatedLife(long lifeTime) {
            if (mTotalLifeTime == 0L) {
                return 1.;
            }
            final double lifeFraction = Math.abs(lifeTime) / (double) mTotalLifeTime;
            if (mInterpolator == null) {
                // default linear
                return Math.signum(lifeTime) * lifeFraction;
            }
            return Math.signum(lifeTime) * mInterpolator.evaluate(lifeFraction);
        }

        protected abstract void apply(Canvas canvas);

        public boolean isAlive() {
            return mLives != 0;
        }

        public void update(long updatePeriod) {
            boolean die;
            if (mLifeIsIncreasing) {
                mLifeTime += updatePeriod;
                switch (mNextLifeMode) {
                    case NEXT_LIFE_MODE_REVERSE_INVERT:
                        die = mLifeTime >= mTotalLifeTime
                                || (mLifeTime >= 0L && mCurrModeState == 3);
                        break;
                    default:
                        die = mLifeTime >= mTotalLifeTime;
                        break;
                }
            } else {
                mLifeTime -= updatePeriod;
                switch (mNextLifeMode) {
                    case NEXT_LIFE_MODE_REVERSE_INVERT:
                        die = mLifeTime <= -mTotalLifeTime
                                || (mLifeTime <= 0L && mCurrModeState == 1);
                        break;
                    default:
                        die = mLifeTime <= 0L;
                        break;
                }
            }
            if (die) {
                if (mLives > 0) {
                    mLives--;
                }
                switch (mNextLifeMode) {
                    case NEXT_LIFE_MODE_REVERSE_INVERT:
                        mCurrModeState++;
                        mCurrModeState %= 4;
                        mLifeIsIncreasing = mCurrModeState == 0 || mCurrModeState == 3;
                        break;
                    case NEXT_LIFE_MODE_REVERSE:
                        mCurrModeState++;
                        mCurrModeState %= 2;
                        mLifeIsIncreasing = mCurrModeState == 0;
                        break;
                    case NEXT_LIFE_MODE_REPEAT:
                        mLifeTime = 0L;
                        break;
                    default:
                        //do nothing
                        break;
                }
            }
        }
    }

    private static class Rotate extends CanvasAnimation {
        private float mCenterX;
        private float mCenterY;
        private float mRotateDeltaDegrees;

        protected Rotate(float rotateDeltaDegrees, float centerX, float centerY, long
                totalLifeTime) {
            super(totalLifeTime);
            mRotateDeltaDegrees = rotateDeltaDegrees;
            mCenterX = centerX;
            mCenterY = centerY;
        }

        @Override
        protected void apply(Canvas canvas) {
            float rotate = (float) (getInterpolatedLife(mLifeTime) *
                    mRotateDeltaDegrees);
            canvas.rotate(rotate, mCenterX * canvas.getWidth(), mCenterY * canvas.getHeight());
        }
    }


    private static class Translate extends CanvasAnimation {
        private float mDeltaX;
        private float mDeltaY;

        protected Translate(float deltaX, float deltaY, long
                totalLifeTime) {
            super(totalLifeTime);
            mDeltaX = deltaX;
            mDeltaY = deltaY;
        }

        @Override
        protected void apply(Canvas canvas) {
            float interpolatedLife = (float) getInterpolatedLife(mLifeTime);
            float dx = interpolatedLife * mDeltaX;
            float dy = interpolatedLife * mDeltaY;
            canvas.translate(dx, dy);
        }
    }

    private static class Scale extends CanvasAnimation {
        private float mDeltaX;
        private float mDeltaY;
        private float mCenterX;
        private float mCenterY;

        protected Scale(float deltaX, float deltaY, float centerX, float centerY, long
                totalLifeTime) {
            super(totalLifeTime);
            mDeltaX = deltaX;
            mDeltaY = deltaY;
            mCenterX = centerX;
            mCenterY = centerY;
        }

        @Override
        protected void apply(Canvas canvas) {
            float interpolatedLife = (float) getInterpolatedLife(mLifeTime);
            float dx = interpolatedLife * mDeltaX;
            float dy = interpolatedLife * mDeltaY;
            canvas.scale(dx, dy, mCenterX, mCenterY);
        }
    }
}
