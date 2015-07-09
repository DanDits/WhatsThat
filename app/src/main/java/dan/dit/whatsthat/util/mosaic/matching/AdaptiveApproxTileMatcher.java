package dan.dit.whatsthat.util.mosaic.matching;

import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * Searches for best matching tiles in an approximating way jumping
 * from one tile to another, always improving on each step. Best used for large
 * data sets as only initialization is in O(n²) but searching approximately constant.
 * Good results (and not improving very
 * much from this) starting with accuracy = 0.2.<br>
 *     Initialization will take longer (O(n²) in n = data size), searching is linear in the worst case
 *     (for accuracy = 1) and else
 * Created by daniel on 03.07.15.
 */
public class AdaptiveApproxTileMatcher<S> extends TileMatcher<S> {


    private double mAccuracy;
    private Map<Integer, SortedMap<Double, Integer>> mDataDistances;
    private Map<Integer, MosaicTile<S>> mData;
    private int mCurrentColorApprox;

    public AdaptiveApproxTileMatcher(Collection<? extends MosaicTile<S>> data, double accuracy, boolean useAlpha, ColorMetric metric) {
        super(useAlpha, metric);
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data given.");
        }
        setAccuracy(accuracy);
        init(data);
    }

    /**
     * Sets the accuracy to the new value, if not exactly equal to the old value
     * this will clear the hashed matches.
     * @param accuracy The new accuracy to set
     * @return true as this matcher uses accuracy.
     */
    @Override
    public boolean setAccuracy(double accuracy) {
        double oldAccuracy = mAccuracy;
        mAccuracy = Math.max(Math.min(accuracy, 1.), 0.);
        if (oldAccuracy != mAccuracy) {
            resetHashMatches();
        }
        return true;
    }

    private void init(Collection<? extends MosaicTile<S>> data) {
        mData = new HashMap<>();
        for (MosaicTile<S> tile : data) {
            mData.put(tile.getAverageARGB(), tile);
        }
        mDataDistances = new HashMap<>(data.size());
        for (MosaicTile<S> tile : data) {
            int tileColor = tile.getAverageARGB();
            SortedMap<Double, Integer> distanceMap = new TreeMap<>();
            for (MosaicTile<S> otherTile : data) {
                int otherTileColor = otherTile.getAverageARGB();
                distanceMap.put(mColorMetric.getDistance(otherTileColor, tileColor, useAlpha), otherTileColor);
            }
            mDataDistances.put(tileColor, distanceMap);
        }
        mCurrentColorApprox = mData.keySet().iterator().next();
    }

    @Override
    protected MosaicTile<S> calculateBestMatch(int targetColor) {
        int currentColorApprox = mCurrentColorApprox;
        double tolerance = mColorMetric.maxValue(useAlpha) * mAccuracy;
        boolean repeat;
        do {
            repeat = false;
            double distanceToTarget = mColorMetric.getDistance(currentColorApprox, targetColor, useAlpha);
            SortedMap<Double, Integer> distancesToColorsInRange = mDataDistances.get(currentColorApprox).subMap(distanceToTarget - tolerance, distanceToTarget + tolerance);
            for (Integer colorsInRange : distancesToColorsInRange.values()) {
                double colorInRangeDistanceToTarget = mColorMetric.getDistance(colorsInRange, targetColor, useAlpha);
                if (colorInRangeDistanceToTarget < distanceToTarget) {
                    currentColorApprox = colorsInRange;
                    distanceToTarget = colorInRangeDistanceToTarget;
                    repeat = true;
                }
            }
        } while (repeat);
        mCurrentColorApprox = currentColorApprox;
        return mData.get(currentColorApprox);
    }

    @Override
    public double getAccuracy() {
        return mAccuracy;
    }

    @Override
    public boolean removeTile(MosaicTile<S> toRemove) {
        Log.d("HomeStuff", "Removing tile : " + toRemove);
        return mData.remove(toRemove.getAverageARGB()) != null && mDataDistances.remove(toRemove.getAverageARGB()) != null;
    }

    @Override
    public int getUsedTilesCount() {
        return mData.size();
    }
}
