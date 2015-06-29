package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Bitmap;

/**
 * Created by daniel on 18.06.15.
 */
public class FramesOneshot extends Frames {
    private boolean mAnimationDone;
    private int mEndFrameIndex;

    public FramesOneshot(Bitmap[] frames, long animationDuration) {
        super(frames, animationDuration / (frames.length > 1 ? frames.length - 1 : 1));
    }

    public void setEndFrameIndex(int index) {
        mEndFrameIndex = index;
    }

    @Override
    public boolean update(long updatePeriod) {
        if (!mAnimationDone) {
            boolean updated = super.update(updatePeriod);
            if (updated && mFrameIndex == 0) {
                mAnimationDone = true;
                mFrameIndex = mEndFrameIndex;
            }
            return updated;
        }
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        mAnimationDone = false;
    }
}
