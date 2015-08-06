package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;

import java.util.Arrays;
import java.util.Map;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageBitmapSource;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.system.RiddleFragment;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectToast;
import dan.dit.whatsthat.util.MultistepPercentProgressListener;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.jama.Matrix;
import dan.dit.whatsthat.util.jama.SingularValueDecomposition;
import dan.dit.whatsthat.util.mosaic.data.MosaicMaker;
import dan.dit.whatsthat.util.mosaic.matching.SimpleLinearTileMatcher;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;

/**
 * Riddle for testing images and other bitmap features. NOT MEANT FOR USERS since there
 * is no riddle in just showing the image straight with no obfuscation and nothing to do!
 * Created by daniel on 17.04.15.
 */
public class RiddleDeveloper extends RiddleGame {

    private static final int MOSAIC_MODES = 3;
    private Bitmap[] mGallery;
    private int mGalleryIndex;
    private String[] mGalleryToast;

    private Bitmap[] mSVDGallery;
    private String[] mSVDGalleryToast;

    private Bitmap[][] mMosaicGalleries;

    private boolean mIsMosaicMode;
    private int mCurrMosaicMode;

    public RiddleDeveloper(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    public void draw(Canvas canvas) {
        if (mGallery != null && mGalleryIndex >= 0 && mGalleryIndex < mGallery.length && mGallery[mGalleryIndex] != null) {
            canvas.drawBitmap(mGallery[mGalleryIndex], 0, 0, null);
        } else {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        MultistepPercentProgressListener multiListener = new MultistepPercentProgressListener(listener, MOSAIC_MODES);
        for (mCurrMosaicMode = 0; mCurrMosaicMode < MOSAIC_MODES; mCurrMosaicMode++) {
            makeMosaics(res, multiListener);
            multiListener.nextStep();
        }
        mCurrMosaicMode = 0;
        switchToMosaicApproximation();
    }



    private void switchToRankApproximation(PercentProgressListener listener) {
        if (mSVDGallery == null) {
            if (listener == null) {
                listener = new PercentProgressListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        Log.d("Riddle", "Progress of rank approx when switching: " + progress);
                    }
                };
            }
            mMosaicGalleries = null;
            makeRankApproximation(listener);
        }
        mIsMosaicMode = false;
        mGallery = mSVDGallery;
        mGalleryToast = mSVDGalleryToast;
        mGalleryIndex = 0;
    }

    private void switchToMosaicApproximation() {
        if (mMosaicGalleries == null) {
            return;
        }
        mIsMosaicMode = true;
        mGallery = mMosaicGalleries[mCurrMosaicMode];
        mGalleryToast = null;
        mGalleryIndex = 0;
    }

    private void makeRankApproximation(PercentProgressListener progress) {
        MultistepPercentProgressListener multiProgress = new MultistepPercentProgressListener(progress, new double[] {0.05, 0.7, 0.25});

        long totalTic = System.currentTimeMillis();

        //IndexedBitmap indexedBitmap = new IndexedBitmap(mBitmap);
        //Matrix sourceMatrix = indexedBitmap.getMatrix();
        Matrix sourceMatrix = BitmapUtil.toMatrix(mBitmap);

        boolean transposeRequired = sourceMatrix.getColumnDimension() > sourceMatrix.getRowDimension(); // so SingularValueDecomposition works...
        if (transposeRequired) {
            sourceMatrix = sourceMatrix.transpose();
        }
        multiProgress.nextStep();

        SingularValueDecomposition decomposition = new SingularValueDecomposition(sourceMatrix);
        multiProgress.nextStep();

        int rank = decomposition.rank();
        int[] rankApproximations = new int[] {1, 2, 3, 4, 5, 10, 50, 100, 150, rank -5, rank - 2, rank -1, rank};
        double[] singularValues = decomposition.getSingularValues();
        Log.d("Riddle", "Calculates singular values: " +  Arrays.toString(singularValues));
        Matrix U = decomposition.getU();
        Matrix VT = decomposition.getV().transpose();

        int index = 1;
        mSVDGallery = new Bitmap[index + rankApproximations.length];
        mSVDGalleryToast = new String[mSVDGallery.length];
        mSVDGallery[0] = mBitmap;
        MultistepPercentProgressListener subProgress = new MultistepPercentProgressListener(multiProgress, rankApproximations.length);

        for (int rankApproximation : rankApproximations) {
            long tic = System.currentTimeMillis();
            Matrix resultMatrix = U.diagTimes(singularValues, rankApproximation, VT);
            if (transposeRequired) {
                resultMatrix = resultMatrix.transpose();
            }

            //IndexedBitmap resultIndexedBitmap = new IndexedBitmap(resultMatrix, indexedBitmap.getIndexedColors());
            //mGallery[index] = resultIndexedBitmap.convertToBitmap();
            mSVDGallery[index] = BitmapUtil.toBitmap(resultMatrix);

            mSVDGalleryToast[index] = "Rank " + rankApproximation + " approx.";
            Log.d("Riddle", "Made rank " + rankApproximation + " approximation in " + (System.currentTimeMillis() - tic) + "ms.");
            subProgress.nextStep();
            index++;
        }
        multiProgress.nextStep();
        Log.d("Riddle", "Made rank approximation " + Arrays.toString(rankApproximations) + " of total rank " + rank + " in " + (System.currentTimeMillis() - totalTic) + "ms.");

    }

