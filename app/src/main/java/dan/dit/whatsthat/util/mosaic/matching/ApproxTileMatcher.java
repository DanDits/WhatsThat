package dan.dit.whatsthat.util.mosaic.matching;


import android.graphics.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.TreeMap;

import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * Creates a new approximating tile matcher offering constant
 * matching speed. A low accuracy and a small variety of MosaicTiles will yield
 * worse results. For huge numbers of MosaicTiles and calls to getBestResult() this
 * matcher is your best friend! 1 million matches on 1 million mosaic tiles take less than
 * 20 seconds for all accuracies in comparison to 20 minutes taken by the {@link LinearTileMatcher}.
 * This matcher cannot use the alpha value.
 * @author Daniel
 *
 */
public class ApproxTileMatcher<S> extends TileMatcher<S> {
	private static final int WORST_SIMILARITY = 3 * 255 * 255;
	private int count;
	private double accuracy;
	private RedMap dataMap;
	private TreeMap<Integer, ArrayList<MosaicTile<S>>> bestMatchData;
	private Random mRand = new Random();

	/**
	 * Creates a new approximating TileMatcher which matches the MosaicTiles
	 * contained in the given MosaicTileDatabase.
	 * @param data The data which's tiles are matched.
	 * @param accuracy The accuracy of the matcher. A value from 0 to 1 with
	 * 0 indicating bad matching and 1 best matching.
	 */
	public ApproxTileMatcher(Collection<? extends MosaicTile<S>> data, double accuracy) {
		super(false, null);
		double normedAcc;
		if (accuracy < 0) {
			normedAcc = 0.0;
		} else if (accuracy > 1.0) {
			normedAcc = 1.0;
		} else {
			normedAcc = accuracy;
		}
		this.accuracy = normedAcc;
		this.bestMatchData = new TreeMap<>();
		this.initMatcher(data);
	}
	
	/**
	 * Inits the matcher with a list of MosaicTiles to be matched.
	 * @param tiles The list of tiles that will be matched.
	 */
	private void initMatcher(Collection<? extends MosaicTile<S>> tiles) {
		this.dataMap = new RedMap();
		this.count = tiles.size();
		for (MosaicTile<S> tile : tiles) {
			int red = tile.getAverageRed();
			int green = tile.getAverageGreen();
			int blue = tile.getAverageBlue();
			GreenMap greenMap = this.dataMap.get(red);
			if (greenMap == null) {
				greenMap = new GreenMap();
				this.dataMap.put(red, greenMap);
			}
			BlueMap blueMap = greenMap.get(green);
			if (blueMap == null) {
				blueMap = new BlueMap();
				greenMap.put(green, blueMap);
			}
			ArrayList<MosaicTile<S>> sameColoredTiles = blueMap.get(blue);
			if (sameColoredTiles == null) {
				sameColoredTiles = new ArrayList<>();
				blueMap.put(blue, sameColoredTiles);
			}
			sameColoredTiles.add(tile);
		}
	}
	

