package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * Created by daniel on 18.08.15.
 */
public class RiddleFlow extends RiddleGame {
    private static final long UPDATE_PERIOD = 300; //ms
    private int[][] mSolutionRaster;
    private double[][] mPressureRaster;
    private ColorMetric mMetric;
    private boolean mUseAlpha;
    private Bitmap mPresentedBitmap;
    private Canvas mPresentedCanvas;
    private Paint mClearPaint;
    private int mWidth;
    private int mHeight;
    private int[] mOutputRaster;
    private List<Flow> mFlows;

    private enum FlowDirection {
        TOP_LEFT(-1, -1), TOP(0, -1), TOP_RIGHT(1, -1),
        RIGHT(1, 0), BOTTOM_RIGHT(1, 1), BOTTOM(0, 1),
        BOTTOM_LEFT(-1, 1), LEFT(-1, 0);
        private static final FlowDirection[] DIRECTIONS = FlowDirection.values();
        private static final int DIRECTIONS_COUNT = DIRECTIONS.length;

        FlowDirection(int xDelta, int yDelta) {
            mXDelta = xDelta;
            mYDelta = yDelta;
            mAngle = Math.atan2(mYDelta, mXDelta);
        }

        boolean hasDirection(int x, int y, int width, int height) {
            return (y + mYDelta) >= 0 && (y + mYDelta) < height
                    && (x + mXDelta) >= 0 && (x + mXDelta) < width;
        }

        int getDirectionValue(int[][] raster, int x, int y) {
            return raster[y + mYDelta][x + mXDelta];
        }

        int adaptToFlow(int toAdaptTo) {
            //return toAdaptTo;
            final int ordinal = ordinal();
            final int diff = toAdaptTo - ordinal;
            if (diff == 0) {
                return ordinal;
            }
            if (diff > DIRECTIONS_COUNT / 2) {
                // move 1 CCW since this is shorter than CW
                return (ordinal() + DIRECTIONS_COUNT - 1) % DIRECTIONS_COUNT;
            }
            if (diff > 0) {
                // move 1 CW
                return ordinal() + 1;
            }
            if (-diff > DIRECTIONS_COUNT / 2) {
                // move 1 CW since this is shorter than CCW
                return (ordinal() + 1) % DIRECTIONS_COUNT;
            }
            // move 1 CCW since this is shorter than CW
            return ordinal() - 1;
        }

        final int mXDelta;
        final int mYDelta;
        final double mAngle;

        public FlowDirection oppositeDirection() {
            switch (this) {
                case TOP_LEFT:
                    return BOTTOM_RIGHT;
                case TOP:
                    return BOTTOM;
                case TOP_RIGHT:
                    return BOTTOM_LEFT;
                case RIGHT:
                    return LEFT;
                case BOTTOM_RIGHT:
                    return TOP_LEFT;
                case BOTTOM:
                    return TOP;
                case BOTTOM_LEFT:
                    return TOP_RIGHT;
                case LEFT:
                    return RIGHT;
                default:
                    return null;
            }
        }

        public static FlowDirection bestFromAngle(final double angle) {
            double bestAngleDiff = Double.MAX_VALUE;
            FlowDirection bestMatch = null;
            for (int d = 0; d < DIRECTIONS_COUNT; d++) {
                FlowDirection curr = DIRECTIONS[d];
                double currDiff = Math.abs(curr.mAngle - angle);
                if (currDiff < bestAngleDiff) {
                    bestAngleDiff = currDiff;
                    bestMatch = curr;
                } else {
                    currDiff = Math.abs(curr.mAngle - angle + Math.PI * 2.);
                    if (currDiff < bestAngleDiff) {
                        bestAngleDiff = currDiff;
                        bestMatch = curr;
                    } else {
                        currDiff = Math.abs(angle - curr.mAngle + Math.PI * 2.);
                        if (currDiff < bestAngleDiff) {
                            bestAngleDiff = currDiff;
                            bestMatch = curr;
                        }
                    }
                }
            }
            return bestMatch;
        }
    }

