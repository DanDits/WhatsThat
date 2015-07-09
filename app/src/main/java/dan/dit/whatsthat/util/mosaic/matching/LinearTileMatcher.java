package dan.dit.whatsthat.util.mosaic.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * This class implements a {@link TileMatcher} by matching
 * all tiles linearly with an accuracy of 1.0 and optionally using
 * the alpha value of the rgb codes.
 * @author Daniel //TODO not working or super slow?
 *
 */
public class LinearTileMatcher<S> extends TileMatcher<S> {
	private int count;
	private TreeMap<Double, ArrayList<MosaicTile<S>>> allTiles;
	
	/**
	 * Creates a new LinearTileMatcher which matches the tiles contained in the
	 * given MosaicTileDatabase.
	 * @param data The data which's tiles will be matched.
	 * @param useAlpha If the tile matcher should use the alpha value for matching.
	 */
	public LinearTileMatcher(Collection<? extends MosaicTile<S>> data, boolean useAlpha, ColorMetric metric) {
		super(useAlpha, metric);
		this.allTiles = new TreeMap<>();
		this.count = this.allTiles.size();
		for (MosaicTile<S> tile : data) {
			double norm = ColorAnalysisUtil.norm(tile.getAverageARGB(), this.useAlpha);
			if (!this.allTiles.containsKey(norm)) {
				this.allTiles.put(norm, new ArrayList<MosaicTile<S>>());
			}
			this.allTiles.get(norm).add(tile);
		}
	}
	
	/**
	 * Returns the best match of this tile matcher. (Improved) linear
	 * complexity, can use alpha value. Always gives best results (accuracy 1.0).
	 * @param withRGB The RGB(A) color code to match.
	 * @return The best matching mosaic tile.
	 */
	@Override
	public MosaicTile<S> calculateBestMatch(int withRGB) {
		double wantedRGBNorm = ColorAnalysisUtil.norm(withRGB, this.useAlpha);
		double bestSimilarity = mColorMetric.maxValue(useAlpha);
		double shortestDistance = Math.sqrt(bestSimilarity);
		MosaicTile<S> shortestTile = null;
		DividedTreeMapIterator<Double, ArrayList<MosaicTile<S>>> it
			= new DividedTreeMapIterator<>(this.allTiles, wantedRGBNorm);
		ArrayList<MosaicTile<S>> curr;
		Double currKey;
		do {
			
			// search near the wanted norm, this will skip many but not all unnecessary checks
			currKey = it.next(wantedRGBNorm - shortestDistance, wantedRGBNorm + shortestDistance);
			if (currKey != null) {
				curr = this.allTiles.get(currKey);
				// iterate through all with the same norm, checking their distance in 3 or 4 dimensional color space
				for (MosaicTile<S> tile : curr) {
					double currSimilarity = mColorMetric.getDistance(withRGB, tile.getAverageARGB(), this.useAlpha);
					if (currSimilarity < bestSimilarity) {
						bestSimilarity = currSimilarity;
						shortestDistance = ColorAnalysisUtil.norm(tile.getAverageARGB(), this.useAlpha);
						shortestTile = tile;
					}	
				}
			}
		} while (currKey != null);
		//System.out.print("Got this with distance " + shortestDistance + " ");
		return shortestTile;
	}

	/**
	 * Returns 1.0 as this matcher has best results.
	 * @return Always 1.0.
	 */
	@Override
	public double getAccuracy() {
		return 1.0;
	}

	@Override
	public boolean setAccuracy(double accuracy) {
		return false;
	}

	@Override
	public boolean removeTile(MosaicTile toRemove) {
		double norm = ColorAnalysisUtil.norm(toRemove.getAverageARGB(), this.useAlpha);
		if (this.allTiles.containsKey(norm)) {
			boolean contained = this.allTiles.get(norm).remove(toRemove);
			if (contained) {
				this.count--;
			}
			return contained;
		} else {
			return false;
		}
	}

	@Override
	public int getUsedTilesCount() {
		return this.count;
	}
}
