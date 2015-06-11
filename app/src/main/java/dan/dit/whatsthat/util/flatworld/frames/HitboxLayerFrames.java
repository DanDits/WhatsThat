package dan.dit.whatsthat.util.flatworld.frames;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by daniel on 06.06.15.
 */
public class HitboxLayerFrames extends HitboxFrames {

    private int mLayers;
    private Bitmap[][] mFrameLayers;

    public HitboxLayerFrames(Bitmap[] frames, long frameDuration, int backgroundLayers) {
        super(frames, frameDuration);
        mLayers = backgroundLayers;
        if (backgroundLayers < 1) {
            throw new IllegalArgumentException("Use HitboxFrames for no background layers.");
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
