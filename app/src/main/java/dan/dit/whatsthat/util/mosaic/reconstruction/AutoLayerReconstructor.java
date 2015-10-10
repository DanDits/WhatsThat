package dan.dit.whatsthat.util.mosaic.reconstruction;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * Created by daniel on 01.07.15.
 */
public class AutoLayerReconstructor extends Reconstructor {
    private Bitmap mResult;
    private MosaicFragment mFragment;
    private MosaicFragment mNext;
    private int mLayersApplied;
    private Iterator<Integer> mColorIterator;
    private final boolean mUseAlpha;
    private List<List<Integer>> mUsedColorsStartPosition;
    private List<Integer> mUsedColors;
    private int[] mPositionDeltas;
    private ColorMetric mColorMetric;

    public AutoLayerReconstructor(Bitmap source, double factor, boolean useAlpha, ColorMetric metric, PercentProgressListener progress) {
        mUseAlpha = useAlpha;
        mColorMetric = metric;
        init(source, factor, progress);
    }

    private void init(Bitmap source, double factor, PercentProgressListener progress) {

        long tic = System.currentTimeMillis();
        final int width = source.getWidth();
        final int height = source.getHeight();
        mFragment = new MosaicFragment(0, 0, 0);
        mResult = obtainBaseBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        final int[] colors = new int[width * height];
        final int[] deltas = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                colors[x + y * width] = source.getPixel(x, y);
            }
        }
        final double maxSim = mColorMetric.maxValue(mUseAlpha);
        final double sim = ColorAnalysisUtil.factorToSimilarityBound(factor);
        final int simBound = (int) (sim * maxSim);
        final int alreadyReachedMarker = Integer.MIN_VALUE;
        List<Integer> usedColors = new ArrayList<>();
        List<Integer> usedColorsStartPosition = new ArrayList<>();

        for (int analyzedToIndex = 0; analyzedToIndex < colors.length; analyzedToIndex++) {
            // first try to evolve the current color further, under the assumption that similar colors are close to each other this is almost constant in operations
            int currColor = colors[analyzedToIndex];
            if (deltas[analyzedToIndex] == 0) {
                // color not yet reached, use it as start color
                usedColors.add(currColor);
                usedColorsStartPosition.add(analyzedToIndex);
            }
            deltas[analyzedToIndex] = 0; // marker no  longer required, reset delta

            for (int currIndex = analyzedToIndex + 1; currIndex < colors.length; currIndex++) {
                if (mColorMetric.getDistance(colors[currIndex], currColor, mUseAlpha) <= simBound) {
                    colors[currIndex] = currColor;
                    deltas[analyzedToIndex] = currIndex - analyzedToIndex;
                    deltas[currIndex] = alreadyReachedMarker; // so it is not added as a new color when main loop reaches this index
                    break;
                }
            }
            progress.onProgressUpdate((int) (100 * analyzedToIndex / (double) colors.length));
        }

        Log.d("HomeStuff", "DominantLayerReconstructor finished main work (hopefully) in " + (System.currentTimeMillis() - tic) + " has colors " + usedColors.size());

        // now create actual color circles around the filtered used colors, only required if too many colors matched above that are similar but didnt know it
        mUsedColorsStartPosition = new LinkedList<>();
        mUsedColors = new LinkedList<>();
        for (int i = 0; i < usedColors.size(); i++) {
            int currColorI = usedColors.get(i);
            mUsedColors.add(currColorI);
            List<Integer> startPositions = new LinkedList<>();
            mUsedColorsStartPosition.add(startPositions);
            startPositions.add(usedColorsStartPosition.get(i));
            for (int j = i + 1; j < usedColors.size(); j++) {
                Integer potentialColor = usedColors.get(j);
                if (mColorMetric.getDistance(currColorI, potentialColor, mUseAlpha) <= simBound) { // if the pixel != 0 check is not done you need to check here if delta is zero else multiple paths might leed together and result in way too many pixels being drawn
                    startPositions.add(usedColorsStartPosition.get(j));
                    usedColors.remove(j);
                    usedColorsStartPosition.remove(j);
                    j--;
                }
            }
        }


        mPositionDeltas = deltas;
        Log.d("HomeStuff", "Finished DominantLayerReconstructor for " + width + "x" + height + " in " + (System.currentTimeMillis() - tic) + "ms, used colors: " + mUsedColors.size());
    }

    @Override
    public boolean giveNext(Bitmap nextFragmentImage) {
        if (nextFragmentImage == null || mNext == null
                || nextFragmentImage.getWidth() != mResult.getWidth() || nextFragmentImage.getHeight() != mResult.getHeight()) {
            return false;
        }
        int width = mResult.getWidth();
        List<Integer> startPositions = mUsedColorsStartPosition.get(mLayersApplied);

        for (int position : startPositions) {
            int delta;
            do {
                int x = position % width;
                int y = position / width;
                if (mResult.getPixel(x, y) != 0) {
                    break;
                }
                mResult.setPixel(x, y, nextFragmentImage.getPixel(x, y));
                delta = mPositionDeltas[position];
                position += delta;
            } while (delta > 0);
        }
        mNext = null;
        mLayersApplied++;
        return true;
    }

    @Override
    public MosaicFragment nextFragment() {
        if (mColorIterator == null) {
            mColorIterator = mUsedColors.iterator();
        }
        if (!mColorIterator.hasNext()) {
            return null;
        }
        if (mNext == null) {
            int currColor = mColorIterator.next();
            mFragment.reset(mResult.getWidth(), mResult.getHeight(), currColor);
            mNext = mFragment;
        }
        return mNext;
    }

    @Override
    public boolean hasAll() {
        return mColorIterator != null && !mColorIterator.hasNext();
    }

    @Override
    public Bitmap getReconstructed() {
        return mResult;
    }

    @Override
    public int estimatedProgressPercent() {
        int colors = mUsedColors.size();
        if (colors <= 0) {
            return 0;
        }
        return (int) (100 * mLayersApplied / (double) colors);
    }
}
