package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Bitmap;

/**
 * Created by daniel on 18.06.15.
 */
public class FramesOneshot extends Frames {
    private boolean mAnimationDone;

    public FramesOneshot(Bitmap[] frames, long animationDuration) {
        super(frames, animationDuration / (frames.length > 1 ? frames.length - 1 : 1));
    }

    @Override
    public boolean update(long updatePeriod) {
        if (!mAnimationDone) {
            boolean updated = super.update(updatePeriod);
            if (updated && mFrameIndex == 0) {
                mAnimationDone = true;
                mFrameIndex = getCount() - 1;
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
