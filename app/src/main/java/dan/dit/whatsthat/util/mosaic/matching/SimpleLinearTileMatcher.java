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

package dan.dit.whatsthat.util.mosaic.matching;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * This class implements a {@link TileMatcher}. This is a very basic
 * TileMatcher, very slow (no caching) in O(n) with n being the amount
 * of tiles given in the database. But maybe useful if you create
 * a new Matcher and want to check the results...<br>
 * Has accuracy 1.0
 * @author Daniel
 *
 */
public class SimpleLinearTileMatcher<S> extends TileMatcher<S> {
	private List<MosaicTile<S>> tiles;
	
	/**
	 * Creates a new SimpleTileMatcher from the MosaicTiles in the given database, optionally
	 * alpha is used for matching.
	 * @param data The data to take the MosaicTiles of.
	 * @param useAlpha If alpha should be used for matching.
	 */
	public SimpleLinearTileMatcher(Collection<? extends MosaicTile<S>> data, boolean useAlpha, ColorMetric metric) {
		super(useAlpha, metric);
		this.tiles = new LinkedList<>(data);
	}

	@Override
	public MosaicTile<S> calculateBestMatch(int withRGB) {
		double bestSim = Double.MAX_VALUE;
		MosaicTile<S> bestTile = null;
		for (MosaicTile<S> tile : this.tiles) {
			double sim = mColorMetric.getDistance(tile.getAverageARGB(), withRGB, useAlpha);
			if (sim < bestSim) {
				bestSim = sim;
				bestTile = tile;
			}
		}
		return bestTile;
	}

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
		return this.tiles.remove(toRemove);
	}

	@Override
	public int getUsedTilesCount() {
		return this.tiles.size();
	}

	@Override
	public void setColorMetric(ColorMetric metric) {
		mColorMetric = metric;
	}

}
