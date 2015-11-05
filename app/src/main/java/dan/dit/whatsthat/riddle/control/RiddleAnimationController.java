package dan.dit.whatsthat.riddle.control;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by daniel on 04.11.15.
 */
public class RiddleAnimationController {

    /**
     * Will be drawn on a cleared canvas before anything else is drawn.
     */
    public static final int LEVEL_GROUNDING = -1;

    /**
     * Will be drawn before any drawing of the riddle happens.
     */
    public static final int LEVEL_BACKGROUND = 0;

    /**
     * Will be drawn after all drawing of the riddle happened.
     */
    public static final int LEVEL_ON_TOP = 100;
    private final List<RiddleAnimation> mAnimations = Collections.synchronizedList(new
            ArrayList<RiddleAnimation>());
    private final OnAnimationCountChangedListener mListener;
    private List<RiddleAnimation> mAnimationsIterateData = new ArrayList<>();
    private long mTime;

    public interface OnAnimationCountChangedListener {
        void onAnimationCountChanged();
    }

    public RiddleAnimationController(@NonNull OnAnimationCountChangedListener listener) {
        mListener = listener;
    }

    // in GameThread
    public void update(long updatePeriod) {
        mTime += updatePeriod; // so birth time is guaranteed to be > 0
        List<RiddleAnimation> animationIterate = mAnimationsIterateData;
        for (int i = 0; i < animationIterate.size(); i++) {
            RiddleAnimation curr = animationIterate.get(i);
            if (curr.mBirthTime <= 0) {
                // not yet born
                if (-curr.mBirthTime <= mTime) {
                    // Happy birth millisecond!!
                    curr.mBirthTime = mTime;
                    curr.onBorn();
                } else {
                    continue; // let time pass and wait
                }
            }
            curr.update(updatePeriod);
            if (!curr.isAlive()) {
                removeAnimation(curr);
            }
        }
    }

    public int getActiveAnimationsCount() {
        return mAnimations.size();
    }

    public void addAnimation(@NonNull RiddleAnimation animation) {
        addAnimation(animation, 0L);
    }

    public void addAnimation(@NonNull RiddleAnimation animation, long delay) {
        mAnimations.add(animation);
        animation.mBirthTime = -(mTime + Math.max(delay, 0L)); // non positive values mark not yet
        // born animation
        synchronized (mAnimations) {
            mAnimationsIterateData = new ArrayList<>(mAnimations);
        }
        mListener.onAnimationCountChanged();
    }

    public void clear() {
        // remove all animations, making sure they are killed
        List<RiddleAnimation> animationIterate = mAnimations;
        for (int i = 0; i < animationIterate.size(); i++) {
            RiddleAnimation curr = animationIterate.get(i);
            curr.onKilled(true);
        }
        mAnimations.clear();
        mAnimationsIterateData.clear();
    }
    private void removeAnimation(@NonNull RiddleAnimation animation) {
        if (mAnimations.remove(animation)) {
            animation.onKilled(false);
            synchronized (mAnimations) {
                mAnimationsIterateData = new ArrayList<>(mAnimations);
            }
            mListener.onAnimationCountChanged();
        }
    }

    // in GameThread
    public void draw(@NonNull Canvas canvas, @Nullable Paint paint, int level) {
        List<RiddleAnimation> animationIterate = mAnimationsIterateData;
        for (int i = 0; i < animationIterate.size(); i++) {
            RiddleAnimation curr = animationIterate.get(i);
            if (curr.mDrawingLevel == level && curr.mBirthTime <= mTime) {
                curr.draw(canvas, paint);
            }
        }
    }

}
