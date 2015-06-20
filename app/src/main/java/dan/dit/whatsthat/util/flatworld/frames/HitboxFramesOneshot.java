package dan.dit.whatsthat.util.flatworld.frames;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by daniel on 18.06.15.
 */
public class HitboxFramesOneshot extends HitboxFrames {
    private boolean mAnimationDone;
    private int mEndFrameIndex;

    public HitboxFramesOneshot(Bitmap[] frames, long animationDuration) {
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
                Log.d("Riddle", "Animation done.");
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