    public RiddleFlow(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    protected int calculateGainedScore() {
        return RiddleGame.DEFAULT_SCORE;
    }

    @Override
    public void draw(Canvas canvas) {
        long start = System.currentTimeMillis();
        mPresentedCanvas.drawPaint(mClearPaint);
        boolean drawData = true;
        if (drawData) {
            mPresentedBitmap.setPixels(mOutputRaster, 0, mWidth, 0, 0, mWidth, mHeight);
        } else {
            for (int y = 0; y < mPresentedBitmap.getHeight(); y++) {
                for (int x = 0; x < mPresentedBitmap.getWidth(); x++) {
                    mPresentedBitmap.setPixel(x, y, ColorAnalysisUtil.interpolateColorLinear(Color.BLUE, Color.RED, (float) mPressureRaster[y][x]));//visualiez pressure
                }
            }
        }
        canvas.drawBitmap(mPresentedBitmap, 0, 0, null);
        Log.d("Riddle", "Time taken for drawing: " + (System.currentTimeMillis() - start));
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mFlows = new ArrayList<>();
        mMetric = ColorMetric.Absolute.INSTANCE;
        mWidth = mBitmap.getWidth();
        mHeight = mBitmap.getHeight();
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mOutputRaster = new int[mHeight * mWidth];
        mSolutionRaster = new int[mHeight][mWidth];
        for (int x = 0; x < mWidth; x++) {
            for (int y = 0; y < mHeight; y++) {
                mSolutionRaster[y][x] = mBitmap.getPixel(x, y);
            }
        }

        //init pressure
        mPressureRaster = new double[mHeight][mWidth];
        for (int x = 0; x < mWidth; x++) {
            for (int y = 0; y < mHeight; y++) {
                double pressure = 0.;
                for (int d = 0; d < FlowDirection.DIRECTIONS_COUNT; d++) {
                    FlowDirection dir = FlowDirection.DIRECTIONS[d];
                    if (dir.hasDirection(x, y, mWidth, mHeight)) {
                        pressure += mMetric.getDistance(mSolutionRaster[y][x], mSolutionRaster[y + dir.mYDelta][x + dir.mXDelta], mUseAlpha);
                    } else {
                        pressure += mMetric.maxValue(mUseAlpha);
                    }
                }
                pressure /= mMetric.maxValue(mUseAlpha) * FlowDirection.DIRECTIONS_COUNT;//normalize, pressure in [0,1]
                mPressureRaster[y][x] = Math.pow(pressure, 0.15);
            }
        }

        mPresentedBitmap = Bitmap.createBitmap(mWidth, mHeight, mBitmap.getConfig());
        mPresentedCanvas = new Canvas(mPresentedBitmap);
    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updatePeriod) {
        Log.d("Riddle", "Starting periodic event with upatePeriod: " + updatePeriod);
        long start = System.currentTimeMillis();
        executeFlow();
        Log.d("Riddle", "Time taken for execute flow: " + (System.currentTimeMillis() - start));

        final long sleep = UPDATE_PERIOD - updatePeriod;
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
        }
    }

    private void executeFlow() {
        for (int i = 0; i < mFlows.size(); i++) {
            Flow curr = mFlows.get(i);
            curr.apply();
            curr.spread();
        }
        for (int i = 0; i < mFlows.size(); i++) {
            if (mFlows.get(i).mIntensity <= 0.) {
                mFlows.remove(i);
                i--;
            }
        }
    }

    private class Flow {
        private final int mColor;
        private boolean[][] mTakenPositionsRaster;
        private boolean[][] mTakenPositionsRasterWork;
        private double mIntensity;
        private final double mStartPressure;

        public Flow(int color, double pressure, int startX, int startY) {
            mColor = color;
            mIntensity = 1.;
            mStartPressure = pressure;
            mTakenPositionsRaster = new boolean[mHeight][mWidth];
            mTakenPositionsRasterWork = new boolean[mHeight][mWidth];
            mTakenPositionsRaster[startY][startX] = true;
        }

        public void apply() {
            for (int y = 0; y < mHeight; y++) {
                for (int x = 0; x < mWidth; x++) {
                    if (mTakenPositionsRaster[y][x]) {
                        int pos = x + y * mWidth;
                        int currColor = mOutputRaster[pos];
                        if (currColor != 0) {
                            mOutputRaster[pos] = ColorAnalysisUtil.mix(currColor, mColor, 100, (int) (100 * mIntensity));
                        } else {
                            mOutputRaster[pos] = mColor;
                        }
                    }
                }
            }
        }

        public void spread() {
            mIntensity -= 0.003;
            boolean spread = false;
            for (int y = 0; y < mHeight; y++) {
                for (int x = 0; x < mWidth; x++) {
                    mTakenPositionsRasterWork[y][x] = mTakenPositionsRaster[y][x];
                    if (mTakenPositionsRasterWork[y][x]) {
                        for (int d = 0; d < FlowDirection.DIRECTIONS_COUNT; d++) {
                            FlowDirection curr = FlowDirection.DIRECTIONS[d];
                            if (curr.hasDirection(x, y, mWidth, mHeight) && Math.abs(mPressureRaster[y + curr.mYDelta][x + curr.mXDelta] - mStartPressure) < 0.05) {
                                mTakenPositionsRasterWork[y + curr.mYDelta][x + curr.mXDelta] = true;
                                spread = true;
                            }
                        }
                    }
                }
            }
            if (!spread) {
                mIntensity = 0.;
            }
            //swap
            boolean[][] temp = mTakenPositionsRaster;
            mTakenPositionsRaster = mTakenPositionsRasterWork;
            mTakenPositionsRasterWork = temp;
        }
    }


    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x >= 0 && y >= 0 && x < mWidth && y < mHeight) {
                long start = System.currentTimeMillis();
                int col = mSolutionRaster[y][x];
                mFlows.add(new Flow(Color.argb(255, Color.red(col), Color.green(col), Color.blue(col)), mPressureRaster[y][x], x, y));
                Log.d("Riddle", "Time taken for motion event down: " + (System.currentTimeMillis() - start));
            }
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return null;
    }
}
