package dan.dit.whatsthat.util.mosaic.data;

import android.graphics.Bitmap;
import android.util.Log;

import dan.dit.whatsthat.util.MultistepPercentProgressListener;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;
import dan.dit.whatsthat.util.mosaic.reconstruction.AutoLayerReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.FixedLayerReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.MosaicFragment;
import dan.dit.whatsthat.util.mosaic.reconstruction.MultiRectReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.Reconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.RectReconstructor;

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
        return make(reconstructor, progress);
    }

    public Bitmap makeRect(Bitmap source, int wantedRows, int wantedColumns, ProgressCallback progress) {
        Reconstructor reconstructor = new RectReconstructor(source,
                wantedRows, wantedColumns);
        return make(reconstructor, progress);
    }

    public Bitmap makeAutoLayer(Bitmap source, double mergeFactor, ProgressCallback progress) {
		MultistepPercentProgressListener multiProgress = new MultistepPercentProgressListener(progress, 2);
        Reconstructor reconstructor = new AutoLayerReconstructor(source, mergeFactor, mUseAlpha, mColorMetric, progress);
		multiProgress.nextStep();
        Bitmap result = make(reconstructor, progress);
		multiProgress.nextStep();
        return result;
    }

    public Bitmap makeFixedLayer(Bitmap source, int clusterCount, ProgressCallback progress) {
        MultistepPercentProgressListener multiProgress = new MultistepPercentProgressListener(progress, 2);
        Reconstructor reconstructor = new FixedLayerReconstructor(source, clusterCount, mUseAlpha, mColorMetric, progress);
        multiProgress.nextStep();
        Bitmap result = make(reconstructor, progress);
        multiProgress.nextStep();
        return result;
    }

	private Bitmap make(Reconstructor reconstructor, ProgressCallback progress) {
		if (reconstructor == null) {
			throw new IllegalArgumentException("No reconstructor given to make mosaic.");
		}

		while (!reconstructor.hasAll() && (progress == null || !progress.isCancelled())) {
			MosaicFragment nextFrag = reconstructor.nextFragment();
			Bitmap nextImage;
			do {
				MosaicTile<S> tile = mMatcher.getBestMatch(nextFrag.getAverageRGB());
				if (tile == null) {
					// matcher has no more tiles!
                    Log.e("HomeStuff", "Mosaic maker ran out of tiles!");
					return null;
				}
				nextImage = mBitmapSource.getBitmap(tile, nextFrag.getWidth(), nextFrag.getHeight());

				if (nextImage == null) {
					// no image?! maybe the image (file) got invalid (image deleted, damaged,...)
					// delete it from matcher
					// and cache and search again
					mMatcher.removeTile(tile);
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
