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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;

import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.ParticleSystemPool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.control.LookRiddleAnimation;
import dan.dit.whatsthat.riddle.control.RiddleAnimation;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.control.RiddleScore;
import dan.dit.whatsthat.riddle.types.TypesHolder;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.flatworld.look.Frames;
import dan.dit.whatsthat.util.flatworld.look.FramesOneshot;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.listlock.ListLockMaxIndex;
import dan.dit.whatsthat.util.listlock.LockDistanceRefresher;
import dan.dit.whatsthat.util.mosaic.reconstruction.pattern.CirclePatternReconstructor;

/**
 * A specific riddle implementation that hides the image behind circles.
 * Each circle can be split into 4 smaller circles by clicking on it or moving nearby.
 * The circle color is a sample from the brightness of the pixels that are covered by the circle (square area).
 * Created by daniel on 31.03.15.
 */
public class RiddleCircle extends RiddleGame {
    private static final float MIN_RADIUS = 2.0f; // dp >=1, minimum radius for each circle, will click other nearby circles instead
    /*
     * A value that is kind of a magic number that makes clicking on circles feel less painful because of the finger and screen inaccuracy.
     * If the circle center and click point are within this euclidian distance from each other it will click the circle.
     * 0.433070866 = 11mm = average finger thickness , needs to be multiplied by screen density
     */
    private static final float HUMAN_FINGER_THICKNESS = 20.f; //  dp
    private static final float FRAME_WIDTH = 3f;
    // reasonably high, will be the biggest ones too, higher or unlimited can kill the main thread
    // for a test you can easily go up to 50k circles in no time (with no riddle limits and MIN_RADIUS=1.0f) on a <= 400x400 riddle
    private static final int MODE_MOVING_MAX_CIRCLES_CHECKED = 5000;
    private static final int MAX_CIRCLES_FOR_EXTRA_SCORE = 200;
    private static final int MAX_CIRCLES_FOR_EXTRA_EXTRA_SCORE = 100;

    /*
     * Holds the brightness for each pixel of the original bitmap (row wise pixel evaluation).
     */
    private double[]mRaster;

    /*
     * Store each circles essential values for easy lightweight plotting. Could also be done by drawing
     * to a bitmap and only updating the required region, but so far the calculation overhead is ok.
     */
    private List<Float> mCircleCenterX;
    private List<Float> mCircleCenterY;
    private List<Float> mCircleRadius;
    private List<Integer> mColor;

    /*
     * Required for drawing circle and background and color calculation.
     */
    private Paint mPaint;
    private Paint mFramePaint;
    private Paint mClearPaint;
    private Bitmap mCirclesBitmap;
    private Canvas mCirclesCanvas;

    /*
     * Internal coordinate system is offset with these coordinates to center the canvas within the view
     * if the bitmap is smaller than the view.
     */
    private float mTopLeftCornerY;
    private float mTopLeftCornerX;
    private double mAverageBrightness;

    private ListLockMaxIndex mLock;
    private LockDistanceRefresher mLockRefresher;
    private boolean mFeatureDivideByMove;
    private Resources mRes;

    private Timer mTimer;
    private boolean mForbidCircleDivision;
    private LookRiddleAnimation mBigBrotherAnim;
    private ParticleSystemPool mParticlePool;

