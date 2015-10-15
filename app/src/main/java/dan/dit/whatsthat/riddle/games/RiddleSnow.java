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
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementSnow;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.flatworld.collision.GeneralHitboxCollider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.collision.HitboxCircle;
import dan.dit.whatsthat.util.flatworld.effects.WorldEffect;
import dan.dit.whatsthat.util.flatworld.look.BitmapLook;
import dan.dit.whatsthat.util.flatworld.look.CircleLook;
import dan.dit.whatsthat.util.flatworld.look.Frames;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.look.NinePatchLook;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMoonMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNewtonFrictionMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNoMover;
import dan.dit.whatsthat.util.flatworld.world.Actor;
import dan.dit.whatsthat.util.flatworld.world.FlatRectWorld;
import dan.dit.whatsthat.util.flatworld.world.FlatWorldCallback;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 15.04.15.
 */
public class RiddleSnow extends RiddleGame implements FlatWorldCallback {
    private static final float STATE_DELTA_ON_IDEA_CHILD_COLLECT_WITH_ACTIVE_DEVIL = 0.1f;
    private static final float STATE_DELTA_ON_IDEA_CHILD_COLLECT = 1.f/3.f;
    private static final int STATE_DELTA_ON_WALL_EXPLOSION = 2;
    private static final float SNOWBALL_BASE_START_FRACTION = 1.f/4.f;
    private static final float GRAVITY = 400.f; //dp
    private static final float SNOWBALL_SCREENSIZE_MAX_FRACTION = 1.f / 3.f;
    private static final float BORDER_WIDTH = 3f;
    private static final float IDEA_COLLECTION_RADIUS_BASE = 21;
    public static final int IDEAS_REQUIRED_FOR_MAX_SIZE = 10;
    private static final long RELOAD_RIDDLE_BLOCK_DURATION = 2000L; //ms
    private static final long EXPLOSION_DELAY = 3000; //ms
    private static final long TOUCH_GRAVITY_FULL_EFFECT_DELAY = 500; //ms
    private static final double EXPLOSION_HIT_WALL_FRACTION = 0.4;
    private static final double SNOW_EXPLOSION_SIZE_MULTIPLIER = 1.25;
    private static final float CRASHED_WALL_BIGGER_SPEED_MULTIPLIER = 1.5f;
    private static final float CRASHED_WALL_SMALL_SPEED_MULTIPLIER = 0.5f;
    private static final float EXPLOSION_SPEED_MULTIPLIER = 10.f;
    private static final float DEVIL_RADIUS_FRACTION_OF_CELL_MAX_RADIUS = 0.25f;
    public static final boolean DEFAULT_DEVIL_IS_VISIBLE = true;
    private static final int MAX_WALL_COLLISONS_FOR_SCORE_BONUS = 0;
    private static final String CACHE_FULL_EXPLOSION0 = Types.Snow.NAME + "FullExplosion0";
    private static final String CACHE_FULL_EXPLOSION1 = Types.Snow.NAME + "FullExplosion1";
    private static final String CACHE_FULL_EXPLOSION2 = Types.Snow.NAME + "FullExplosion2";
    private static final String CACHE_FULL_EXPLOSION3 = Types.Snow.NAME + "FullExplosion3";
    private static final String CACHE_FULL_EXPLOSION4 = Types.Snow.NAME + "FullExplosion4";

    private long mReloadRiddleMoveBlockDuration;

    private Bitmap mBackgroundSnow;
    private Bitmap mFogLayer;
    private Canvas mFogLayerCanvas;
    private Bitmap[] mFullExplosion;
    private Paint mExplosionPaint;
    private FlatRectWorld mWorld;

    private float mGravity;
    private float mRiddleOffsetX;
    private float mRiddleOffsetY;
    private Paint mBorderPaint;
    private Random mRand;
    private boolean mFeatureTouchGravity;
    private long mTouchGravityStartPressTime;
    private float mTouchGravityPressX;
    private float mTouchGravityPressY;
    private Canvas mBackgroundSnowCanvas;
    private boolean mTouchGravityIsPressed;
    private List<Float> mExplosionHistoryX;
    private List<Float> mExplosionHistoryY;
    private List<Integer> mExplosionHistoryType;
    private List<Integer> mExplosionHistorySize;
    private Paint mClearPaint;
    private Cell mCell;
    private Idea mIdea;
    private Devil mDevil;
    private long mIdleTimeCounter;

    public RiddleSnow(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    public void onClose() {
        super.onClose();
        mWorld = null;
        mBackgroundSnow = null;
        mBorderPaint = null;
        mRand = null;
        mBackgroundSnowCanvas = null;
        mFogLayer = null;
        mFogLayerCanvas = null;
        ImageUtil.CACHE.freeImage(CACHE_FULL_EXPLOSION0, mFullExplosion[0]);
        ImageUtil.CACHE.freeImage(CACHE_FULL_EXPLOSION1, mFullExplosion[1]);
        ImageUtil.CACHE.freeImage(CACHE_FULL_EXPLOSION2, mFullExplosion[2]);
        ImageUtil.CACHE.freeImage(CACHE_FULL_EXPLOSION3, mFullExplosion[3]);
        ImageUtil.CACHE.freeImage(CACHE_FULL_EXPLOSION4, mFullExplosion[4]);
        mFullExplosion = null;
        mExplosionPaint = null;
        mExplosionHistoryX = null;
        mExplosionHistoryY = null;
        mExplosionHistorySize = null;
        mExplosionHistoryType = null;
        mClearPaint = null;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isNotClosed()) {
            return;
        }
        canvas.drawBitmap(mBackgroundSnow, 0, 0, null);
        mWorld.draw(canvas, null);
        canvas.drawRect(BORDER_WIDTH / 2.f, BORDER_WIDTH / 2.f, mConfig.mWidth - BORDER_WIDTH / 2.f, mConfig.mHeight - BORDER_WIDTH / 2.f, mBorderPaint);
    }


    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        setTouchControl(!TestSubject.getInstance().hasToggleableFeature(SortimentHolder.ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR));

