package dan.dit.whatsthat.util.mosaic.reconstruction;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;
import java.util.Random;

import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * 'Better' version of AutoLayerReconstructor using the k-means algorithm
 * with the given ColorMetric. In contrast to the other layer reconstructor
 * the amount of layers needs to be fixed and given though. Results are cleaner for the same run speed
 * and similar parameter settings, though the parameter needs to be determined by the user('s preference)
 * and cannot be set to an arbitrary constant.
 * Created by daniel on 03.07.15.
 */
public class FixedLayerReconstructor extends Reconstructor {
    private static final int MAX_RECALCULATIONS_BASE = 5;
    private static final int MAX_RECALCULATIONS_LINEAR_GROWTH = 1;
    private final boolean mUseAlpha;
    private final ColorMetric mColorMetric;
    private MosaicFragment mFragment;
    private Bitmap mResult;
    private int[] mPixelClusterNumber;
    private Bitmap[] mClusterBitmaps;
    private int[] mClusterColors;
    private int mCurrCluster;

    public FixedLayerReconstructor(Bitmap source, int clusterCount, boolean useAlpha, ColorMetric metric, PercentProgressListener progress) {
        mUseAlpha = useAlpha;
        mColorMetric = metric;
        init(source, clusterCount, progress);
    }

    private void init(Bitmap source, final int clusterCount, PercentProgressListener progress) {
        if (clusterCount < 1) {
            throw new IllegalArgumentException("No clusters?!" + clusterCount);
        }
        final int width = source.getWidth();
        final int height = source.getHeight();
        mFragment = new MosaicFragment(0, 0, 0);
        mResult = obtainBaseBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        mClusterBitmaps = new Bitmap[clusterCount];
        int[] pixelColors = new int[width * height];
        mPixelClusterNumber = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixelColors[x + y * width] = source.getPixel(x, y);
            }
        }

        // k-means algorithm
        Random rand = new Random();

        int[] clusterCenters = new int[clusterCount];
        double[] clusterWeights = new double[clusterCount];

        /* //init (with random centers, also other possibilities as convergence greatly depends on starting values)
        for (int i = 0; i < clusterCenters.length; i++) {
            clusterCenters[i] = pixelColors[rand.nextInt(pixelColors.length)];
        }*/

        // init k-means++ style by preferring centers that are further away from the last center
        clusterCenters[0] = pixelColors[rand.nextInt(pixelColors.length)];
        final double maxDist = mColorMetric.maxValue(mUseAlpha);
        for (int cluster = 1; cluster < clusterCount; cluster++) {
            clusterCenters[cluster] = pixelColors[rand.nextInt(pixelColors.length)];
            for (int i = 0; i < pixelColors.length; i++) {
                double distFraction = mColorMetric.getDistance(pixelColors[i], clusterCenters[cluster - 1], mUseAlpha) / maxDist;
                distFraction *= distFraction;
                distFraction *= distFraction;
                distFraction *= i / (double) pixelColors.length;
                if (rand.nextDouble() < distFraction) {
                    clusterCenters[cluster] = pixelColors[i];
                    break;
                }
            }
        }

        int[] clusterCenterRed = new int[clusterCount];
        int[] clusterCenterGreen = new int[clusterCount];
        int[] clusterCenterBlue = new int[clusterCount];
        int[] clusterCenterAlpha = mUseAlpha ? new int[clusterCount] : null;
        int[] clusterSize = new int[clusterCount];
        int redistributionCount = 0;
        int maxRedistributions = MAX_RECALCULATIONS_BASE + MAX_RECALCULATIONS_LINEAR_GROWTH * clusterCount;
        int changed = Integer.MAX_VALUE;
        int lastChanged;
        do {
            // for improved speed we expect monotonous convergence in the amount of pixels changed, if this ever increases again we stop
            lastChanged = changed;
            changed = 0;
            // redistribute into clusters
            Arrays.fill(clusterWeights, 0.);
            for (int i = 0; i < pixelColors.length; i++) {
                int currColor = pixelColors[i];
                double minWeightIncrease = Double.MAX_VALUE;
                int minWeightIncreaseIndex = 0;
                for (int cluster = 0; cluster < clusterCount; cluster++) {
                    double currWeightIncrease = mColorMetric.getDistance(clusterCenters[cluster], currColor, mUseAlpha);
                    if (currWeightIncrease < minWeightIncrease) {
                        minWeightIncrease = currWeightIncrease;
                        minWeightIncreaseIndex = cluster;
                    }
                }
                clusterWeights[minWeightIncreaseIndex] += minWeightIncrease;
                int oldNumber = mPixelClusterNumber[i];
                mPixelClusterNumber[i] = minWeightIncreaseIndex;
                if (oldNumber != minWeightIncreaseIndex) {
                    changed++;
                }
            }
            redistributionCount++;

            // recalculate centers
            Arrays.fill(clusterCenterRed, 0);
            Arrays.fill(clusterCenterGreen, 0);
            Arrays.fill(clusterCenterBlue, 0);
            if (mUseAlpha) {
                Arrays.fill(clusterCenterAlpha, 0);
            }
            Arrays.fill(clusterSize, 0);
            for (int i = 0; i < pixelColors.length; i++) {
                int cluster = mPixelClusterNumber[i];
                int color = pixelColors[i];
                clusterCenterRed[cluster] += Color.red(color);
                clusterCenterGreen[cluster] += Color.green(color);
                clusterCenterBlue[cluster] += Color.blue(color);
                if (mUseAlpha) {
                    clusterCenterAlpha[cluster] += Color.alpha(color);
                }
                clusterSize[cluster]++;
            }
            for (int cluster = 0; cluster < clusterCount; cluster++) {
                int size = clusterSize[cluster];
                if (size > 0) {
                    int red = clusterCenterRed[cluster] / size;
                    int green = clusterCenterGreen[cluster] / size;
                    int blue = clusterCenterBlue[cluster] / size;
                    int alpha = mUseAlpha ? clusterCenterAlpha[cluster] / size : 255;
                    clusterCenters[cluster] = Color.argb(alpha, red, green, blue);
                }
            }
            progress.onProgressUpdate((int) (PercentProgressListener.PROGRESS_COMPLETE * redistributionCount / (double) maxRedistributions));
            Log.d("HomeStuff", "ClusterLayerReconstructor changed pixels: " + changed + " in run " + redistributionCount);
        } while (changed <= lastChanged && redistributionCount < maxRedistributions);
        Log.d("HomeStuff", "Finished ClusteredLayerReconstructor with " + redistributionCount + " recalculations for " + clusterCount + " clusters.");
        mClusterColors = clusterCenters;
        progress.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
    }

    @Override
    public boolean giveNext(Bitmap nextFragmentImage) {
        if (nextFragmentImage == null || hasAll() || nextFragmentImage.getWidth() != mResult.getWidth() || nextFragmentImage.getHeight() != mResult.getHeight()) {
            return false;
        }
        mClusterBitmaps[mCurrCluster] = nextFragmentImage;
        mCurrCluster++;
        return true;
    }

    @Override
    public MosaicFragment nextFragment() {
        if (hasAll()) {
            return null;
        }
        mFragment.reset(mResult.getWidth(), mResult.getHeight(), mClusterColors[mCurrCluster]);
        return mFragment;
    }

    @Override
    public boolean hasAll() {
        return mCurrCluster >= mClusterColors.length;
    }

    @Override
    public Bitmap getReconstructed() {
        int width = mResult.getWidth();
        for (int i = 0; i < mPixelClusterNumber.length; i++) {
            int x = i % width;
            int y = i / width;
            int cluster = mPixelClusterNumber[i];
            mResult.setPixel(x, y, mClusterBitmaps[cluster].getPixel(x, y));
        }
        return mResult;
    }

    @Override
    public int estimatedProgressPercent() {
        return (int) (100 * mCurrCluster / (double) mClusterColors.length);
    }
}