	@Override
	public MosaicTile<S> calculateBestMatch(int withRGB) {
		ArrayList<MosaicTile<S>> hashedTileList = this.bestMatchData.get(withRGB);
		if (hashedTileList != null && !hashedTileList.isEmpty()) {
			return hashedTileList.get((int) (Math.random() * hashedTileList.size()));
			// tile list size can only be zero if a tile of this list was removed, in this case
			// we need to search again and refresh the list
		}
		final int wantedRed = Color.red(withRGB);
		final int wantedGreen = Color.green(withRGB);
		final int wantedBlue = Color.blue(withRGB);
		int bestSimilarity = ApproxTileMatcher.WORST_SIMILARITY;
		ArrayList<MosaicTile<S>> bestTileList = null;
		
		// this algorithm uses the special divided map iterator since it allows iteration
		// near the wanted key color, if the colors of the matchers tiles are well spread, this is extremly fast
		// start by iteration through the redmap, split at the wanted red
		DividedTreeMapIterator<Integer, GreenMap> redIt 
			= new DividedTreeMapIterator<>(this.dataMap, wantedRed);
		int remainingSearchesRed = (int) (this.dataMap.size() * this.accuracy);
		Integer currRedValue;
		do {
			currRedValue = redIt.next(
					wantedRed - bestSimilarity, 
					wantedRed + bestSimilarity);
			if (currRedValue != null) {
				
				// continue by iterating through the current greenmap, split at wanted green
				GreenMap currGreen = this.dataMap.get(currRedValue);
				int remainingSearchesGreen = (int) (currGreen.size() * this.accuracy);
				DividedTreeMapIterator<Integer, BlueMap> greenIt 
					= new DividedTreeMapIterator<>(currGreen, wantedGreen);
				Integer currGreenValue;
				do {
					currGreenValue = greenIt.next(
							wantedGreen - bestSimilarity, 
							wantedGreen + bestSimilarity);
					if (currGreenValue != null) {
						
						// finally iterate through the current blue map, split at wanted blue
						BlueMap currBlue = currGreen.get(currGreenValue);
						int remainingSearchesBlue = (int) (currBlue.size() * this.accuracy);
						DividedTreeMapIterator<Integer, ArrayList<MosaicTile<S>>> blueIt
							= new DividedTreeMapIterator<>(currBlue, wantedBlue);
						Integer currBlueValue;
						do {
							currBlueValue = blueIt.next(
									wantedBlue - bestSimilarity, 
									wantedBlue + bestSimilarity);
							if (currBlueValue != null) {
								
								// check this RGB for distance to the wanted RGB
								ArrayList<MosaicTile<S>> currList = currBlue.get(currBlueValue);
								int currSimilarity = (wantedRed - currRedValue) * (wantedRed - currRedValue)
										+ (wantedGreen - currGreenValue) * (wantedGreen - currGreenValue) 
										+ (wantedBlue - currBlueValue) * (wantedBlue - currBlueValue);
								
								if (currSimilarity < bestSimilarity) {
									bestSimilarity = currSimilarity;
									bestTileList = currList;
								}
								remainingSearchesBlue--;
							}
						} while (currBlueValue != null && remainingSearchesBlue > 0);
						remainingSearchesGreen--;
					}
				} while (currGreenValue != null && remainingSearchesGreen > 0);
				remainingSearchesRed--;
			}
		} while (currRedValue != null && remainingSearchesRed > 0);
		
		if (bestTileList != null) {
			this.bestMatchData.put(withRGB, bestTileList);
			// pick a random one of the list as they all have the same distance
			// to the wanted RGB and save it and the distance 
			return bestTileList.get((int) (mRand.nextDouble() * bestTileList.size()));
		} else {
			return null;
		}
	}
	
	@Override
	public boolean removeTile(MosaicTile<S> toRemove) {
		if (toRemove != null) {
			int red = toRemove.getAverageRed();
			int green = toRemove.getAverageGreen();
			int blue = toRemove.getAverageBlue();
			GreenMap greenMap = this.dataMap.get(red);
			// first search if the tile is stored by this matcher, if it
			// is stored, then we find it directly by its rgba values
			if (greenMap != null) {
				BlueMap blueMap = greenMap.get(green);
				if (blueMap != null) {
					ArrayList<MosaicTile<S>> tiles = blueMap.get(blue);
					if (tiles != null && tiles.remove(toRemove)) {
						//  mosaic tile was stored by this matcher
						// check if the list and color maps are empty and
						// must be removed
						if (tiles.size() == 0) {
							blueMap.remove(blue);
							if (blueMap.size() == 0) {
								greenMap.remove(green);
								if (greenMap.size() == 0) {
									this.dataMap.remove(red);
								}
							}
						}
						this.count--;
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public double getAccuracy() {
		return this.accuracy;
	}
	
	@Override
	public int getUsedTilesCount() {
		return this.count;
	}
	
	/**
	 * A simply wrapper class for readability. The last axis in the three dimensional
	 * color space containing the blue component sorted in ascending order and at each
	 * entry all tiles with this blue value.
	 * @author Daniel
	 *
	 */
	private class BlueMap extends TreeMap<Integer, ArrayList<MosaicTile<S>>> {
		private static final long serialVersionUID = 1L;
		
	}
	
	/**
	 * A simply wrapper class for readability. The second axis in the three dimensional
	 * color space containing the green component sorted in ascending order and at each
	 * entry a sorted blue map.
	 * @author Daniel
	 *
	 */
	private class GreenMap extends TreeMap<Integer, BlueMap> {
		private static final long serialVersionUID = 1L;
		
	}
	
	/**
	 * A simply wrapper class for readability. The first axis in the three dimensional
	 * color space containing the red component sorted in ascending order and at each
	 * entry a sorted green map.
	 * @author Daniel
	 *
	 */
	private class RedMap extends TreeMap<Integer, GreenMap> {
		private static final long serialVersionUID = 1L;
		
	}
}