        mWorld = new FlatRectWorld(new RectF(0, 0, mConfig.mWidth, mConfig.mHeight), new GeneralHitboxCollider(), this);
        mRand = new Random();
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(BORDER_WIDTH);
        mBorderPaint.setColor(Color.BLACK);
        mRiddleOffsetX = (mConfig.mWidth - mBitmap.getWidth()) / 2.f;
        mRiddleOffsetY = (mConfig.mHeight - mBitmap.getHeight()) / 2.f;

        listener.onProgressUpdate(30);
        mGravity = ImageUtil.convertDpToPixel(GRAVITY, mConfig.mScreenDensity);

        // fog layer bitmap will be changed in progress so caching it makes no sense
        mFogLayer = ImageUtil.loadBitmap(res, R.drawable.nebel, mConfig.mWidth, mConfig.mHeight, BitmapUtil.MODE_FIT_EXACT);
        mFogLayer.setHasAlpha(true);
        mFogLayerCanvas = new Canvas(mFogLayer);
        mBackgroundSnow = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, mBitmap.getConfig());
        mBackgroundSnow.setHasAlpha(true);
        mBackgroundSnowCanvas = new Canvas(mBackgroundSnow);
        drawBackground();
        listener.onProgressUpdate(50);
        mIdleTimeCounter = AchievementSnow.Achievement7.IDLE_TIME_PASSED;
        Compacter currentStateData = getCurrentState();
        boolean devilVisible = DEFAULT_DEVIL_IS_VISIBLE;
        int devilState = Devil.STATE_PROTECT;
        if (currentStateData != null) {
            mReloadRiddleMoveBlockDuration = RELOAD_RIDDLE_BLOCK_DURATION;
            if (currentStateData.getSize() >= 6) {
                try {
                    if (mConfig.mWidth == currentStateData.getInt(0) && mConfig.mHeight == currentStateData.getInt(1)) {
                        initCell(res, currentStateData.getFloat(2), currentStateData.getFloat(3), currentStateData.getFloat(4));

                    }
                    devilState = currentStateData.getInt(5);
                    devilVisible = currentStateData.getBoolean(6);
                } catch (CompactedDataCorruptException e) {
                    currentStateData = null;
                }
            }
        }
        listener.onProgressUpdate(60);
        if (currentStateData == null) {
            mReloadRiddleMoveBlockDuration = 0L;

            initCell(res, mConfig.mWidth / 2.f, mConfig.mHeight / 2.f, 0.f);
            listener.onProgressUpdate(70);
        }

        initIdea(res);
        initDevil(res, devilState, devilVisible);
        nextIdea();

        int explosionSize = (int) (2 * mCell.mMaxRadius * SNOW_EXPLOSION_SIZE_MULTIPLIER);
        mFullExplosion = new Bitmap[5];
        mFullExplosion[0] = ImageUtil.CACHE.obtainImage(CACHE_FULL_EXPLOSION0, res, R.drawable.explosion1, explosionSize, explosionSize, false);
        mFullExplosion[1] = ImageUtil.CACHE.obtainImage(CACHE_FULL_EXPLOSION1, res, R.drawable.explosion2, explosionSize, explosionSize, false);
        mFullExplosion[2] = ImageUtil.CACHE.obtainImage(CACHE_FULL_EXPLOSION2, res, R.drawable.explosion3, explosionSize, explosionSize, false);
        mFullExplosion[3] = ImageUtil.CACHE.obtainImage(CACHE_FULL_EXPLOSION3, res, R.drawable.explosion4, explosionSize, explosionSize, false);
        mFullExplosion[4] = ImageUtil.CACHE.obtainImage(CACHE_FULL_EXPLOSION4, res, R.drawable.explosion5, explosionSize, explosionSize, false);
        mExplosionPaint = new Paint();
        mExplosionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        listener.onProgressUpdate(80);
        mExplosionHistoryX = new ArrayList<>();
        mExplosionHistoryY = new ArrayList<>();
        mExplosionHistorySize = new ArrayList<>();
        mExplosionHistoryType = new ArrayList<>();
        if (currentStateData != null) {
            for (int i = 8; i + 3 < currentStateData.getSize(); i+=4) {
                try {
                    drawExplosion(currentStateData.getFloat(i), currentStateData.getFloat(i + 1), currentStateData.getInt(i + 2), currentStateData.getInt(i + 3));
                } catch (CompactedDataCorruptException e) {
                    Log.e("Riddle", "Corrupt data when reconstructing snow: " + e);
                }
            }
        }

        listener.onProgressUpdate(100);
    }

    private void initCell(Resources res, float x, float y, float radius) {
        float maxRadius = (Math.min(mConfig.mWidth, mConfig.mHeight) / 2.f * SNOWBALL_SCREENSIZE_MAX_FRACTION);
        int dim = (int) (maxRadius * 2);
        Bitmap[] explosive = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.zelle_explosion_1, dim, dim, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.zelle_explosion_2, dim, dim, BitmapUtil.MODE_FIT_EXACT)};
        float startRadius = maxRadius * SNOWBALL_BASE_START_FRACTION;
        float currRadius = startRadius;
        if (radius >= startRadius) {
            currRadius = radius;
        }
        Bitmap maxCell = ImageUtil.loadBitmap(res, R.drawable.zelle, dim, dim, BitmapUtil.MODE_FIT_EXACT);
        mCell = Cell.make(x, y, currRadius, maxCell, explosive, startRadius, maxRadius);
        mWorld.addActor(mCell);
    }

    private void initIdea(Resources res) {
        float radius = ImageUtil.convertDpToPixel(IDEA_COLLECTION_RADIUS_BASE, mConfig.mScreenDensity);
        int size = 2 * (int) radius;
        Bitmap imageCandy = ImageUtil.loadBitmap(res, R.drawable.idea_candy, size, size, true);
        Bitmap imageToxic = ImageUtil.loadBitmap(res, R.drawable.idea_toxic, size, size, true);

        mIdea = makeIdea(0, 0, radius, imageCandy, imageToxic);
        mWorld.addActor(mIdea);
    }

    private void initDevil(Resources res, int state, boolean devilVisible) {
        float radius = mCell.mMaxRadius * DEVIL_RADIUS_FRACTION_OF_CELL_MAX_RADIUS;
        int size = (int) (2 * radius);
        Bitmap imageProtect = ImageUtil.loadBitmap(res, R.drawable.angel, size, size, true);
        Bitmap imageDamaged = ImageUtil.loadBitmap(res, R.drawable.angel_damaged, size, size, true);
        Bitmap imageRecovering = ImageUtil.loadBitmap(res, R.drawable.angel_recovering, size, size, true);
        Bitmap[] stateImages = new Bitmap[Devil.STATES_COUNT];
        stateImages[Devil.STATE_PROTECT] = imageProtect;
        stateImages[Devil.STATE_DAMAGED] = imageDamaged;
        stateImages[Devil.STATE_RECOVERING] = imageRecovering;
        mDevil = makeDevil(mCell, 0, 0, radius, stateImages, state, res);
        boolean wasSilent = mDevil.mSilent;
        mDevil.mSilent = true;
        if (devilVisible) {
            mDevil.onAppear();
        } else {
            mDevil.onLeaveWorld();
        }
        mWorld.addActor(mDevil);
        mDevil.mSilent = wasSilent;
    }

    private void nextIdea() {
        final int tries = 5;
        int count = 0;
        do {
            mWorld.setRandomPositionInside(mIdea, mRand);
            count++;
        } while (count < tries && mWorld.getCollider().checkCollision(mIdea.getHitbox(), mCell.getHitbox())); // try to get it in some distance, not too important
    }

    private void needForSpeed() {
        float cellX = mCell.getHitbox().getCenterX();
        float cellY = mCell.getHitbox().getCenterY();
        if (mFeatureTouchGravity && mTouchGravityIsPressed && (mTouchGravityPressX != cellX || mTouchGravityPressY != cellY)) {
            float gravityFraction = Math.min(1.0f, (System.currentTimeMillis() - mTouchGravityStartPressTime) / ((float) TOUCH_GRAVITY_FULL_EFFECT_DELAY));
            double angleBetweenTouchAndSnow = Math.atan2(cellY - mTouchGravityPressY, cellX - mTouchGravityPressX);
            float forceX = - gravityFraction * mGravity * (float) Math.cos(angleBetweenTouchAndSnow);
            float forceY = - gravityFraction * mGravity * (float) Math.sin(angleBetweenTouchAndSnow);
            mCell.updateFrictionAndAccel(forceX, forceY, 0.f);
        }
        updateSpeedAchievementData();
    }

    private void updateSpeedAchievementData() {
        AchievementDataRiddleType typeData = mConfig.mAchievementTypeData;
        if (typeData != null) {
            typeData.putValue(AchievementSnow.KEY_TYPE_MAX_SPEED, (long) mCell.getSpeed(), AchievementSnow.CELL_SPEED_REQUIRED_DELTA);
        }
    }

    private void drawExplosion(float explosionCenterX, float explosionCenterY, int explosionSize, int explosionType) {
        mExplosionHistoryX.add(explosionCenterX);
        mExplosionHistoryY.add(explosionCenterY);
        mExplosionHistorySize.add(explosionSize);
        mExplosionHistoryType.add(explosionType);
        Bitmap explosionImage;
        if (explosionSize >= 2 * mCell.mMaxRadius * SNOW_EXPLOSION_SIZE_MULTIPLIER) {
            // full explosion
            explosionImage = mFullExplosion[explosionType];
        } else {
            explosionImage = BitmapUtil.resize(mFullExplosion[explosionType], explosionSize, explosionSize);
        }
        mFogLayerCanvas.drawBitmap(explosionImage, explosionCenterX - explosionImage.getWidth() / 2.f, explosionCenterY - explosionImage.getHeight() / 2.f, mExplosionPaint);
        drawBackground();
    }

    private void drawBackground() {
        mBackgroundSnowCanvas.drawPaint(mClearPaint);
        mBackgroundSnowCanvas.drawBitmap(mBitmap, mRiddleOffsetX, mRiddleOffsetY, null);
        mBackgroundSnowCanvas.drawBitmap(mFogLayer, 0, 0, null);
    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updateTime) {
        mReloadRiddleMoveBlockDuration -= updateTime;
        if (mIdleTimeCounter > 0L) {
            mIdleTimeCounter -= updateTime;
        }
        if (mReloadRiddleMoveBlockDuration > 0L) {
            return; // wait some time after loading existing riddle so we don't crash immediately
        }
        if (mIdleTimeCounter <= 0L && mConfig.mAchievementGameData != null && mIdleTimeCounter != Long.MIN_VALUE) {
            mIdleTimeCounter = Long.MIN_VALUE;
            mConfig.mAchievementGameData.increment(AchievementSnow.Achievement7.KEY_IDLE_TIME_PASSED, 1L, 0L);
        }
        mWorld.update(updateTime);
        needForSpeed();
        if (mCell.updateAndCheckExplosionTimer(updateTime)) {
            onExplosion(false);
        }
        return;
    }

    @Override
    public boolean onOrientationEvent(float azimuth, float pitch, float roll) {
        if (mFeatureTouchGravity) {
            return false;
        }
        // forceX/Y in screen coordinate space
        float forceX = mGravity * (float) Math.sin(roll);
        float forceY = -mGravity * (float) Math.sin(pitch);
        mCell.updateFrictionAndAccel(forceX, forceY, 3.f / 4.f);
        return false; // we draw periodically and not on orientation event
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDevil.checkIfMocked(event.getX(), event.getY());
        }
        if (mFeatureTouchGravity && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mTouchGravityStartPressTime = System.currentTimeMillis();
            mTouchGravityPressX = event.getX();
            mTouchGravityPressY = event.getY();
            mTouchGravityIsPressed = true;
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_CLICKS_DOWN_DURING_NO_SENSOR, 1L, 0L);
            }
        } else if (mFeatureTouchGravity && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            mTouchGravityPressX = event.getX();
            mTouchGravityPressY = event.getY();
        } else if (mFeatureTouchGravity && event.getActionMasked() == MotionEvent.ACTION_UP) {
            mTouchGravityIsPressed = false;
            mCell.updateFrictionAndAccel(0, 0, 0);
        }
        return false;
    }

    private void setTouchControl(boolean enable) {
        mFeatureTouchGravity = enable;
    }

    public boolean requiresOrientationSensor() {
        return !mFeatureTouchGravity;
    }

    @Override
    public void enableNoOrientationSensorAlternative() {
        setTouchControl(true);
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        Compacter cmp = new Compacter();
        cmp.appendData(mConfig.mWidth);
        cmp.appendData(mConfig.mHeight);
        cmp.appendData(mCell.getHitbox().getCenterX());
        cmp.appendData(mCell.getHitbox().getCenterY());
        cmp.appendData(mCell.mHitboxCircle.getRadius());
        cmp.appendData(mDevil.mState);
        cmp.appendData(mDevil.isActive());
        cmp.appendData(""); // in case we need the slot
        Iterator<Float> xIt = mExplosionHistoryX.iterator();
        Iterator<Float> yIt = mExplosionHistoryY.iterator();
        Iterator<Integer> sizeIt = mExplosionHistorySize.iterator();
        Iterator<Integer> typeIt = mExplosionHistoryType.iterator();
        while (xIt.hasNext()) {
            cmp.appendData(xIt.next());
            cmp.appendData(yIt.next());
            cmp.appendData(sizeIt.next());
            cmp.appendData(typeIt.next());
        }
        return cmp.compact();
    }

    @Override
    protected void calculateGainedScore(int[] scores) {
        int wallCollisions = mConfig.mAchievementGameData != null ? mConfig.mAchievementGameData.getValue(AchievementSnow.KEY_GAME_COLLISION_COUNT, 0L).intValue() : 0;
        int bonus = (wallCollisions <= MAX_WALL_COLLISONS_FOR_SCORE_BONUS ? Types.SCORE_SIMPLE : 0);
        super.calculateGainedScore(scores);
        scores[3] += bonus;
        scores[0] += bonus;
    }

    @Override
    protected Bitmap makeSnapshot() {
        int width = SNAPSHOT_DIMENSION.getWidthForDensity(mConfig.mScreenDensity);
        int height = SNAPSHOT_DIMENSION.getHeightForDensity(mConfig.mScreenDensity);
        Bitmap snapshot = Bitmap.createScaledBitmap(mBitmap, width, height, false);
        Canvas canvas = new Canvas(snapshot);
        Bitmap overlay = Bitmap.createScaledBitmap(mBackgroundSnow, width, height, false);
        canvas.drawBitmap(overlay, 0, 0, null);
        float fractionX = width / (float) mConfig.mWidth;
        float fractionY = height / (float) mConfig.mHeight;
        RectF cellHitbox = mCell.getHitbox().getBoundingRect();
        Bitmap snow = Bitmap.createScaledBitmap(mCell.mMaxCell, (int) (fractionX * cellHitbox.width()),
                (int) (fractionY * cellHitbox.height()), false);
        canvas.drawBitmap(snow, (int) (cellHitbox.left * fractionX), (int) (cellHitbox.top * fractionY), null);
        return snapshot;
    }

    @Override
    protected void initAchievementData() {
        if (mConfig.mAchievementGameData != null) {
            long wasEnabled = mConfig.mAchievementGameData.getValue(AchievementSnow.KEY_GAME_FEATURE_ORIENTATION_SENSOR_ENABLED, -1L);
            Log.d("Riddle", "Was feature enabled: " + wasEnabled);
            mConfig.mAchievementGameData.putValue(AchievementSnow
                    .KEY_GAME_FEATURE_ORIENTATION_SENSOR_ENABLED, mFeatureTouchGravity ? 1L : 0L,
                    AchievementProperties.UPDATE_POLICY_ALWAYS);
            if (wasEnabled != -1L && ((wasEnabled == 0L) == mFeatureTouchGravity)) {
                Log.d("Riddle", "Feature changed.");
                mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_FEATURE_ORIENTATION_SENSOR_CHANGED, 1L, 0L);
            }
        }
    }

    @Override
    public void onReachedEndOfWorld(Actor columbus, float x, float y, int borderFlags) {
        boolean collisionLeft = (borderFlags & FlatRectWorld.BORDER_FLAG_LEFT) != 0;
        boolean collisionRight = (borderFlags & FlatRectWorld.BORDER_FLAG_RIGHT) != 0;
        boolean collisionTop = (borderFlags & FlatRectWorld.BORDER_FLAG_TOP) != 0;
        boolean collisionBottom = (borderFlags & FlatRectWorld.BORDER_FLAG_BOTTOM) != 0;
        if (columbus == mDevil) {
            mDevil.onTouchedOutside();
            return;
        }
        if (columbus != mCell) {
            return;
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValues(AchievementSnow.KEY_GAME_COLLISION_SPEED, (long) mCell.getSpeed(), AchievementProperties.UPDATE_POLICY_ALWAYS,
                    AchievementSnow.KEY_GAME_PRE_COLLISION_CELL_STATE, (long) mCell.getState(), AchievementProperties.UPDATE_POLICY_ALWAYS,
                    null, 0L, 0L);
        }
        mCell.applyWallPhysics(mWorld, collisionLeft, collisionTop, collisionRight, collisionBottom);
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_COLLISION_COUNT, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        onExplosion(true);
    }

    private void onExplosion(boolean hitWall) {
        float radius = mCell.mHitboxCircle.getRadius();
        float explosionX = mCell.getHitbox().getCenterX();
        float explosionY = mCell.getHitbox().getCenterY();
        if (mCell.onExplosion(hitWall, mDevil)) {
            updateCellStateAchievementData();

            int explosionSize = (int) (1 + SNOW_EXPLOSION_SIZE_MULTIPLIER * 2 * radius * (hitWall ? EXPLOSION_HIT_WALL_FRACTION : 1.f));
            int explosionIndex = mRand.nextInt(mFullExplosion.length);
            if (mConfig.mAchievementGameData != null) {
                if (hitWall) {
                    mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_WALL_EXPLOSION, 1L, 0L);
                } else {
                    mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_BIG_EXPLOSION, 1L, 0L);
                }
            }
            drawExplosion(explosionX, explosionY, explosionSize, explosionIndex);
            updateSpeedAchievementData();
        }
    }

    @Override
    public void onLeftWorld(Actor jesus, int borderFlags) {
        jesus.onLeaveWorld();
    }

    @Override
    public void onCollision(Actor colliding1, Actor colliding2) {
        if (checkCollisionPair(colliding1, colliding2, mCell, mIdea)) {
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_IDEAS_COLLECTED, 1, 0);
            }
            mCell.onCollectIdea();
            mDevil.onCellCollectIdea();
            updateCellStateAchievementData();
            nextIdea();
        } else if (checkCollisionPair(colliding1, colliding2, mDevil, mIdea)) {
            if (mDevil.attemptCollectIdea()) {
                if (mConfig.mAchievementGameData != null) {
                    mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_ANGEL_COLLECTED_IDEA, 1L, 0L);
                }
            }
        } else if (((colliding1 == mCell && colliding2.onCollision(mCell)))
                    || (colliding2 == mCell && colliding1.onCollision(mCell))) {
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementSnow.KEY_GAME_CELL_COLLECTED_SPORE, 1L, 0L);
            }
            updateCellStateAchievementData();
        } else if (((colliding1 == mDevil && colliding2.onCollision(mDevil)))
                || (colliding2 == mDevil && colliding1.onCollision(mDevil))) {
        }
    }

    private static boolean checkCollisionPair(Actor colliding1, Actor colliding2, Actor toCheck1, Actor toCheck2) {
        return (colliding1 == toCheck1 && colliding2 == toCheck2) || (colliding1 == toCheck2 && colliding2 == toCheck1);
    }

    private void updateCellStateAchievementData() {
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementSnow.KEY_GAME_CELL_STATE, (long) mCell.getState(), AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
    }

    @Override
    public void onMoverStateChange(Actor actor) {

    }

    public static class Cell extends Actor {
        public static final int STATE_NORMAL = -1;
        public static final int STATE_EXPLOSIVE = -2;
        private static final long FRAME_DURATION = 250L;
        private final HitboxNewtonFrictionMover mCellMover;
        private HitboxCircle mHitboxCircle;
        private final float mStartRadius;
        private final float mMaxRadius;
        private final float mStateRadiusDelta;
        private final Bitmap mMaxCell;
        private final Frames mCellLook;
        private long mExplosionCountDown;

        public Cell(HitboxCircle hitbox, HitboxNewtonFrictionMover mover, Frames cellLook, float startRadius, float maxRadius, Bitmap defaultMaxCell) {
            super(hitbox, mover, cellLook);
            mHitboxCircle = hitbox;
            mCellMover = mover;
            mStartRadius = startRadius;
            mMaxRadius = maxRadius;
            mStateRadiusDelta = (mMaxRadius - mStartRadius) / (float) IDEAS_REQUIRED_FOR_MAX_SIZE;
            mMaxCell = defaultMaxCell;
            mCellLook = cellLook;
            setActive(true);
        }

        public static Cell make(float x, float y, float radius, Bitmap defaultMaxCell, Bitmap[] explosionFrames, float startRadius, float maxRadius) {
            HitboxCircle hitbox = new HitboxCircle(x,y, radius);
            HitboxNewtonFrictionMover mover = new HitboxNewtonFrictionMover();
            Frames cellLook = Cell.makeCellFrames(null, radius, defaultMaxCell);
            Look explosionCellLook = new Frames(explosionFrames, FRAME_DURATION);
            Cell cell = new Cell(hitbox, mover, cellLook, startRadius, maxRadius, defaultMaxCell);
            cell.putStateFrames(STATE_NORMAL, cellLook);
            cell.putStateFrames(STATE_EXPLOSIVE, explosionCellLook);
            return cell;
        }

        private static Frames makeCellFrames(Frames base, float radius, Bitmap defaultMaxCell) {
            int size = (int) (radius * 2);
            Bitmap[] frames = base == null ? new Bitmap[1] : base.getFrames();
            if (frames[0] == null || frames[0].getWidth() != size) {
                frames[0] = BitmapUtil.resize(defaultMaxCell, size, size);
            }
            return base == null ? new Frames(frames, FRAME_DURATION) : base;
        }

        private float calculateFriction() {
            double fractionOfMaxSize = mHitboxCircle.getRadius()  / mMaxRadius;
            return (float) (0.55 * Math.exp(-1.5 * fractionOfMaxSize));
        }

        public void onCollectIdea() {
            boolean explosion = false;
            float radius = mHitboxCircle.getRadius();
            if (radius >= mMaxRadius) {
                radius = mMaxRadius;
                explosion = true;
            } else {
                radius += mStateRadiusDelta;
                if (radius >= mMaxRadius) {
                    radius = mMaxRadius;
                    explosion = true;
                }
                Cell.makeCellFrames(mCellLook, radius, mMaxCell);
            }
            mHitboxCircle.setRadius(radius);
            if (explosion && mExplosionCountDown == 0L) {
                setStateFrames(STATE_EXPLOSIVE);
                mExplosionCountDown = EXPLOSION_DELAY;
            }
        }

        private int getState() {
            if (mHitboxCircle.getRadius() >= mMaxRadius) {
                return IDEAS_REQUIRED_FOR_MAX_SIZE;
            }
            int state = Math.round((mHitboxCircle.getRadius() - mStartRadius) / mStateRadiusDelta);
            return Math.min(state, IDEAS_REQUIRED_FOR_MAX_SIZE - 1);
        }

        private double getSpeed() {
            return mCellMover.getSpeed();
        }

        public void updateFrictionAndAccel(float forceX, float forceY, float prevAccelFraction) {
            float friction =  calculateFriction();
            mCellMover.setFriction(friction);
            float accelX = (1-friction) * forceX;
            float accelY = (1-friction) * forceY;
            mCellMover.setAcceleration(accelX * (1.f - prevAccelFraction) + mCellMover.getAccelerationX() * prevAccelFraction,
                    accelY * (1.f - prevAccelFraction) + mCellMover.getAccelerationY() * prevAccelFraction);
        }

        public void applyWallPhysics(FlatRectWorld world, boolean collisionLeft, boolean collisionTop, boolean collisionRight, boolean collisionBottom) {

            float speedMultiplier = mHitboxCircle.getRadius() > mStartRadius ? -CRASHED_WALL_BIGGER_SPEED_MULTIPLIER : -CRASHED_WALL_SMALL_SPEED_MULTIPLIER;
            if (collisionLeft) {
                mHitboxCircle.setLeft(world.getLeft() + 1);
                mCellMover.multiplySpeed(speedMultiplier, 1.f);
            }
            if (collisionTop) {
                mHitboxCircle.setTop(world.getTop() + 1);
                mCellMover.multiplySpeed(1.f, speedMultiplier);
            }
            if (collisionRight) {
                mHitboxCircle.setRight(world.getRight() - 1);
                mCellMover.multiplySpeed(speedMultiplier, 1.f);
            }
            if (collisionBottom) {
                mHitboxCircle.setBottom(world.getBottom() - 1);
                mCellMover.multiplySpeed(1.f, speedMultiplier);
            }
        }

        private boolean updateAndCheckExplosionTimer(long updateTime) {
            if (mExplosionCountDown > 0) {
                mExplosionCountDown -= updateTime;
                return mHitboxCircle.getRadius() >= mMaxRadius && mExplosionCountDown <= 0;
            }
            return false;
        }

        private boolean onExplosion(boolean hitWall, Devil devil) {
            final float radius = mHitboxCircle.getRadius();
            if (radius > mStartRadius) {
                float delta;
                if (hitWall) {
                    if (devil.isActive()) {
                        delta = STATE_DELTA_ON_WALL_EXPLOSION * mStateRadiusDelta;
                    } else {
                        delta = radius - mStartRadius;
                    }
                } else {
                    devil.onAppear();
                    delta = radius - mStartRadius;
                    mCellMover.multiplySpeed(EXPLOSION_SPEED_MULTIPLIER, EXPLOSION_SPEED_MULTIPLIER);
                }
                return shrinkCell(delta);
            }
            return false;
        }

        private boolean shrinkCell(double shrinkDelta) {
            float radius = mHitboxCircle.getRadius();
            if (radius > mStartRadius) {
                radius -= shrinkDelta;
                if (radius < mStartRadius) {
                    radius = mStartRadius;
                }

                makeCellFrames(mCellLook, radius, mMaxCell);
                mHitboxCircle.setRadius(radius);
                mExplosionCountDown = 0L;
                setStateFrames(STATE_NORMAL);
                return true;
            }
            return false;
        }

        public boolean onCollectIdeaChild(Devil devil) {
            return !(devil.isActive() && devil.mState == Devil.STATE_PROTECT) && shrinkCell(mStateRadiusDelta * (devil.isActive() ? STATE_DELTA_ON_IDEA_CHILD_COLLECT_WITH_ACTIVE_DEVIL : STATE_DELTA_ON_IDEA_CHILD_COLLECT));
        }
    }

    private Idea makeIdea(float x, float y, float radius, Bitmap candy, Bitmap toxic) {
        HitboxCircle hitbox = new HitboxCircle(x, y, radius);
        Look candyLook = new BitmapLook(candy);
        Look toxicLook = new BitmapLook(toxic);
        return new Idea(hitbox, candyLook, toxicLook);
    }

    private class Idea extends Actor {

        private static final int STATE_CANDY = 0;
        private static final int STATE_TOXIC = 1;
        private static final int MAX_CHILDREN = 40;
        private static final double SPAWNS_PER_SECOND = 2.;
        private List<IdeaChild> mChildren = new LinkedList<>();
        private Random mRand = new Random();

        public Idea(HitboxCircle hitbox, Look candyLook, Look toxicLook) {
            super(hitbox, HitboxNoMover.INSTANCE, candyLook);
            putStateFrames(STATE_CANDY, candyLook);
            putStateFrames(STATE_TOXIC, toxicLook);
            for (int i = 0; i < MAX_CHILDREN; i++) {
                IdeaChild child = makeIdeaChild(this);
                mChildren.add(child);
                mWorld.addActor(child);
            }
            setActive(true);
        }

        @Override
        public boolean update(long updatePeriod) {
            boolean result = super.update(updatePeriod);
            if (mRand.nextDouble() < updatePeriod / 1000. * SPAWNS_PER_SECOND) {
                for (IdeaChild child : mChildren) {
                    if (!child.isActive()) {
                        child.prepare(mRand);
                        child.setActive(true);
                        break;
                    }
                }
            }
            return result;
        }
    }

    private IdeaChild makeIdeaChild(Idea parent) {
        RectF parentBounds = parent.getHitbox().getBoundingRect();
        float parentHalfWidth = parentBounds.width() / 2.f;
        float radius = parentHalfWidth * IdeaChild.PARENT_RADIUS_FRACTION;
        Hitbox hitbox = new HitboxCircle(parentBounds.centerX(), parentBounds.centerY(), radius);
        HitboxNewtonFrictionMover mover = new HitboxNewtonFrictionMover();
        return new IdeaChild(hitbox, mover, new CircleLook(radius, CHILD_COLORS[mRand.nextInt(CHILD_COLORS.length)]));
    }

    private static final int[] CHILD_COLORS = new int[] {0xfff10000, 0xff06a928, 0xffff9600, 0xff7b00f9, 0xffd6f400, 0xff009071};
    private class IdeaChild extends Actor {
        private static final float FRICTION = 0.5f;
        private static final float PARENT_RADIUS_FRACTION = 0.20f;
        private final HitboxNewtonFrictionMover mMover;

        public IdeaChild(Hitbox hitbox, HitboxNewtonFrictionMover mover, Look defaultLook) {
            super(hitbox, mover, defaultLook);
            mMover = mover;
        }

        @Override
        public void onLeaveWorld() {
            setActive(false);
        }

        @Override
        public boolean onCollision(Actor with) {
            if (with == mCell) {
                setActive(false);
                return mCell.onCollectIdeaChild(mDevil);
            } else if (with == mDevil) {
                mDevil.attemptCollectIdeaChild(this);
                return true;
            }
            return false;
        }

        private void prepare(Random rand) {
            RectF parentBounds = mIdea.getHitbox().getBoundingRect();
            float parentHalfWidth = parentBounds.width() / 2.f;
            float angle = rand.nextFloat() * (float) Math.PI * 2;
            float cosAngle = (float) Math.cos(angle);
            float sinAngle = (float) Math.sin(angle);
            getHitbox().setCenter(parentBounds.centerX() + parentHalfWidth * cosAngle,
                    parentBounds.centerY() + parentHalfWidth * sinAngle);
            float outspeed = Math.min(mWorld.getWidth(), mWorld.getHeight()) * 0.3f;
            mMover.setSpeed(outspeed * cosAngle, outspeed* sinAngle);
            mMover.setFriction(FRICTION * rand.nextFloat());
        }
    }

    private Devil makeDevil(Cell cell, float x, float y, float radius, Bitmap[] stateImages, int state, Resources res) {
        HitboxCircle hitbox = new HitboxCircle(x, y, radius);
        Look[] looks = new Look[stateImages.length];
        for (int i = 0; i < stateImages.length; i++) {
            looks[i] = new BitmapLook(stateImages[i]);
        }
        HitboxMoonMover moonMover = new HitboxMoonMover(cell.getHitbox(), 1, ImageUtil.convertDpToPixel(7.f, mConfig.mScreenDensity));
        return new Devil(hitbox, moonMover, state, looks, res);
    }

    private static final long[] SURROUND_DURATION = new long[] {1900L, 2000L, 3000L};
    private class Devil extends Actor {
        private static final long TOUCH_OUTSIDE_LOCK_DURATION = 250L;
        private static final long RECOVER_DURATION = 20000L;
        private static final int STATE_PROTECT = 0;
        private static final int STATE_DAMAGED = 1;
        private static final int STATE_RECOVERING = 2;
        private static final int STATES_COUNT = 3;

        private final Resources mRes;
        private int mState;
        private long mLastOutsideTouch;
        private HitboxMoonMover mMoonMover;
        private NinePatchLook[] mTalkingBackground;
        private WorldEffect mTalkingEffect;
        private boolean mSilent;
        private long mTimeToRecover;

        public Devil(HitboxCircle hitbox, HitboxMoonMover moonMover, int state, Look[] stateLooks, Resources res) {
            super(hitbox, moonMover, stateLooks[state]);
            for (int i = 0; i < STATES_COUNT; i++) {
                putStateFrames(i, stateLooks[i]);
            }
            mMoonMover = moonMover;
            mMoonMover.setMoonYear(SURROUND_DURATION[state]);
            mState = state;
            mRes = res;
            mTalkingBackground = new NinePatchLook[] {
                    new NinePatchLook(NinePatchLook.loadNinePatch(res, R.drawable.say_tl), mConfig.mScreenDensity),
                    new NinePatchLook(NinePatchLook.loadNinePatch(res, R.drawable.say_tr), mConfig.mScreenDensity),
                    new NinePatchLook(NinePatchLook.loadNinePatch(res, R.drawable.say_br), mConfig.mScreenDensity),
                    new NinePatchLook(NinePatchLook.loadNinePatch(res, R.drawable.say_bl), mConfig.mScreenDensity)};
            setActive(true);
            updateCellCandyVision();
            moonMover.update(getHitbox(), 0L);
        }

        private void updateCellCandyVision() {
            if (isActive()) {
                mIdea.setStateFrames(Idea.STATE_TOXIC);
            } else {
                mIdea.setStateFrames(Idea.STATE_CANDY);
            }
        }

        private void recover() {
            if (mState == STATE_PROTECT) {
                return;
            }
            mState = STATE_PROTECT;
            setStateFrames(mState);
            mMoonMover.setMoonYear(SURROUND_DURATION[mState]);
            talk(R.array.devil_talk_recovered, 0.7);
        }

        @Override
        public boolean update(long updateTime) {
            boolean result = super.update(updateTime);
            if (mState == STATE_RECOVERING) {
                mTimeToRecover -= updateTime;
                if (mTimeToRecover <= 0) {
                    recover();
                }
            }
            return result;
        }

        public void onTouchedOutside() {
            if (mState == STATE_RECOVERING) {
                return; // ignore
            }
            if (System.currentTimeMillis() - mLastOutsideTouch >= TOUCH_OUTSIDE_LOCK_DURATION) {
                mLastOutsideTouch = 0L;
            }
            if (mLastOutsideTouch == 0L) {
                mLastOutsideTouch = System.currentTimeMillis();
                mMoonMover.invertDirection();
            }
        }

        public void talk(int textId, double probability) {
            if (!mSilent && (mTalkingEffect == null || mTalkingEffect.getState() == WorldEffect.STATE_TIMEOUT)
                    && mRand.nextDouble() < probability) {
                String[] texts = mRes.getStringArray(textId);
                mTalkingEffect = mWorld.attachTimedMessage(this, mTalkingBackground, texts[mRand.nextInt(texts.length)], 5000L);
                mTalkingEffect.startFade(0xFFFFFFFF, 0x00FFFFFF, 2000L, 3000L);
            }
        }

        public boolean attemptCollectIdea() {
            if (mState < STATE_RECOVERING) {
                mState++;
                setStateFrames(mState);
                mMoonMover.setMoonYear(SURROUND_DURATION[mState]);
                nextIdea();
                if (mState == STATE_RECOVERING) {
                    mTimeToRecover = RECOVER_DURATION;
                    talk(R.array.devil_talk_recovering_start, 0.5);
                }
                return true;
            }
            return false;
        }

        public void attemptCollectIdeaChild(IdeaChild toCollect) {
            if (mState < STATE_RECOVERING) {
                toCollect.setActive(false);
            }
        }

        @Override
        public void onLeaveWorld() {
            setActive(false);
            talk(R.array.devil_talk_killed, 1.0);
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.putValue(AchievementSnow.KEY_GAME_DEVIL_VISIBLE_STATE, 0L, AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
            updateCellCandyVision();
        }

        public void onAppear() {
            setActive(true);
            recover();
            talk(R.array.devil_talk_return, 1.0);
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.putValue(AchievementSnow.KEY_GAME_DEVIL_VISIBLE_STATE, 1L, AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
            updateCellCandyVision();
        }

        public void checkIfMocked(float x, float y) {
            if (isActive() && mDevil.getHitbox().isInside(x, y)) {
                mDevil.talk(R.array.devil_talk_mock, 0.02);
            }
        }

        public void onCellCollectIdea() {
            if (mDevil.isActive()) {
                if (mCell.getState() == IDEAS_REQUIRED_FOR_MAX_SIZE) {
                    mDevil.talk(R.array.devil_talk_cell_eat_candy_explosive, 1.);
                } else {
                    mDevil.talk(R.array.devil_talk_cell_eat_candy, 1.0 / IDEAS_REQUIRED_FOR_MAX_SIZE);
                }
            }
        }
    }
}
