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

package dan.dit.whatsthat.util.mosaic.data;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import dan.dit.whatsthat.util.general.MultistepPercentProgressListener;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;
import dan.dit.whatsthat.util.mosaic.reconstruction.AutoLayerReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.FixedLayerReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.MosaicFragment;
import dan.dit.whatsthat.util.mosaic.reconstruction.MultiRectReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.Reconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.RectReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.pattern.CirclePatternReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.pattern.LegoPatternReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.pattern.PatternReconstructor;

/**
 * This class uses a tile matcher as a source pool for MosaicTiles and reconstructs
 * an mosaic for a source bitmap with some Reconstructor type.
 * @param <S>
 */
public class MosaicMaker<S> {
	private final BitmapSource<S> mBitmapSource;
	private TileMatcher<S> mMatcher;
	private boolean mUseAlpha;
	private ColorMetric mColorMetric;

    public interface ProgressCallback extends PercentProgressListener {
        boolean isCancelled();
    }

    private static class MultiStepPercentProgressCallback extends MultistepPercentProgressListener implements ProgressCallback {

        private final ProgressCallback mCallback;

        public MultiStepPercentProgressCallback(ProgressCallback callback, int steps) {
            super(callback, steps);
            mCallback = callback;
        }


        @Override
        public boolean isCancelled() {
            return mCallback.isCancelled();
        }
    }

	public MosaicMaker(TileMatcher<S> tileMatcher, BitmapSource<S> bitmapSource, boolean useAlpha, ColorMetric metric) {
		if (tileMatcher == null || bitmapSource == null) {
			throw new IllegalArgumentException("No matcher or source given.");
		}
		mMatcher = tileMatcher;
		mBitmapSource = bitmapSource;
        mUseAlpha = useAlpha;
        setColorMetric(metric);
	}

	public void setUseAlpha(boolean useAlpha) {
		mUseAlpha = useAlpha;
		mMatcher.setUseAlpha(useAlpha);
	}

    public void setColorMetric(ColorMetric metric) {
        mColorMetric = metric;
        if (mColorMetric == null) {
            throw new IllegalArgumentException("No color metric given.");
        }
        mMatcher.setColorMetric(metric);
    }

    public Bitmap makeMultiRect(Bitmap source, int wantedRows, int wantedColumns, double mergeFactor, ProgressCallback progress) {
        Reconstructor reconstructor = new MultiRectReconstructor(source,
                wantedRows, wantedColumns, mergeFactor, mUseAlpha, mColorMetric);
        return make(mMatcher, mBitmapSource, reconstructor, progress);
    }

    public Bitmap makeRect(Bitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        Reconstructor reconstructor = new RectReconstructor(source,
                wantedRows, wantedColumns);
        return make(mMatcher, mBitmapSource, reconstructor, progress);
    }

    public Bitmap makeAutoLayer(Bitmap source, double mergeFactor, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        Reconstructor reconstructor = new AutoLayerReconstructor(source, mergeFactor, mUseAlpha, mColorMetric, multiProgress);
		multiProgress.nextStep();
        Bitmap result = make(mMatcher, mBitmapSource, reconstructor, multiProgress);
		multiProgress.nextStep();
        return result;
    }

    public Bitmap makeFixedLayer(Bitmap source, int clusterCount, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        Reconstructor reconstructor = new FixedLayerReconstructor(source, clusterCount, mUseAlpha, mColorMetric, multiProgress);
        multiProgress.nextStep();
        Bitmap result = make(mMatcher, mBitmapSource, reconstructor, multiProgress);
        multiProgress.nextStep();
        return result;
    }

    public static Bitmap makePattern(Resources res, Bitmap source, String patternName,
                                     boolean useAlpha, ColorMetric metric,
                                     int rows, int columns, ProgressCallback progress) {
        MultiStepPercentProgressCallback multiProgress = new MultiStepPercentProgressCallback(progress, 2);
        PatternReconstructor reconstructor;
        switch (patternName) {
            default:
                // fall through
            case CirclePatternReconstructor.NAME:
                reconstructor = new CirclePatternReconstructor(source, rows, columns, Color
                        .TRANSPARENT);
                break;
            case LegoPatternReconstructor.NAME:
                reconstructor = new LegoPatternReconstructor(res, source, rows, columns, Color
                        .TRANSPARENT);
                break;
        }
        multiProgress.nextStep();
        Bitmap result = make(reconstructor.<Void>makeMatcher(useAlpha, metric), reconstructor
                        .<Void>makeSource(),
                reconstructor,
                multiProgress);
        multiProgress.nextStep();
        return result;
    }

	private static <S>Bitmap make(TileMatcher<S> matcher, BitmapSource<S> source, Reconstructor
            reconstructor,
                               ProgressCallback progress) {
		if (reconstructor == null) {
			throw new IllegalArgumentException("No reconstructor given to make mosaic.");
		}

		while (!reconstructor.hasAll() && (progress == null || !progress.isCancelled())) {
			MosaicFragment nextFrag = reconstructor.nextFragment();
			Bitmap nextImage;
			do {
				MosaicTile<S> tile = matcher.getBestMatch(nextFrag.getAverageRGB());
				if (tile == null) {
					// matcher has no more tiles!
                    Log.e("HomeStuff", "Mosaic maker ran out of tiles!");
					return null;
				}
				nextImage = source.getBitmap(tile, nextFrag.getWidth(), nextFrag.getHeight());

				if (nextImage == null) {
					// no image?! maybe the image (file) got invalid (image deleted, damaged,...)
					// delete it from matcher
					// and cache and search again
					matcher.removeTile(tile);
				}
				// will terminate since the matcher will lose a tile each iteration or find a valid one, 
				// if no tile found anymore, returns false
			} while (nextImage == null);

			if (!reconstructor.giveNext(nextImage)) {
				// reconstructor did not accept the give image, but it was valid and of correct dimension, 
				// does not occur
				// with RectReconstructor / MultiRectReconstructor, but who knows, if I ever forget 
				// this I would be stuck here
                Log.e("HomeStuff", "Given image not accepted by reconstructor!");
				return null;
			}
            if (progress != null) {
                progress.onProgressUpdate(reconstructor.estimatedProgressPercent());
            }
		}
		if (progress != null && !progress.isCancelled()) {
			progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
		}
        if (progress != null && progress.isCancelled()) {
            return null;
        }
		return reconstructor.getReconstructed();
	}

    public boolean usesAlpha() {
        return mUseAlpha;
    }

    public ColorMetric getColorMetric() {
        return mColorMetric;
    }
}
