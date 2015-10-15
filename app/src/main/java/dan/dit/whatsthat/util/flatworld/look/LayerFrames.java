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
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 06.06.15.
 */
public class LayerFrames extends Frames {

    private int mLayers;
    private Bitmap[][] mFrameLayers;

    public LayerFrames(Bitmap[] frames, long frameDuration, int backgroundLayers) {
        super(frames, frameDuration);
        mLayers = backgroundLayers;
        if (backgroundLayers < 1) {
            throw new IllegalArgumentException("Use Frames for no background layers.");
        }
        mFrameLayers = new Bitmap[frames.length][mLayers];
    }

    public void setBackgroundLayerBitmap(int frame, int layer, Bitmap layerImage) {
        mFrameLayers[frame][layer] = layerImage;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, Paint paint) {
        if (mVisible) {
            for (int i = 0; i < mLayers; i++) {
                Bitmap currFrame = mFrameLayers[mFrameIndex][i];
                if (currFrame != null) {
                    canvas.drawBitmap(currFrame, x - currFrame.getWidth() + getWidth() + mOffsetX, y - currFrame.getHeight() + getHeight() + mOffsetY, paint);
                }
            }
        }
        super.draw(canvas, x, y, paint);
    }
}
