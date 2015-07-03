package dan.dit.whatsthat.util.mosaic.matching;


import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * This class models an abstract TileMatcher which can with a certain
 * accuracy give the best matching mosaic tile to a given rgb color.
 * @author Daniel
 *
 */
public abstract class TileMatcher<S> {

    protected final ColorMetric mColorMetric;
    private Map<Integer, MosaicTile<S>> mBestMatches;

    public void setHashMatches(boolean hashMatches) {
        if (hashMatches && mBestMatches == null) {
            mBestMatches = new HashMap<>();
        } else if (!hashMatches) {
            mBestMatches = null;
        }
    }

    protected MosaicTile<S> getBestMatchHashed(int color) {
        if (mBestMatches == null) {
            throw new IllegalStateException("Hash not used!");
        }
        MosaicTile<S> match = mBestMatches.get(color);
        if (match != null) {
            return match;
        }
        match = calculateBestMatch(color);
        mBestMatches.put(color, match);
        return match;
    }

	/**
	 * If the TileMatcher uses the alpha value of the rgb values for matching.
	 */
	protected final boolean useAlpha;
	
	/**
	 * Creates a new tile matcher, the given flag simply signals
	 * if the implementing tile matcher uses alpha for matching.
     * @param useAlpha If the matcher uses the alpha value for matching.
     * @param metric The color metric to use, if null defaults to Euclid2.
     */
	protected TileMatcher(boolean useAlpha, ColorMetric metric) {
		this.useAlpha = useAlpha;
        mColorMetric = metric == null ? ColorMetric.Euclid2.INSTANCE : metric;
	}

	/**
	 * Returns the best matching MosaicTile for the given rgb color.
	 * The result and speed of this calculation highly depends on the tile matcher.
	 * Higher accuracy usually returns better results at a cost of speed.
	 * @param withRGB The rgb to match. If the alpha value is used can be requested by
	 * usesAlpha().
	 * @return The best matching mosaic tile. If the tile matcher has tile data
	 * this will never be <code>null</code>.
	 */
	protected abstract MosaicTile<S> calculateBestMatch(int withRGB);

    public MosaicTile<S> getBestMatch(int color) {
        if (mBestMatches == null) {
            return calculateBestMatch(color);
        }
        return getBestMatchHashed(color);
    }

	/**
	 * Returns the accuracy of the best match. 
	 * @return A value from <code>0</code> (bad accuracy) to
	 * <code>1</code> (best result).
	 */
	public abstract double getAccuracy();
	
	/**
	 * Removes one occurance of the given MosaicTile from the TileMatcher. This operation can
	 * be performed during matching and is useful to eleminate tiles which reference
	 * an invalid image file (which got deleted or is unaccesable) or MosaicTiles that should not be used
	 * anymore for any other reason.
	 * @param toRemove The MosaicTile to remove.
	 * @return <code>true</code> only if the tile was contained in the set of MosaicTiles
	 * of this matcher and then removed.
	 */
	public abstract boolean removeTile(MosaicTile<S> toRemove);
	
	/**
	 * Returns the amount of MosaicTiles used by this TileMatcher.
	 * @return The amount of MosaicTiles used by this TileMatcher.
	 */
	public abstract int getUsedTilesCount();
	
	/**
	 * Returns <code>true</code> if calculateBestMatch() takes the alpha
	 * value of the rgb color codes into account.
	 * @return If this matcher uses alpha.
	 */
	public boolean usesAlpha() {
		return this.useAlpha;
	}
}
