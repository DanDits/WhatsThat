package dan.dit.whatsthat.riddle.control;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 04.11.15.
 */
public abstract class RiddleAnimation {
    final int mDrawingLevel; // allows controlling drawing order of animations and clients of the
    // controller to draw levels where appropriate

    long mBirthTime; // allows adding a delay for the animation
    private StateListener mListener;

    public interface StateListener {
        void onBorn();
        void onKilled(boolean murdered);
    }

    protected RiddleAnimation(int drawingLevel) {
        mDrawingLevel = drawingLevel;
    }

    public RiddleAnimation setStateListener(StateListener listener) {
        mListener = listener;
        return this;
    }

    abstract boolean isAlive();

    /**
     * Murders this RiddleAnimation. Will invoke onKilled after onMurdered.
     * @return If the animation was alive before being murdered.
     */
    public boolean murder() {
        boolean wasAlive = isAlive();
        onMurdered();
        onKilled(true);
        return wasAlive;
    }

    public abstract void onMurdered();

    /**
     * The RiddleAnimation updates itself, the behavior is defined by the animation. This is only
     * invoked if the animation was just born or still alive after the last update.
     * @param updatePeriod The amount of milliseconds of time passed.
     */
    protected abstract void update(long updatePeriod);

    public abstract void draw(Canvas canvas, Paint paint);

    /**
     * Invoked when the animation was killed. If the animation was alive (isAlive() was true)
     * then the animation was murdered. This can happen when the riddle is closing, treatment can
     * take this into account and make lightweight drawing/actions. If this method is overwritten
     * the parent method should be invoked unless the listener shall not be informed of this event.
     * @param murdered If the animation was murdered.
     */
    protected void onKilled(boolean murdered) {
        StateListener listener = mListener;
        if (listener != null) {
            listener.onKilled(murdered);
        }
    }

    public void onBorn() {
        StateListener listener = mListener;
        if (listener != null) {
            listener.onBorn();
        }
    }
}
