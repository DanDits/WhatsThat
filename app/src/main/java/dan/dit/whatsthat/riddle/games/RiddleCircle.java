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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.listlock.ListLockMaxIndex;
import dan.dit.whatsthat.util.listlock.LockDistanceRefresher;

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

    public RiddleCircle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    public void onClose() {
        super.onClose();
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
        mCircleCenterX = new LinkedList<>();
        mCircleCenterY = new LinkedList<>();
        mCircleRadius = new LinkedList<>();
        mColor = new LinkedList<>();
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
    protected int calculateGainedScore() {
        return RiddleGame.DEFAULT_SCORE;
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
        int color = calculateColor(x, y, r);
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

    private int calculateColor(float x, float y, float r) {
        // by default this calculates the average brightness of the area [x-r,x+r][y-r,y+r]
        int left = (int)(x - r);
        int right = (int)(x + r);
        int top = (int)(y - r);
        int bottom = (int)(y + r);
        double brightness = 0;
        double consideredPoints = 0;
        // do not only consider pixels within the circle but within the square defined by the circle bounds
        for (int i = left; i <= right; i++) {
            for (int j = top; j <= bottom; j++) {
                int rasterIndex = j * mBitmap.getWidth() + i;
                if (rasterIndex >= 0 && rasterIndex < mRaster.length) {
                    brightness += mRaster[rasterIndex];
                    consideredPoints++;
                }
            }
        }
        // 1 = very bright -> white
        brightness /= consideredPoints;
        // logistic filter 1/(1+e^(-kx)) to minimize grey values and emphasize bright and dark ones
        // use higher k for less grey values
        brightness = 1. / (1. + Math.exp(-15. * (brightness - mAverageBrightness)));
        int grey = (int) (255. * brightness);
        return ColorAnalysisUtil.toRGB(grey, grey, grey, 255);
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
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementCircle.KEY_CIRCLE_COUNT, (long) mCircleCenterX.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
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
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementCircle.KEY_CIRCLE_DIVIDED_BY_CLICK, 1L, 0L);
            }
            return true;
        }
        return false;
    }

    private boolean onMove(float x, float y) {
        Iterator<Float> xIt = mCircleCenterX.iterator();
        Iterator<Float> yIt = mCircleCenterY.iterator();
        Iterator<Float> rIt = mCircleRadius.iterator();
        int index = 0;
        float minR = 2.f * ImageUtil.convertDpToPixel(MIN_RADIUS, mConfig.mScreenDensity);
        float humanFingerThickness = ImageUtil.convertDpToPixel(HUMAN_FINGER_THICKNESS, mConfig.mScreenDensity);
        // since the number of circles can easily get very high we are not very strict here with picking a circle
        // the first one that can evolve and is close enough will be used
        while (xIt.hasNext() && mLock.isUnlocked(index)) {
            float currX = xIt.next();
            float currY = yIt.next();
            float currR = rIt.next();
            double dist = Math.sqrt((currX - x) * (currX - x) + (currY - y) * (currY - y));
            if (dist <= Math.max(currR, humanFingerThickness) && currR >= minR) {
                mLock.lock(1);
                float newRadius = currR / 2.f;
                // don't redraw all but only the required area
                mCirclesCanvas.drawRect(currX - currR, currY - currR, currX + currR, currY + currR, mClearPaint);
                evolveCircleUnchecked(index, currX, currY, newRadius, true);
                if (mConfig.mAchievementGameData != null) {
                    mConfig.mAchievementGameData.increment(AchievementCircle.KEY_CIRCLE_DIVIDED_BY_MOVE, 1L, 0L);
                }
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
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
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementCircle.KEY_CIRCLE_COUNT, (long) mCircleCenterX.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
    }

}