    private void makeMosaics(Resources res, PercentProgressListener listener) {
        long tic;
        long totalTic = System.currentTimeMillis();
        Log.d("Riddle", "Making mosaic.");

        Map<String, Image> images = RiddleFragment.ALL_IMAGES;
        ImageBitmapSource source = new ImageBitmapSource(res, images);
        ColorMetric metric = ColorMetric.Euclid2.INSTANCE;
        boolean useAlpha = true;

        tic = System.currentTimeMillis();
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(images.values(), useAlpha, metric);
        Log.d("Riddle", "Matcher initialized with " + images.size() + " images in " + (System.currentTimeMillis() - tic) + "ms.");
        matcher.setHashMatches(true);
        MosaicMaker<String> maker = new MosaicMaker<>(matcher, source, useAlpha, metric);
        double[] rectColumnRows = new double[] {1, 2, 5, 10, 20, 30};
        double[] fixedLayerParams = new double[] {3, 5, 7};
        double[] mergeFactorParams = new double[] {0.1, 0.25, 0.5, 0.75, 1.};

        double[] params;
        if (mCurrMosaicMode == 0) {
            params = rectColumnRows;
        } else if (mCurrMosaicMode == 1) {
            params = mergeFactorParams;
        } else {
            params = fixedLayerParams;
        }
        if (mMosaicGalleries == null) {
            mMosaicGalleries = new Bitmap[MOSAIC_MODES][];
        }
        mMosaicGalleries[mCurrMosaicMode] = new Bitmap[params.length + 1];
        mMosaicGalleries[mCurrMosaicMode][0] = mBitmap;
        int index = 1;
        MultistepPercentProgressListener multistepProgress = new MultistepPercentProgressListener(listener, params.length);
        for (double param : params) {
            tic = System.currentTimeMillis();
            if (mCurrMosaicMode == 0) {
                mMosaicGalleries[mCurrMosaicMode][index] = maker.makeRect(mBitmap, (int) param, (int) param, multistepProgress);
            } else if (mCurrMosaicMode == 1) {
                mMosaicGalleries[mCurrMosaicMode][index] = maker.makeMultiRect(mBitmap, 30, 30, param, multistepProgress);
            } else if ((int) param > 0) {
                mMosaicGalleries[mCurrMosaicMode][index] = maker.makeFixedLayer(mBitmap, (int) param, multistepProgress);
            }
            Log.d("Riddle", "Next step with param" + param + " of " + Arrays.toString(params) + " index " + index + " mode " +mCurrMosaicMode);
            multistepProgress.nextStep();
            Log.d("Riddle", "Resulting used " + matcher.getUsedTilesCount() + " with " + images.size() + " in the pool (time taken " + (System.currentTimeMillis() - tic) + "ms.)");
            //ImageUtil.saveToFile(mMosaic, "mosaic_" + String.valueOf(factor) + ".png");
            index++;
        }
        listener.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
        Log.d("Riddle", "Mosaic generation finished in " + (System.currentTimeMillis() - totalTic) + "ms.");
    }

    private long mLastMoveStartTime;
    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (System.currentTimeMillis() - mLastMoveStartTime > 2000L) {
                mLastMoveStartTime = System.currentTimeMillis();
            } else {
                return false;
            }
            if (mIsMosaicMode && mCurrMosaicMode < MOSAIC_MODES - 1) {
                mCurrMosaicMode++;
                switchToMosaicApproximation();
            } else if (mIsMosaicMode) {
                switchToRankApproximation(null);
            } else {
                mCurrMosaicMode = 0;
                switchToMosaicApproximation();
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && mGallery != null) {
            mGalleryIndex++;
            mGalleryIndex %= mGallery.length;
            if (mGalleryToast != null && mGalleryToast[mGalleryIndex] != null) {
                TestSubjectToast toast = new TestSubjectToast(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                        0, 0, 0, 0, 600);
                toast.mText = mGalleryToast[mGalleryIndex];
                TestSubject.getInstance().postToast(toast, 0);
            }
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return "";
    }

    @Override
    protected int calculateGainedScore() {
        return 0;
    }

}