    public RiddleCircle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mRaster = null;
        mPaint = null;
        mFramePaint = null;
        mCirclesCanvas = null;
        mCirclesBitmap = null;
        mCircleCenterX = null;
        mCircleCenterY = null;
        mCircleRadius = null;
        mClearPaint = null;
        mColor = null;
    }

    @Override
    public void initBitmap(Resources res, PercentProgressListener listener) {
        mRes = res;

        mParticlePool = new ParticleSystemPool(new ParticleSystemPool.ParticleSystemMaker() {
            @Override
            public ParticleSystem makeParticleSystem() {
                return RiddleCircle.this.makeParticleSystem(mRes, 10, R.drawable
                        .spark, 400L).setFadeOut(200, new AccelerateInterpolator());
            }
        }, 10);
        // fill raster with brightness and calculate average brightness
        {
            mRaster = new double[mBitmap.getHeight() * mBitmap.getWidth()];
            int index = 0;
            for (int y = 0; y < mBitmap.getHeight(); y++) {
                for (int x = 0; x < mBitmap.getWidth(); x++) {
                    mRaster[index] = ColorAnalysisUtil.getBrightnessWithAlpha(mBitmap.getPixel(x, y));
                    mAverageBrightness += mRaster[index];
                    index++;
                }
            }
        }
        mAverageBrightness /= mBitmap.getWidth() * mBitmap.getHeight();

        listener.onProgressUpdate(35);
        mFeatureDivideByMove = TestSubject.getInstance().hasFeature(SortimentHolder.ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE);

        mCirclesBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        mCirclesCanvas = new Canvas(mCirclesBitmap);
        listener.onProgressUpdate(50);

        //setup colors and paint

        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mFramePaint = new Paint();
        mFramePaint.setAntiAlias(true);
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(FRAME_WIDTH);

        //init value holder for circles
        mCircleCenterX = new ArrayList<>();
        mCircleCenterY = new ArrayList<>();
        mCircleRadius = new ArrayList<>();
        mColor = new ArrayList<>();
        mLock = new ListLockMaxIndex(mCircleCenterX, MODE_MOVING_MAX_CIRCLES_CHECKED);
        mLockRefresher = new LockDistanceRefresher(mLock, Math.max(mBitmap.getWidth(), mBitmap.getHeight()) / 2.f);

        listener.onProgressUpdate(66);

        // try reconstructing circles
        Compacter cmp = getCurrentState();
        if (cmp != null && cmp.getSize() > 3) {
            // we are reconstructing this riddle, lets try it if dimensions kinda match
            double aspectRatio = mBitmap.getWidth() / ((double) mBitmap.getHeight());
            int widthLoaded = -1;
            double aspectRatioLoaded = -1;
            try {
                widthLoaded = cmp.getInt(0);
                aspectRatioLoaded = widthLoaded / ((double) cmp.getInt(1));
            } catch (CompactedDataCorruptException e) {
                Log.e("Riddle", "Could not load width/height from data to reconstruct circle " + e.getMessage());
            }
            if (Math.abs(aspectRatio - aspectRatioLoaded) < 1E-3) {
                // equal ratios
                float scaling = mBitmap.getWidth() / ((float) widthLoaded);
                int index = 2;
                while (index + 3 < cmp.getSize()) {
                    try {
                        addCircle(scaling * cmp.getFloat(++index), scaling * cmp.getFloat(++index), scaling * cmp.getFloat(++index), true);
                    } catch (CompactedDataCorruptException e) {
                        Log.e("Riddle", "Could not circle data when reconstructing " + e.getMessage());
                        break;
                    }
                }
            }
        }
        listener.onProgressUpdate(90);
        if (mCircleCenterX.size() == 0) {
            //init basic circle(s), one circle in the center with maximum radius in bounds, we prefer square views.
            initCircles(0.f, 0.f, mBitmap.getWidth(), mBitmap.getHeight());
        }

        //riddle is now fully initialized and ready to be displayed and interacted with
    }

    @Override
    public void onGotVisible() {
        if (mCircleCenterX.size() == 1) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bigBrotherAnimationChecked();
                }
            }, 60000L);
        }
    }

    private void bigBrotherAnimationChecked() {
        if (mCircleCenterX != null && mCircleCenterX.size() == 1) {
            long step1Duration = 5000L;
            long step2Duration = 200L;
            long step3Duration = 4000L;
            long step4Duration = 300L;
            long step5Duration = 4000L;
            long totalLifeTime = step1Duration + step2Duration + step3Duration + step4Duration +
                    step5Duration;
            int sizeFraction = 2;
            Bitmap[] frames = new Bitmap[5];
            frames[0] = ImageUtil.loadBitmap(mRes, R.drawable.googly_eyes, mConfig.mWidth / sizeFraction, mConfig
                    .mHeight / sizeFraction, true);
            frames[1] = null;
            frames[2] = frames[0];
            frames[3] = null;
            frames[4] = frames[0];
            Frames look = new FramesOneshot(frames, totalLifeTime)
                    .setFrameDuration(0, step1Duration)
                    .setFrameDuration(1, step2Duration)
                    .setFrameDuration(2, step3Duration)
                    .setFrameDuration(3, step4Duration)
                    .setFrameDuration(4, step5Duration);
            mBigBrotherAnim = new LookRiddleAnimation(look, mConfig.mWidth / 2 - look
                    .getWidth() / 2, mConfig.mHeight / 3 - look.getHeight() / 2, totalLifeTime);
            mBigBrotherAnim.setStateListener(new RiddleAnimation.StateListener() {
                @Override
                public void onBorn() {
                    mForbidCircleDivision = true;
                }

                @Override
                public void onKilled(boolean murdered) {
                    mForbidCircleDivision = false;

                }
            });
            addAnimation(mBigBrotherAnim);
        }
    }

    private boolean initCircles(float topLeftX, float topLeftY, float width, float height) {
        float halfWidth = width / 2.f;
        float halfHeight = height / 2.f;
        float r = Math.min(halfWidth, halfHeight);
        if (!addCircle(topLeftX + halfWidth, topLeftY + halfHeight, r, true)) {
            return false;
        }
        if (2 * r <= width - 4 * ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity)) {
            // we got a landscape bitmap... and there is enough space on the left and right for some circles, fill it
            initCircles(topLeftX, topLeftY, halfWidth -  r, height);
            initCircles(topLeftX + halfWidth + r, topLeftY, halfWidth -  r, height);
        } else if (2 * r <= height - 4 * ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity)) {
            // we got a portrait bitmap, fill it if possible, see landscape for more details
            initCircles(topLeftX, topLeftY, width, halfHeight- r);
            initCircles(topLeftX, topLeftY + halfHeight + r, width, halfHeight - r);
        }
        return true;
    }

    @Override
    protected void addBonusReward(@NonNull RiddleScore.Rewardable rewardable) {
        int bonus = (mCircleCenterX.size() < MAX_CIRCLES_FOR_EXTRA_SCORE ? TypesHolder.SCORE_SIMPLE :
                    mCircleCenterX.size() < MAX_CIRCLES_FOR_EXTRA_EXTRA_SCORE ? TypesHolder
                            .SCORE_MEDIUM : 0);
        rewardable.addBonus(bonus);
    }

    /**
     * Adds a circle at given internal coordinates if these are within bounds of the bitmap.
     * @param x The x center coordinate.
     * @param y The y center coordinate.
     * @param r The radius of the circle.
     * @param draw If the circle should draw itself
     * @return Only true if a new circle was added and this circle was fully inside bounds.
     */
    private boolean addCircle(float x, float y, float r, boolean draw) {
        if (r < ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity) || x - r < 0 || x + r > mBitmap.getWidth() || y - r < 0 || y + r > mBitmap.getHeight()) {
            return false; // out of bounds
        }
        mCircleCenterX.add(x);
        mCircleCenterY.add(y);
        mCircleRadius.add(r);
        int color = CirclePatternReconstructor.calculateColor(mRaster, mAverageBrightness,
                mBitmap.getWidth(), mBitmap.getHeight(), x, y, r);
        mColor.add(color);
        if (draw) {
            mPaint.setColor(color);
            mCirclesCanvas.drawCircle(x, y, r, mPaint);
        }
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        mTopLeftCornerX = Math.abs(mBitmap.getWidth() - canvas.getWidth()) / 2;
        mTopLeftCornerY = Math.abs(mBitmap.getHeight() - canvas.getHeight()) / 2;
        canvas.drawBitmap(mCirclesBitmap, mTopLeftCornerX, mTopLeftCornerY, null);
        drawBorder(canvas);
    }

    private void drawBorder(Canvas canvas) {
        canvas.drawRect(mTopLeftCornerX, mTopLeftCornerY, mTopLeftCornerX + mBitmap.getWidth(), mTopLeftCornerY + mBitmap.getHeight(), mFramePaint);
    }


    // splits the circle into 4 subcircles, appends them and removes itself from the list
    private void evolveCircleUnchecked(int index, float x, float y, float radius, boolean draw) {
        mCircleCenterX.remove(index);
        mCircleCenterY.remove(index);
        mCircleRadius.remove(index);
        mColor.remove(index);
        addCircle(x - radius, y - radius, radius, draw);
        addCircle(x + radius, y - radius, radius, draw);
        addCircle(x - radius, y + radius, radius, draw);
        addCircle(x + radius, y + radius, radius, draw);
        mConfig.mAchievementGameData.putValue(AchievementCircle.KEY_CIRCLE_COUNT, (long) mCircleCenterX.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
    }

    private void reDraw() {
        Iterator<Float> xIt = mCircleCenterX.iterator();
        Iterator<Float> yIt = mCircleCenterY.iterator();
        Iterator<Float> rIt = mCircleRadius.iterator();
        Iterator<Integer> colorIt = mColor.iterator();
        mCirclesCanvas.drawRect(0, 0, mCirclesCanvas.getWidth(), mCirclesCanvas.getHeight(), mClearPaint);
        while (rIt.hasNext()) {
            float x = xIt.next();
            float y = yIt.next();
            float r = rIt.next();
            int color = colorIt.next();
            mPaint.setColor(color);
            mCirclesCanvas.drawCircle(x, y, r, mPaint);
        }
    }

    private boolean onTouchDown(float clickX, float clickY) {
        // first step: find closest circle that still can split up into smaller circles
        double maxDist = Double.MAX_VALUE;
        Iterator<Float> xIt = mCircleCenterX.iterator();
        Iterator<Float> yIt = mCircleCenterY.iterator();
        Iterator<Float> rIt = mCircleRadius.iterator();
        int closestIndex = 0;
        float closestX = 0;
        float closestY = 0;
        float closestRadius = 1;
        for (int i = 0; i < mCircleRadius.size(); i++) {
            float x = xIt.next();
            float y = yIt.next();
            float r = rIt.next();
            double dist = Math.sqrt((clickX - x)*(clickX - x) + (clickY - y)*(clickY - y));
            if (dist < maxDist && r >= 2.f * ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity)) {
                maxDist = dist;
                closestIndex = i;
                closestX = x;
                closestY = y;
                closestRadius = r;
            }
        }
        // next step: remove closest circle, add 4 new ones inside the old one if we can split further
        float newRadius = closestRadius / 2.f;
        double distanceClickAndClosest = Math.sqrt((clickX - closestX) * (clickX - closestX) + (clickY - closestY) * (clickY - closestY));
        if (newRadius >= ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity) && (distanceClickAndClosest <= closestRadius || distanceClickAndClosest <= ImageUtil.convertDpToPixel(HUMAN_FINGER_THICKNESS, mConfig.mScreenDensity))) {

            mCirclesCanvas.drawRect(closestX - closestRadius, closestY - closestRadius, closestX + closestRadius, closestY+ closestRadius, mClearPaint);
            evolveCircleUnchecked(closestIndex, closestX, closestY, newRadius, true);
            mConfig.mAchievementGameData.increment(AchievementCircle.KEY_CIRCLE_DIVIDED_BY_CLICK, 1L, 0L);
            return true;
        }
        return false;
    }

    private boolean onMove(float x, float y) {
        int index = 0;
        float minR = 2.f * ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity);
        float humanFingerThickness = ImageUtil.convertDpToPixel(HUMAN_FINGER_THICKNESS, mConfig.mScreenDensity);
        // since the number of circles can easily get very high we are not very strict here with picking a circle
        // the first one that can evolve and is close enough will be used
        while (index < mCircleCenterX.size() && mLock.isUnlocked(index)) {
            float currX = mCircleCenterX.get(index);
            float currY = mCircleCenterY.get(index);
            float currR = mCircleRadius.get(index);
            double dist = Math.sqrt((currX - x) * (currX - x) + (currY - y) * (currY - y));
            if (dist <= Math.max(currR, humanFingerThickness) && currR >= minR) {
                mLock.lock(1);
                float newRadius = currR / 2.f;
                // don't redraw all but only the required area
                mCirclesCanvas.drawRect(currX - currR, currY - currR, currX + currR, currY + currR, mClearPaint);
                evolveCircleUnchecked(index, currX, currY, newRadius, true);
                mConfig.mAchievementGameData.increment(AchievementCircle.KEY_CIRCLE_DIVIDED_BY_MOVE, 1L, 0L);

                ParticleSystem system = mParticlePool.obtain();
                if (system != null) {
                    system.clearInitializers();
                    float scale = 1.f + 3f * (currR * 2f / mConfig.mWidth); // factor from 1 to 4
                    system.setSpeedModuleAndAngleRange(0.05f, 0.15f, 0, 360)
                            .setAccelerationModuleAndAndAngleRange(0.0001f, 0.0002f, 0, 360)
                            .setScaleRange(scale - 0.05f, scale + 0.05f);
                    system.emit((int) currX, (int) currY, 10, 300);
                }
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (mForbidCircleDivision) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                long clicksDone = mConfig.mAchievementGameData.increment(AchievementCircle
                        .KEY_FORBIDDEN_CIRCLE_DIVISION_CLICK, 1L, 0L);
                if (clicksDone >= AchievementCircle.Achievement12.REQUIRED_CLICKS_TO_VICTORY &&
                        mBigBrotherAnim != null) {
                    mBigBrotherAnim.murder();
                }
            }
            return false;
        }
        mLockRefresher.update(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return onTouchDown(event.getX() - mTopLeftCornerX, event.getY() - mTopLeftCornerY);
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mFeatureDivideByMove) {
            return onMove(event.getX() - mTopLeftCornerX, event.getY() - mTopLeftCornerY);
        }
        return false;

    }


    @Override
    protected @NonNull String compactCurrentState() {
        Compacter cmp = new Compacter(mCircleRadius.size() + 5);
        cmp.appendData(mBitmap.getWidth());
        cmp.appendData(mBitmap.getHeight());
        cmp.appendData(""); // in case we need the slot

        // save a bunch of circles, can take quite some memory if MIN_RADIUS is too small and fully evolved a huge bitmap
        Iterator<Float> xIt = mCircleCenterX.iterator();
        Iterator<Float> yIt = mCircleCenterY.iterator();
        Iterator<Float> rIt = mCircleRadius.iterator();
        while (xIt.hasNext()) {
            cmp.appendData(xIt.next());
            cmp.appendData(yIt.next());
            cmp.appendData(rIt.next());
        }
        return cmp.compact();
    }

    @Override
    protected Bitmap makeSnapshot() {
        int width = SNAPSHOT_DIMENSION.getWidthForDensity(mConfig.mScreenDensity);
        int height = SNAPSHOT_DIMENSION.getHeightForDensity(mConfig.mScreenDensity);
        return Bitmap.createScaledBitmap(mCirclesBitmap, width, height, false);
    }

    @Override
    protected void initAchievementData() {
        mConfig.mAchievementGameData.putValue(AchievementCircle.KEY_CIRCLE_COUNT, (long) mCircleCenterX.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
    }

}