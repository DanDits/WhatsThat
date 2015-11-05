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
    protected boolean performBlending(int frameIndex) {
        return  frameIndex < getCount() - 1 && super.performBlending(frameIndex);
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
