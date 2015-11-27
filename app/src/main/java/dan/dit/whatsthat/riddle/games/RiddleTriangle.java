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
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementTriangle;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.control.RiddleScore;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.listlock.ListLockMaxIndex;
import dan.dit.whatsthat.util.listlock.LockDistanceRefresher;

/**
 * Created by daniel on 09.05.15.
 */
public class RiddleTriangle extends RiddleGame {
    private static final float MIN_AREA_ESTIMATE = 30;
    private static final float SPLIT_MAX_DISTANCE = 40;
    public static final int MAX_SPLIT_PER_CLICK = 15;
    private static final int X_SAMPLES_COUNT = 10;
    private static final int Y_SAMPLES_COUNT = 10; // will only consider X_SAMPLES * Y_SAMPLES pixels per triangle
    private static final int MAX_TRIANGLES_FOR_SCORE_BONUS = 500;
    private List<Triangle> mTriangles;

    private Bitmap mBackground;
    private Canvas mBackgroundCanvas;
    private Paint mClearPaint;
    private Paint mTrianglePaint;
    private ListLockMaxIndex mLock;
    private LockDistanceRefresher mLockRefresher;
    private boolean mFeatureDivideByMove;

    public RiddleTriangle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected @NonNull RiddleScore calculateGainedScore() {
        int bonus = (mTriangles.size() <= MAX_TRIANGLES_FOR_SCORE_BONUS ? Types.SCORE_SIMPLE : 0);
        return super.calculateGainedScore().addBonus(bonus);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mBackground, 0, 0, null);
    }

    @Override
    public void onClose() {
        super.onClose();
        mTriangles = null;
        mBackground = null;
        mBackgroundCanvas = null;
        mTrianglePaint = null;
        mClearPaint = null;
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mBackground = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, mBitmap.getConfig());
        listener.onProgressUpdate(25);
        mBackgroundCanvas = new Canvas(mBackground);
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFeatureDivideByMove = TestSubject.getInstance().hasFeature(SortimentHolder.ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE);

        mTriangles = new ArrayList<>();
        mLock = new ListLockMaxIndex(mTriangles, ListLockMaxIndex.UNLIMITED_ELEMENTS);
        mLockRefresher = new LockDistanceRefresher(mLock, Math.min(mConfig.mWidth, mConfig.mHeight) / 2.f);
        mTrianglePaint = new Paint();
        mTrianglePaint.setAntiAlias(true);
        mTrianglePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTrianglePaint.setStrokeWidth(0.35f * mConfig.mScreenDensity / DisplayMetrics.DENSITY_HIGH);
        mTrianglePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        listener.onProgressUpdate(50);
        initTriangles(getCurrentState());
    }

    private void initTriangles(Compacter cmp) {
        boolean error = false;
        if (cmp != null && cmp.getSize() > 2) {
            int loadedWidth = 0;
            int loadedHeight = 0;
            try {
                loadedWidth = cmp.getInt(0);
                loadedHeight = cmp.getInt(1);
            } catch (CompactedDataCorruptException e) {
                error = true;
            }
            if (!error && loadedWidth == mConfig.mWidth && loadedHeight == mConfig.mHeight) {
                for (int i = 3; i + 5 < cmp.getSize(); i += 6) {
                    try {
                        Triangle t = new Triangle(cmp.getFloat(i), cmp.getFloat(i + 1), cmp.getFloat(i + 2), cmp.getFloat(i + 3), cmp.getFloat(i + 4), cmp.getFloat(i + 5));
                        t.draw(mBitmap, mBackgroundCanvas, mTrianglePaint);
                        mTriangles.add(t);
                    } catch (CompactedDataCorruptException e) {
                        Log.e("Riddle", "Compacted data corrupt for triangle: " + e);
                        error = true;
                        break;
                    }
                }
            }
        }
        if (cmp == null || error) {
            mTriangles.clear();
            mTriangles.add(new Triangle(0f, 0f, 0f, mConfig.mHeight, mConfig.mWidth, mConfig.mHeight).draw(mBitmap, mBackgroundCanvas, mTrianglePaint));
            mTriangles.add(new Triangle(0f, 0f, mConfig.mWidth, 0, mConfig.mWidth, mConfig.mHeight).draw(mBitmap, mBackgroundCanvas, mTrianglePaint));
        }
    }

    private int onClick(float x, float y, int maxCount) {
        int stopBefore = mTriangles.size();
        int splitCount = 0;
        for (int i = 0; i < stopBefore && mLock.isUnlocked(i) && splitCount < maxCount; i++) {
            Triangle curr = mTriangles.get(i);
                if (curr.areaEstimate() > MIN_AREA_ESTIMATE &&
                        (curr.isInside(x, y) || curr.minCornerDistSquared(x, y) < SPLIT_MAX_DISTANCE * SPLIT_MAX_DISTANCE)) {
                mTriangles.remove(curr);
                int delta = -1 + curr.splitAndAdd(mTriangles, mBitmap, mBackgroundCanvas, mClearPaint, mTrianglePaint);

                splitCount++;
                i -= delta;
                stopBefore -= delta;
                mLock.lock(delta);
            }
        }
        return splitCount;
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        mLockRefresher.update(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int splitCount = onClick(event.getX(), event.getY(), MAX_SPLIT_PER_CLICK);
            if (splitCount > 0) {
                updateAchievementTriangleCount(AchievementTriangle.KEY_TRIANGLE_DIVIDED_BY_CLICK, splitCount);
            }
            return splitCount > 0;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && mFeatureDivideByMove) {
            int splitCount = onClick(event.getX(), event.getY(), (int) Math.max(1, MAX_SPLIT_PER_CLICK / 3.));
            if (splitCount > 0) {
                updateAchievementTriangleCount(AchievementTriangle.KEY_TRIANGLE_DIVIDED_BY_MOVE, splitCount);
            }
            return splitCount > 0;
        }
        return false;
    }

    private void updateAchievementTriangleCount(String divisionTypeKey, int delta) {
        if (mConfig.mAchievementGameData == null) {
            return;
        }
        mConfig.mAchievementGameData.enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
        if (!TextUtils.isEmpty(divisionTypeKey) && delta > 0) {
            mConfig.mAchievementGameData.increment(divisionTypeKey, (long) delta, 0L);
            mConfig.mAchievementGameData.putValue(AchievementTriangle.KEY_TRIANGLE_COUNT_BEFORE_LAST_INTERACTION, (long) (mTriangles.size() - delta), AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        mConfig.mAchievementGameData.putValue(AchievementTriangle.KEY_TRIANGLE_COUNT, (long) mTriangles.size(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        mConfig.mAchievementGameData.disableSilentChanges();
    }

    @Override
    public Bitmap makeSnapshot() {
        int width = SNAPSHOT_DIMENSION.getWidthForDensity(mConfig.mScreenDensity);
        int height = SNAPSHOT_DIMENSION.getHeightForDensity(mConfig.mScreenDensity);
        return BitmapUtil.resize(mBackground, width, height);
    }

    @Override
    protected void initAchievementData() {
        updateAchievementTriangleCount(null, 0);
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        Compacter cmp = new Compacter();
        cmp.appendData(mConfig.mWidth);
        cmp.appendData(mConfig.mHeight);
        cmp.appendData(""); // in case we need it
        for (Triangle t : mTriangles) {
            cmp.appendData(t.x1);
            cmp.appendData(t.y1);
            cmp.appendData(t.x2);
            cmp.appendData(t.y2);
            cmp.appendData(t.x3);
            cmp.appendData(t.y3);
        }
        return cmp.compact();
    }

    private static float distSquared(float x1, float y1, float x2, float y2) {
        return (x1-x2) * (x1-x2) + (y1-y2) * (y1-y2);
    }

    private static class Triangle {
        float x1, y1, x2, y2, x3, y3;

        private static final RectF BOUND = new RectF();
        private static final Path LINES = new Path();

        private Triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
            this.x1 = x1;
            this.y1= y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
        }

        private float areaEstimate() {
            // exact for our triangles, but not for general triangles
            return 0.5f * Math.max(Math.abs(x1 - x2), Math.abs(x1 - x3)) * Math.max(Math.abs(y1 - y2), Math.abs(y1 - y3));
        }

        private Triangle draw(Bitmap bitmap, Canvas canvas, Paint paint) {
            initPath();
            int rgb = calculateColor(bitmap);
            paint.setColor(rgb);
            canvas.drawPath(LINES, paint);
            return this;
        }

        private int splitAndAdd(List<Triangle> triangles, Bitmap bitmap, Canvas canvas, Paint clear, Paint paint) {
            initPath();
            //canvas.drawPath(LINES, clear);
            float d1 = distSquared(x1, y1, x2, y2);
            float d2 = distSquared(x1, y1, x3, y3);
            float d3 = distSquared(x2, y2, x3, y3);
            float p1x, p1y, p2x, p2y, p3x, p3y, sharedx, sharedy;
            if (d1 > d2 && d1 > d3) {
                p1x = x1; p1y = y1;
                p2x = x2; p2y = y2;
                p3x = x3; p3y = y3;
            } else if (d2 > d1 && d2 > d3) {
                p1x = x1; p1y = y1;
                p2x = x3; p2y = y3;
                p3x = x2; p3y = y2;
            } else {
                p1x = x2; p1y = y2;
                p2x = x3; p2y = y3;
                p3x = x1; p3y = y1;
            }
            sharedx  = p1x + (p2x - p1x) * 0.5f;
            sharedy = p1y + (p2y - p1y) * 0.5f;
            Triangle t1 = new Triangle(p1x, p1y, sharedx, sharedy, p3x, p3y);
            Triangle t2 = new Triangle(p2x, p2y, sharedx, sharedy, p3x, p3y);
            t1.draw(bitmap, canvas, paint);
            t2.draw(bitmap, canvas, paint);
            triangles.add(t1);
            triangles.add(t2);
            return 2;
        }

        private void initPath() {
            LINES.rewind();
            LINES.moveTo(x1, y1);
            LINES.lineTo(x2, y2);
            LINES.lineTo(x3, y3);
            LINES.close();
        }
        private int calculateColor(Bitmap forColor) {
            LINES.computeBounds(BOUND, true);
            int red = 0;
            int green = 0;
            int blue = 0;
            int alpha = 0;
            int pixelInTriangle = 0;
            final int xStepSize = Math.max(1, (int) (BOUND.right - BOUND.left) / X_SAMPLES_COUNT);
            final int yStepSize = Math.max(1, (int) (BOUND.bottom - BOUND.top) / Y_SAMPLES_COUNT);
            for (int x = (int) BOUND.left; x < BOUND.right; x+=xStepSize) {
                for (int y = (int) BOUND.top; y < BOUND.bottom; y+=yStepSize) {
                    if (x < forColor.getWidth() && y < forColor.getHeight() && isInside(x, y)) {
                        int bitmapRgb = forColor.getPixel(x, y);
                        red += Color.red(bitmapRgb);
                        green += Color.green(bitmapRgb);
                        blue += Color.blue(bitmapRgb);
                        alpha += Color.alpha(bitmapRgb);
                        pixelInTriangle++;
                    }
                }
            }
            if (pixelInTriangle > 0) {
                red /= pixelInTriangle;
                green /= pixelInTriangle;
                blue /= pixelInTriangle;
                alpha /= pixelInTriangle;
                return Color.argb(alpha, red, green, blue);
            } else {
                return Color.TRANSPARENT;
            }
        }

        private static float sign(float x1, float y1, float x2, float y2, float x3, float y3) {
            return (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
        }

        private boolean isInside (float x, float y) {
            boolean b1, b2, b3;
            b1 = sign(x, y, x1, y1, x2, y2) < 0.0f;
            b2 = sign(x, y, x2, y2, x3, y3) < 0.0f;
            b3 = sign(x, y, x3, y3, x1, y1) < 0.0f;

            return ((b1 == b2) && (b2 == b3));
        }

        public float minCornerDistSquared(float x, float y) {
            float d1 = distSquared(x1, y1, x, y);
            float d2 = distSquared(x2, y2, x, y);
            float d3 = distSquared(x3, y3, x, y);
            return Math.min(d1, Math.min(d2, d3));
        }
    }

}
