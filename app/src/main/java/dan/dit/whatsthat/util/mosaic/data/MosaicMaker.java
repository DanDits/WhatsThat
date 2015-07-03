package dan.dit.whatsthat.util.mosaic.data;

import android.graphics.Bitmap;
import android.util.Log;

import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;
import dan.dit.whatsthat.util.mosaic.reconstruction.ClusteredLayerReconstructor;
import dan.dit.whatsthat.util.mosaic.reconstruction.DominantLayerReconstructor;
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
	private final boolean mUseAlpha;
	private ColorMetric mColorMetric;

	public MosaicMaker(TileMatcher<S> tileMatcher, BitmapSource<S> bitmapSource, boolean useAlpha, ColorMetric metric) {
		if (tileMatcher == null || bitmapSource == null) {
			throw new IllegalArgumentException("No matcher or source given.");
		}
		mMatcher = tileMatcher;
		mBitmapSource = bitmapSource;
        mUseAlpha = useAlpha;
		mColorMetric = metric;
	}

    public Bitmap makeMultiRect(Bitmap source, int wantedRows, int wantedColumns, double mergeFactor, PercentProgressListener progress) {
        Reconstructor reconstructor = new MultiRectReconstructor(source,
                wantedRows, wantedColumns, mergeFactor, mUseAlpha, mColorMetric);
        return make(reconstructor, progress, 0);
    }

    public Bitmap makeRect(Bitmap source, int wantedRows, int wantedColumns, PercentProgressListener progress) {
        Reconstructor reconstructor = new RectReconstructor(source,
                wantedRows, wantedColumns);
        return make(reconstructor, progress, 0);
    }

    public Bitmap makeDominantLayer(Bitmap source, double mergeFactor, final PercentProgressListener progress) {
        final int progressForInit = 50;
        Reconstructor reconstructor = new DominantLayerReconstructor(source, mergeFactor, mUseAlpha, mColorMetric,
                new PercentProgressListener() {

                    @Override
                    public void onProgressUpdate(int p) {
                        progress.onProgressUpdate((int) (progressForInit * p / 100.));
                    }
                });
        return make(reconstructor, progress, progressForInit);
    }

    public Bitmap makeClusteredLayer(Bitmap source, int clusterCount, final PercentProgressListener progress) {
        final int progressForInit = 50;
        Reconstructor reconstructor = new ClusteredLayerReconstructor(source, clusterCount, mUseAlpha, mColorMetric,
                new PercentProgressListener() {

                    @Override
                    public void onProgressUpdate(int p) {
                        progress.onProgressUpdate((int) (progressForInit * p / 100.));
                    }
                });
        return make(reconstructor, progress, progressForInit);
    }

	private Bitmap make(Reconstructor reconstructor, PercentProgressListener progress, int startProgress) {
		if (reconstructor == null) {
			throw new IllegalArgumentException("No reconstructor given to make mosaic.");
		}

		while (!reconstructor.hasAll()) {
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
                progress.onProgressUpdate((int) (startProgress + reconstructor.estimatedProgressPercent() * (100 - startProgress) / 100.));
            }
		}

		return reconstructor.getReconstructed();
	}
}
