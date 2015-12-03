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

package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementFlow;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.control.RiddleScore;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * Created by daniel on 18.08.15.
 */
public class RiddleFlow extends RiddleGame {
    private static final long UPDATE_PERIOD = 50L; //ms
    private static final long FLOW_MAX_DURATION = 20000L; //ms
    private static final boolean APPLY_TRUE_SOLUTION_COLOR_PER_PIXEL = true; //true would make it too easy and show the true pixel color forever as soon as a flow reaches the pixel
    private static final boolean SEARCH_RANDOMLY_FOR_EQUAL_PRESSURES = true; //better: true, this avoids the preference of picking the first FlowDirection in the list (TopLeft) if there is no clear precedence which would result in many diagonal lines, instead random yields zig-zig flows
    private static final boolean USE_ALPHA = true; // if the color metric has to use alpha values of colors to calculate pressure raster. If false black and alpha only images would be completely uniform in pressure and unsolvable
    private static final ColorMetric METRIC = ColorMetric.Absolute.INSTANCE; // color metric used, can be anything, but should include all colors and alpha uniformly
    private static final int MAX_FLOWS_FOR_SCORE_BONUS = 10;

    // searching edges results in lines searching for pixels with high pressure values and makes them search for edges (lines of great color difference) to follow. Else it follows colors of equal pressure so lines need to be clicked to follow them.
    private static final int SEARCH_EDGES_EACH_X_FLOWS = 3;

    private int[][] mSolutionRaster;
    private double[][] mPressureRaster;
    private Bitmap mPresentedBitmap;
    private Canvas mPresentedCanvas;
    private Paint mClearPaint;
    private int mWidth;
    private int mHeight;
    private double[][] mFlowIntensityRaster;
    private int[] mOutputRaster;
    private List<Flow> mFlows;
    private List<Integer> mFlowStartsX;
    private List<Integer> mFlowStartsY;
    private Random mRand;
    private float mOffsetX;
    private float mOffsetY;
    private int mRevealedPixelsCount;

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

        final int mXDelta;
        final int mYDelta;
        final double mAngle;

