package dan.dit.whatsthat.util.mosaic.matching;

import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * Created by daniel on 05.12.15.
 */
public class TrivialMatcher<S> extends TileMatcher<S> {
    private NullTile mTile;
    public TrivialMatcher() {
        super(true, ColorMetric.Absolute.INSTANCE);
        mTile = new NullTile();
    }

    private class NullTile implements MosaicTile<S> {
        protected int mAverageColor;
        @Override
        public S getSource() {
            return null;
        }

        @Override
        public int getAverageARGB() {
            return mAverageColor;
        }
    }

    @Override
    protected MosaicTile<S> calculateBestMatch(int withRGB) {
        mTile.mAverageColor = withRGB;
        return mTile;
    }

    @Override
    public double getAccuracy() {
        return 1.;
    }

    @Override
    public boolean setAccuracy(double accuracy) {
        return false;
    }

    @Override
    public boolean removeTile(MosaicTile<S> toRemove) {
        return false;
    }

    @Override
    public int getUsedTilesCount() {
        return 1;
    }

    @Override
    public void setColorMetric(ColorMetric metric) {

    }
}