        boolean hasDirection(int x, int y, int width, int height) {
            return (y + mYDelta) >= 0 && (y + mYDelta) < height
                    && (x + mXDelta) >= 0 && (x + mXDelta) < width;
        }
    }

    public RiddleFlow(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {
        mConfig.mAchievementGameData.putValue(AchievementFlow.KEY_GAME_TOTAL_PIXELS_COUNT, (long) mOutputRaster.length, AchievementProperties.UPDATE_POLICY_ALWAYS);
    }
    @Override
    public Bitmap makeSnapshot() {
        return BitmapUtil.resize(mPresentedBitmap, SNAPSHOT_DIMENSION.getWidthForDensity(mConfig.mScreenDensity), SNAPSHOT_DIMENSION.getHeightForDensity(mConfig.mScreenDensity));
    }

    @Override
    protected @NonNull RiddleScore calculateGainedScore() {
        int bonus = (mFlowStartsX.size() < MAX_FLOWS_FOR_SCORE_BONUS ? Types.SCORE_MEDIUM : 0);
        return super.calculateGainedScore().addBonus(bonus);
    }

    @Override
    public void draw(Canvas canvas) {
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
        canvas.drawBitmap(mPresentedBitmap, mOffsetX, mOffsetY, null);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mRand = new Random();
        mFlowStartsX = new ArrayList<>(128);
        mFlowStartsY = new ArrayList<>(128);
        mFlows = new ArrayList<>();
        mWidth = mBitmap.getWidth();
        mHeight = mBitmap.getHeight();
        mOffsetX = (mConfig.mWidth - mWidth) / 2.f;
        mOffsetY = (mConfig.mHeight - mHeight) / 2.f;

        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mOutputRaster = new int[mHeight * mWidth];
        mSolutionRaster = new int[mHeight][mWidth];
        for (int x = 0; x < mWidth; x++) {
            for (int y = 0; y < mHeight; y++) {
                mSolutionRaster[y][x] = mBitmap.getPixel(x, y);
            }
        }
        final int firstProgress = 10;
        listener.onProgressUpdate(firstProgress);

        //init pressure
        mPressureRaster = new double[mHeight][mWidth];
        final double metricMax = METRIC.maxValue(USE_ALPHA);
        for (int y = 0; y < mHeight; y++) {
            for (int x = 0; x < mWidth; x++) {
                double pressure = 0.;
                for (int d = 0; d < FlowDirection.DIRECTIONS_COUNT; d++) {
                    FlowDirection dir = FlowDirection.DIRECTIONS[d];
                    if (dir.hasDirection(x, y, mWidth, mHeight)) {
                        pressure += METRIC.getDistance(mSolutionRaster[y][x], mSolutionRaster[y + dir.mYDelta][x + dir.mXDelta], USE_ALPHA);
                    } else {
                        pressure += metricMax;
                    }
                }
                pressure /= metricMax * FlowDirection.DIRECTIONS_COUNT;//normalize, pressure in [0,1]
                mPressureRaster[y][x] = Math.pow(pressure, 0.15);
            }
            listener.onProgressUpdate(firstProgress + (int) ((PercentProgressListener.PROGRESS_COMPLETE - firstProgress) * y / (double) mHeight));
        }

        mPresentedBitmap = Bitmap.createBitmap(mWidth, mHeight, mBitmap.getConfig());
        mPresentedCanvas = new Canvas(mPresentedBitmap);

        mFlowIntensityRaster = new double[mHeight][mWidth];
        initPreviousFlows(getCurrentState());
    }

    private void initPreviousFlows(Compacter currentState) {
        if (currentState == null || currentState.getSize() < 3) {
            return;
        }
        //first slot empty
        try {
            for (int i = 1; i + 1 < currentState.getSize(); i += 2) {
                addFlow(currentState.getInt(i), currentState.getInt(i + 1), true);
            }
        } catch (CompactedDataCorruptException e) {
            Log.e("Riddle", "Error adding previous flows when loading RiddleFlow: " + e);
        }
    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updatePeriod) {
        executeFlow(updatePeriod);

        final long sleep = UPDATE_PERIOD - updatePeriod;
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
        }
    }

    private void executeFlow(long updatePeriod) {
        for (int i = 0; i < mFlows.size(); i++) {
            Flow curr = mFlows.get(i);
            curr.spread(updatePeriod);
        }
        int diedOfTimeout = 0;
        int diedOfCollision = 0;
        for (int i = 0; i < mFlows.size(); i++) {
            Flow current = mFlows.get(i);
            if (current.mIntensity <= 0.) {
                if (!current.mSilentDeath) {
                    if (current.mTimeOut) {
                        diedOfTimeout++;
                    } else {
                        diedOfCollision++;
                    }
                }
                mFlows.remove(i);
                i--;
            }
        }
        mConfig.mAchievementGameData.enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
        mConfig.mAchievementGameData.putValue(AchievementFlow.KEY_GAME_CELLI_ACTIVE_COUNT, (long) mFlows.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        if (diedOfTimeout > 0) {
            mConfig.mAchievementGameData.increment(AchievementFlow.KEY_GAME_CELLI_TIMED_OUT_COUNT, (long) diedOfTimeout, 0L);
        }
        if (diedOfCollision > 0) {
            mConfig.mAchievementGameData.increment(AchievementFlow.KEY_GAME_CELLI_COLLIDED_COUNT, (long) diedOfCollision, 0L);
        }
        mConfig.mAchievementGameData.putValue(AchievementFlow.KEY_GAME_REVEALED_PIXELS_COUNT, (long) mRevealedPixelsCount, AchievementProperties.UPDATE_POLICY_ALWAYS);
        mConfig.mAchievementGameData.disableSilentChanges();
    }


    private class Flow {
        private final int mColor;
        private final double mStartPressure;
        private boolean mTimeOut;
        private boolean mSilentDeath;
        private double mIntensity;
        private int mX;
        private int mY;
        private FlowDirection mLastFlowDirection;
        private boolean mSearchEdges;
        private final int[] mFlowDirectionCandidates = new int[FlowDirection.DIRECTIONS_COUNT];

        public Flow(int startX, int startY, boolean silentDeath) {
            mX = startX;
            mY = startY;
            mSilentDeath = silentDeath;
            mFlowStartsX.add(mX);
            mFlowStartsY.add(mY);
            mColor = mSolutionRaster[mY][mX];
            mStartPressure = mPressureRaster[mY][mX];
            mSearchEdges = mFlows.size() % SEARCH_EDGES_EACH_X_FLOWS == 0;
            mIntensity = 1.0;
            mTimeOut = true;
        }



        public void spread(long updatePeriod) {
            int candidatesCount = 0;
            for (int d = 0; d < FlowDirection.DIRECTIONS_COUNT; d++) {
                FlowDirection curr = FlowDirection.DIRECTIONS[d];
                if (curr.hasDirection(mX, mY, mWidth, mHeight)) {
                    if (mFlowIntensityRaster[mY + curr.mYDelta][mX + curr.mXDelta] < mIntensity) {
                        if (!SEARCH_RANDOMLY_FOR_EQUAL_PRESSURES && curr == mLastFlowDirection) {
                            int currAtStart = mFlowDirectionCandidates[0];
                            mFlowDirectionCandidates[0] = d;
                            mFlowDirectionCandidates[candidatesCount] = currAtStart;
                        } else {
                            mFlowDirectionCandidates[candidatesCount] = d;
                        }
                        candidatesCount++;
                    }
                }
            }
            if (candidatesCount == 0) {
                mIntensity = 0; //die
                mTimeOut = false;
            } else {
                if (SEARCH_RANDOMLY_FOR_EQUAL_PRESSURES) {
                    shuffleArray(mFlowDirectionCandidates, candidatesCount);
                }
                // find candidate to flow to
                double bestFeatureValue = mSearchEdges ? -Double.MAX_VALUE : Double.MAX_VALUE;
                int bestNeighborX = 0;
                int bestNeighborY = 0;
                for (int i = 0; i < candidatesCount; i++) {
                    FlowDirection dir = FlowDirection.DIRECTIONS[mFlowDirectionCandidates[i]];
                    int neighborX = mX + dir.mXDelta;
                    int neighborY = mY + dir.mYDelta;
                    final double pressureToCompare =  mStartPressure;
                    double feature = mSearchEdges ? mPressureRaster[neighborY][neighborX] : Math.abs(mPressureRaster[neighborY][neighborX] - pressureToCompare);
                    boolean acceptDirection = mSearchEdges ? feature > bestFeatureValue : feature < bestFeatureValue;
                    if (acceptDirection) {
                        bestFeatureValue = feature;
                        bestNeighborX = neighborX;
                        bestNeighborY = neighborY;
                        mLastFlowDirection = dir;
                    }
                }
                mX = bestNeighborX;
                mY = bestNeighborY;
                int colorToApply = APPLY_TRUE_SOLUTION_COLOR_PER_PIXEL ? mSolutionRaster[mY][mX] : mColor;
                if (Color.alpha(colorToApply) == 0) {
                    colorToApply = Color.argb(255, mRand.nextInt(256), mRand.nextInt(256), mRand.nextInt(256));
                }
                mOutputRaster[mX + mWidth * mY] = colorToApply;
                mIntensity -= updatePeriod / (double) FLOW_MAX_DURATION;
                if (mFlowIntensityRaster[mY][mX] == 0) {
                    mRevealedPixelsCount++;
                }
                mFlowIntensityRaster[mY][mX] = mIntensity;
            }
        }
    }

    //Fisher–Yates shuffle
    private void shuffleArray(int[] ar, int length) {
        for (int i = length - 1; i > 0; i--) {
            int index = mRand.nextInt(i + 1);
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    private boolean addFlow(int x, int y, boolean silentDeath) {
        if (x >= 0 && y >= 0 && x < mWidth && y < mHeight) {
            mFlows.add(new Flow(x, y, silentDeath));
            return true;
        }
        return false;
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int x = (int) (event.getX() - mOffsetX);
            int y = (int) (event.getY() - mOffsetY);
            if (addFlow(x, y, false) && mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementFlow.KEY_GAME_CELLI_CREATED, 1L, 0L);
            }
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        //we have kind of a problem for this riddle: we cannot restore the state unless
        // we save all pixels and the intensity raster completely since only the start points
        // and the random seed will not result in the same image because of timing issues with the
        // intensity
        Compacter cmp = new Compacter();
        cmp.appendData(""); // empty slot in case it is needed
        for (int i = 0; i < mFlowStartsX.size(); i++) {
            cmp.appendData(mFlowStartsX.get(i));
            cmp.appendData(mFlowStartsY.get(i));
        }
        return cmp.compact();
    }
}
