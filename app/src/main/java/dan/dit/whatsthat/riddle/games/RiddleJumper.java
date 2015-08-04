package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementJumper;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.flatworld.collision.GeneralHitboxCollider;
import dan.dit.whatsthat.util.flatworld.collision.HitboxRect;
import dan.dit.whatsthat.util.flatworld.look.Frames;
import dan.dit.whatsthat.util.flatworld.look.FramesOneshot;
import dan.dit.whatsthat.util.flatworld.look.LayerFrames;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxJumpMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNewtonMover;
import dan.dit.whatsthat.util.flatworld.world.Actor;
import dan.dit.whatsthat.util.flatworld.world.FlatRectWorld;
import dan.dit.whatsthat.util.flatworld.world.FlatWorldCallback;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 10.05.15.
 */
public class RiddleJumper extends RiddleGame implements FlatWorldCallback {
    //fixed constants //TODO memory leaks? Test if also GC interrupts when paused
    private static final float RELATIVE_HEIGHT_BASELINE = 1.f;
    private static final long ONE_SECOND = 1000L;
    private static final long UPDATE_PERIOD = 16L;
    private static final long PSEUDO_RUN_SPEED = 1;

    //variable constants
    private static final long BACKGROUND_SLIDE_DURATION = 30000L; //ms
    private static final long FRAME_DURATION = 125L; //ms
    private static final float RUNNER_LEFT_OFFSET = 10.f; // pixel
    private static final float JUMP_RELATIVE_HEIGHT_DELTA = 1.25f;
    private static final float DOUBLE_JUMP_RELATIVE_HEIGHT_DELTA = 1.8f;
    private static final long JUMP_DURATION = 500L; //ms, time required to perform the normal jump: reaching the peak and landing again.
    private static final long DOUBLE_JUMP_REST_DURATION = (long) ((2 * DOUBLE_JUMP_RELATIVE_HEIGHT_DELTA - JUMP_RELATIVE_HEIGHT_DELTA) * JUMP_DURATION / 2); //ms, time required from normal jump peak to full peak and landing again
    private static final float OBSTACLE_RELATIVE_HEIGHT_SMALL = 0.7f;
    private static final float OBSTACLE_RELATIVE_HEIGHT_FLYING = 0.4f;
    private static final float OBSTACLE_RELATIVE_HEIGHT_BIG = 1.1f;
    private static final long OBSTACLES_RIGHT_LEFT_DURATION = 1100L; //ms, describes the speed the obstacles travel from right to left
    private static final long FLYER_FALL_DURATION = OBSTACLES_RIGHT_LEFT_DURATION; //ms, describes the speed the flying falls to bottom
    private static final long FIRST_OBSTACLE_DURATION = 3000L; //ms, delay until the first obstacle appears

    private static final int REQUIRED_TOTAL_OBSTACLES = 400; // obstacles score passed till totally visible
    private static final int DIFFICULTIES = 4;
    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;
    public static final int DIFFICULTY_ULTRA = 3;
    private static final int[] DISTANCE_RUN_THRESHOLDS = new int[] {0, (int) (150 * ONE_SECOND), (int) (400 * ONE_SECOND), (int) (800 * ONE_SECOND)};
    private static final long[] NEXT_OBSTACLE_MIN_TIME_SMALL = new long[] {1000L, 775L, 600L, 600L, 550L};
    private static final long[] NEXT_OBSTACLE_MIN_TIME_BIG = new long[] {1000L, 1200L, 1100L, 1050L, 925L};
    private static final long[] NEXT_OBSTACLE_MAX_TIME = new long[] {2000L, 1700L, 1400L, 1250L, 1150L}; //ms, maximum time until the next obstacle appears
    private static final long[] NEXT_OBSTACLE_MIN_TIME_SMALL_WIDTH = new long[] {1200L, 1100L, 1100L, 1050L, 900L};
    private static final double NEXT_OBSTACLE_PREVIOUS_MIN_TIME_WEIGHT = 0.7;
    private static final int[] DIFFICULTY_COLORS = new int[] {Color.GREEN, Color.YELLOW, Color.RED, Color.WHITE, Color.CYAN};
    private static final int EASY_SMALL_OBSTACLES = 6;
    private static final int MEDIUM_SMALL_OBSTACLES = 4;
    private static final int HARD_BIG_OBSTACLES = 4;
    private static final int ULTRA_OBSTACLES = 3;
    private static final float BUBBLE_SCALE = 0.8f;
    private static final float MAX_SOLUTION_SCALE = 0.5f;
    private static final float BUBBLE_CENTER_Y_ESTIMATE = 0.765f * 0.5f;
    private static final float DISTANCE_RUN_PENALTY_ON_SAVE_FRACTION = 0.75f; // mainly required so that it is not worth closing the riddle (or app) when you know you are going to collide


    private Bitmap mThoughtbubble;
    private Bitmap mSolutionBackground;
    private Canvas mSolutionBackgroundCanvas;
    private Paint mClearPaint;
    private int mSolutionBackgroundHeight;
    private FlatRectWorld mWorld;
    private Runner mRunner;
    private int mRunnerHeight;
    private Bitmap mRunnerBackground;
    private Canvas mRunnerBackgroundCanvas;
    private Rect mRunnerBackgroundRect;
    private Rect mRunnerBackgroundRectDest;
    private Bitmap mBackgroundImage;
    private int mRunnerBackgroundSeparatorX;
    private long mBackgroundSlideCounter;
    private List<Obstacle> mCurrentObstacles;
    private Bitmap mForeground;
    private Canvas mForegroundCanvas;
    private long mNextObstacleCounter;
    private Random mRand;
    private boolean mCollisionBreak;
    private int mObstaclesPassed;
    private Obstacle mNextObstacle;
    private float mDistanceRun;
    private boolean mValidDistanceRun;
    private Paint mTextPaint;
    private List<List<Obstacle>> mObstacles;
    private int mDifficulty;
    private long mDifficultyMinTimeExtra;
    private Paint mSolutionPaint;
    private Paint mGradientPaint;
    private boolean mStateMotionIsDown;
    private boolean mFlagDoSuperJump;
    private float mObstaclesSpeed;
    private Obstacle mBoss;
    private float mLastDrawnSolutionFraction;
    private float mLastDrawnPassedFraction;
    private Bitmap mScaledSolution;

    public RiddleJumper(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
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
    public synchronized void draw(Canvas canvas) {
        canvas.drawBitmap(mRunnerBackground, 0, 0, null);
        canvas.drawBitmap(mSolutionBackground, 0, 0, null);
        canvas.drawBitmap(mForeground, 0, 0, null);
        canvas.drawText(Integer.toString(distanceRunToMeters(mDistanceRun)) + "m", canvas.getWidth() / 2.f, 35.f, mTextPaint);
    }

    public static int distanceRunToMeters(float distanceRun) {
        return (int) (distanceRun / ONE_SECOND);
    }

    public static float meterToDistanceRun(int meter) {
        return meter * ONE_SECOND;
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mRand = new Random();
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mTextPaint = new Paint();
        mTextPaint.setTextSize(35.f);
        mSolutionBackgroundHeight = (int) (mConfig.mHeight / Types.Jumper.BITMAP_ASPECT_RATIO);
        mSolutionBackground = Bitmap.createBitmap(mConfig.mWidth, mSolutionBackgroundHeight, Bitmap.Config.ARGB_8888);
        mSolutionBackgroundCanvas = new Canvas(mSolutionBackground);
        listener.onProgressUpdate(20);
        mObstaclesSpeed = - mConfig.mWidth / ((float) OBSTACLES_RIGHT_LEFT_DURATION / ONE_SECOND);
        mSolutionPaint = new Paint();
        mSolutionPaint.setAntiAlias(true);
        mSolutionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mGradientPaint = new Paint();
        mGradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mThoughtbubble = ImageUtil.loadBitmap(res, R.drawable.gedankenblase, (int) (BUBBLE_SCALE * mConfig.mWidth), (int) (BUBBLE_SCALE * mSolutionBackgroundHeight), true);
        listener.onProgressUpdate(30);
        Compacter cmp = getCurrentState();
        initDistanceRun(cmp);
        if (cmp != null && cmp.getSize() > 0) {
            try {
                mBackgroundSlideCounter = cmp.getLong(0);
            } catch (CompactedDataCorruptException e) {
                mBackgroundSlideCounter = 0L; // not very important data
            }
        }
        listener.onProgressUpdate(40);
        mForeground = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, Bitmap.Config.ARGB_8888);
        mWorld = new FlatRectWorld(new RectF(0, 0, mForeground.getWidth(), mForeground.getHeight()), new GeneralHitboxCollider(), this);
        mForegroundCanvas = new Canvas(mForeground);
        listener.onProgressUpdate(60);
        initRunner(res);
        listener.onProgressUpdate(65);
        initObstacles(res, cmp, listener);
        listener.onProgressUpdate(90);
        updateSolution(true);
        listener.onProgressUpdate(95);
        drawForeground();
        listener.onProgressUpdate(100);
    }

    private void initObstacles(Resources res, Compacter data, PercentProgressListener listener) {
        mObstaclesPassed = 0;
        if (data != null && data.getSize() > 1) {
            try {
                mObstaclesPassed = data.getInt(1);
            } catch (CompactedDataCorruptException e) {
                // default data is already set
            }
        }
        mCurrentObstacles = new ArrayList<>();
        mObstacles = new ArrayList<>();
        for (int i = 0; i < DIFFICULTIES; i++) {
            mObstacles.add(new ArrayList<Obstacle>());
        }
        listener.onProgressUpdate(70);
        initEasy(res);
        listener.onProgressUpdate(75);
        initMedium(res);
        listener.onProgressUpdate(80);
        initHard(res);
        listener.onProgressUpdate(85);
        initUltra(res);
        mNextObstacleCounter = FIRST_OBSTACLE_DURATION;
    }

    private void initEasy(Resources res) {
        int difficulty = DIFFICULTY_EASY;
        int width = mConfig.mWidth;
        int heightSmall = (int) (OBSTACLE_RELATIVE_HEIGHT_SMALL * mRunnerHeight);
        Bitmap[] monsterFeuer = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterfeuer1, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterfeuer2, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterfeuer3, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterfeuer4, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterfeuer5, width, heightSmall, false)};
        Bitmap[] monsterTeufel = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterteufel1, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterteufel2, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterteufel3, width, heightSmall, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterteufel4, width, heightSmall, false)};
        List<Obstacle> obstacles = mObstacles.get(difficulty);
        int startCount = obstacles.size();
        while (obstacles.size() - startCount < EASY_SMALL_OBSTACLES) {
            FramesOneshot feuerFrames = new FramesOneshot(monsterFeuer, (long) ((0.4 + mRand.nextDouble() * 0.6) *  OBSTACLES_RIGHT_LEFT_DURATION) );
            feuerFrames.setEndFrameIndex(monsterFeuer.length - 1);
            obstacles.add(Obstacle.makeObstacle(feuerFrames, 0.9f, 0.85f, mConfig.mWidth,
                    getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_SMALL), NEXT_OBSTACLE_MIN_TIME_SMALL, mWorld, mObstaclesSpeed, 0));
            Look teufelFrames = new Frames(monsterTeufel, FRAME_DURATION);
            obstacles.add(Obstacle.makeObstacle(teufelFrames, 0.75f, 0.95f, mConfig.mWidth,
                    getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_SMALL), NEXT_OBSTACLE_MIN_TIME_SMALL, mWorld, mObstaclesSpeed, 0));
        }
    }

    private void initMedium(Resources res) {
        int difficulty = DIFFICULTY_MEDIUM;
        int width = mConfig.mWidth;
        int heightSmall = (int) (OBSTACLE_RELATIVE_HEIGHT_SMALL * mRunnerHeight);
        int heightFlying = (int) (OBSTACLE_RELATIVE_HEIGHT_FLYING * mRunnerHeight);
        Bitmap[] monsterUfoMedium = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterufomedium1, width, heightSmall, false)};
        Bitmap[] flieger = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterflying1, width, heightFlying, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterflying2, width, heightFlying, false)};
        float fliegerTop = getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_FLYING) - 1.2f * mRunnerHeight;
        float fliegerFallSpeed =  -(fliegerTop - getTopForRelativeHeight(0) + flieger[0].getHeight()) / ((float) FLYER_FALL_DURATION / ONE_SECOND);

                List<Obstacle > obstacles = mObstacles.get(difficulty);
        obstacles.addAll(mObstacles.get(DIFFICULTY_EASY));
        int startCount = obstacles.size();
        while (obstacles.size() - startCount < MEDIUM_SMALL_OBSTACLES) {
            Look ufoMediumFrames = new Frames(monsterUfoMedium, FRAME_DURATION);
            obstacles.add(Obstacle.makeObstacle(ufoMediumFrames, 0.7f, 0.9f, mConfig.mWidth,
                    getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_SMALL), NEXT_OBSTACLE_MIN_TIME_SMALL, mWorld, mObstaclesSpeed, 0));
            Look fliegerLook = new Frames(flieger, FRAME_DURATION);
            obstacles.add(Obstacle.makeObstacle(fliegerLook, 0.8f, 0.9f, mConfig.mWidth,
                    fliegerTop, NEXT_OBSTACLE_MIN_TIME_SMALL, mWorld, mObstaclesSpeed, fliegerFallSpeed));
        }
        Look fliegerLook = new Frames(flieger, FRAME_DURATION);
        obstacles.add(Obstacle.makeObstacle(fliegerLook, 0.8f, 0.9f, mConfig.mWidth,
                fliegerTop, NEXT_OBSTACLE_MIN_TIME_SMALL, mWorld, mObstaclesSpeed, 0));

    }

    private void initHard(Resources res) {
        int difficulty = DIFFICULTY_HARD;
        int width = mConfig.mWidth;
        int heightBig = (int) (OBSTACLE_RELATIVE_HEIGHT_BIG * mRunnerHeight);
        Bitmap[] monsterWind = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterwind1, width, heightBig, false),
                ImageUtil.loadBitmap(res, R.drawable.monsterwind2, width, heightBig, false)};
        mObstacles.get(difficulty).addAll(mObstacles.get(DIFFICULTY_MEDIUM));
        List<Obstacle> obstacles = mObstacles.get(difficulty);
        int startCount = obstacles.size();
        while (obstacles.size() - startCount < HARD_BIG_OBSTACLES) {
            Look windFrames = new Frames(monsterWind, FRAME_DURATION);
            obstacles.add(Obstacle.makeObstacle(windFrames, 0.8f, 0.85f,
                    mConfig.mWidth, getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_BIG), NEXT_OBSTACLE_MIN_TIME_BIG, mWorld, mObstaclesSpeed, 0));
        }
    }

    private void initUltra(Resources res) {
        int difficulty = DIFFICULTY_ULTRA;
        int width = mConfig.mWidth;
        int heightSmall = (int) (OBSTACLE_RELATIVE_HEIGHT_SMALL * mRunnerHeight);
        Bitmap[] monsterUfoHard = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monsterufohard1, width, heightSmall, false)};
        int heightBig = (int) (OBSTACLE_RELATIVE_HEIGHT_BIG * mRunnerHeight);
        Bitmap[] crazyBig = new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.monstercrazy1, width, heightBig, false),
                ImageUtil.loadBitmap(res, R.drawable.monstercrazy2, width, heightBig, false),
                ImageUtil.loadBitmap(res, R.drawable.monstercrazy3, width, heightBig, false),
                ImageUtil.loadBitmap(res, R.drawable.monstercrazy4, width, heightBig, false),
                ImageUtil.loadBitmap(res, R.drawable.monstercrazy5, width, heightBig, false)};
        Bitmap bigBeam = ImageUtil.loadBitmap(res, R.drawable.monstercrazy6, width, mForeground.getHeight(), false);
        mObstacles.get(difficulty).addAll(mObstacles.get(DIFFICULTY_HARD));
        List<Obstacle> obstacles = mObstacles.get(difficulty);
        int startCount = obstacles.size();
        while (obstacles.size() - startCount < ULTRA_OBSTACLES - 1) {
            Look ufoHardFrames = new Frames(monsterUfoHard, FRAME_DURATION);
            obstacles.add(Obstacle.makeObstacle(ufoHardFrames, 0.9f, 0.7f,
                    mConfig.mWidth, getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_SMALL), NEXT_OBSTACLE_MIN_TIME_SMALL_WIDTH, mWorld, mObstaclesSpeed, 0));

        }
        LayerFrames crazyFrames = new LayerFrames(crazyBig, OBSTACLES_RIGHT_LEFT_DURATION / (crazyBig.length - 1), 1);
        crazyFrames.setBackgroundLayerBitmap(4, 0, bigBeam);
        mBoss = Obstacle.makeObstacle(crazyFrames, 0.7f, 0.85f,
                mConfig.mWidth, getTopForRelativeHeight(OBSTACLE_RELATIVE_HEIGHT_BIG), NEXT_OBSTACLE_MIN_TIME_BIG, mWorld, mObstaclesSpeed, 0);
        obstacles.add(mBoss);
    }

    private void initRunner(Resources res) {
        mRunnerHeight = mConfig.mHeight - mSolutionBackgroundHeight;

        Bitmap fallingImage = ImageUtil.loadBitmap(res, R.drawable.schritt1, mConfig.mWidth, mRunnerHeight, false);
        Look runnerFramesRun = new Frames(new Bitmap[] {
            fallingImage,
            ImageUtil.loadBitmap(res, R.drawable.schritt2, mConfig.mWidth, mRunnerHeight, false),
            ImageUtil.loadBitmap(res, R.drawable.schritt3, mConfig.mWidth, mRunnerHeight, false),
            ImageUtil.loadBitmap(res, R.drawable.schritt4, mConfig.mWidth, mRunnerHeight, false)}, FRAME_DURATION);
        Look runnerFramesJumpUp = new Frames(new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.schritt5, mConfig.mWidth, mRunnerHeight, false)}, FRAME_DURATION);
        Look runnerFramesJumpDown = new Frames(new Bitmap[] {
                fallingImage}, FRAME_DURATION);
        Look runnerFramesCollision = new Frames(new Bitmap[] {
                ImageUtil.loadBitmap(res, R.drawable.schritt6, mConfig.mWidth, mRunnerHeight, false)}, FRAME_DURATION);
        mRunner = Runner.makeRunner(runnerFramesRun, 0.5f, 0.8f, RUNNER_LEFT_OFFSET, getTopForRelativeHeight(RELATIVE_HEIGHT_BASELINE), mWorld);
        mRunner.putStateFrames(HitboxJumpMover.STATE_JUMP_ASCENDING, runnerFramesJumpUp);
        mRunner.putStateFrames(HitboxJumpMover.STATE_JUMP_FALLING, runnerFramesJumpDown);
        mRunner.putStateFrames(HitboxJumpMover.STATE_NOT_MOVING, runnerFramesRun);
        mRunner.putStateFrames(HitboxJumpMover.STATE_LANDED, runnerFramesRun);
        mRunner.putStateFrames(Runner.STATE_COLLISION, runnerFramesCollision);

        //background related
        mRunnerBackground = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, mBitmap.getConfig());
        mBackgroundImage = ImageUtil.loadBitmap(res, R.drawable.skyline, mRunnerBackground.getWidth(), mRunnerBackground.getHeight(), true);
        mRunnerBackgroundCanvas = new Canvas(mRunnerBackground);
        mRunnerBackgroundRect = new Rect();
        mRunnerBackgroundRectDest = new Rect();
        mRunnerBackgroundSeparatorX = 0;
        updateRunnerBackground();
    }

    private void updateRunnerBackground() {
        if (mRunnerBackgroundSeparatorX < mBackgroundImage.getWidth()) {
            mRunnerBackgroundRect.set(mRunnerBackgroundSeparatorX, 0, mBackgroundImage.getWidth(), mBackgroundImage.getHeight());
            mRunnerBackgroundRectDest.set(0, 0, mBackgroundImage.getWidth() - mRunnerBackgroundSeparatorX, mBackgroundImage.getHeight());
            mRunnerBackgroundCanvas.drawBitmap(mBackgroundImage, mRunnerBackgroundRect, mRunnerBackgroundRectDest, null);
        }
        int missingWidth = mRunnerBackgroundSeparatorX;
        mRunnerBackgroundRect.set(0, 0, missingWidth, mBackgroundImage.getHeight());
        mRunnerBackgroundRectDest.set(mBackgroundImage.getWidth() - missingWidth, 0, mBackgroundImage.getWidth(), mBackgroundImage.getHeight());
        mRunnerBackgroundCanvas.drawBitmap(mBackgroundImage, mRunnerBackgroundRect, mRunnerBackgroundRectDest, null);
    }

    private boolean onBackgroundUpdate() {
        mBackgroundSlideCounter += UPDATE_PERIOD;
        if (mBackgroundSlideCounter >= BACKGROUND_SLIDE_DURATION) {
            mBackgroundSlideCounter -= BACKGROUND_SLIDE_DURATION;
        }
        int oldX = mRunnerBackgroundSeparatorX;
        mRunnerBackgroundSeparatorX = (int) (mBackgroundImage.getWidth() * (mBackgroundSlideCounter / (float) BACKGROUND_SLIDE_DURATION));
        boolean drawBackground = oldX != mRunnerBackgroundSeparatorX;
        if (drawBackground) {
            updateRunnerBackground();
        }
        return drawBackground;
    }

    private synchronized void drawForeground() {
        mForegroundCanvas.drawPaint(mClearPaint);
        mWorld.draw(mForegroundCanvas, null);
    }

    private float getTopForRelativeHeight(float relativeHeight) {
        return mConfig.mHeight - mRunnerHeight * relativeHeight;
    }

    private synchronized void drawSolution(final float fraction, final boolean forceDraw) {
        final float REDRAW_THRESHOLD_FACTOR = 0.01f;
        final boolean fractionRedrawRequired = Math.abs(fraction - mLastDrawnSolutionFraction) >= REDRAW_THRESHOLD_FACTOR;
        final float passedFraction =  mObstaclesPassed / (float) REQUIRED_TOTAL_OBSTACLES;
        final boolean passedFractionRedrawRequired = Math.abs(passedFraction - mLastDrawnPassedFraction) >= REDRAW_THRESHOLD_FACTOR;
        if (!fractionRedrawRequired && !passedFractionRedrawRequired && !forceDraw) {
            return;
        }
        mSolutionBackgroundCanvas.drawPaint(mClearPaint);
        mSolutionBackgroundCanvas.drawBitmap(mThoughtbubble, (mSolutionBackground.getWidth() - mThoughtbubble.getWidth()) / 2, (mSolutionBackground.getHeight() - mThoughtbubble.getHeight()) / 2, null);

        if (passedFraction < 1.f) {
            int lineY = (int) (mSolutionBackgroundCanvas.getHeight() * passedFraction);
            if (forceDraw || passedFractionRedrawRequired) {
                mGradientPaint.setShader(new LinearGradient(0, mSolutionBackgroundCanvas.getHeight(), 0, lineY, Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP));
            }
            mSolutionBackgroundCanvas.drawPaint(mGradientPaint);
            int greyScale = (int) (passedFraction * 255.f);
            mSolutionPaint.setColor(Color.rgb(greyScale, greyScale, greyScale));
            mSolutionBackgroundCanvas.drawLine(0, lineY, mSolutionBackgroundCanvas.getWidth(), lineY, mSolutionPaint);
        }

        if (fraction > 0) {
            if (mScaledSolution == null || fractionRedrawRequired || forceDraw) {
                int wantedWidth = (int) (fraction * mBitmap.getWidth());
                int wantedHeight = (int) (fraction * mBitmap.getHeight());
                mScaledSolution = null;
                if (wantedWidth > 0 && wantedHeight > 0) {
                    mScaledSolution = BitmapUtil.resize(mBitmap, wantedWidth, wantedHeight);
                }
            }
            if (mScaledSolution != null) {
                mSolutionBackgroundCanvas.drawBitmap(mScaledSolution, mSolutionBackground.getWidth() / 2 - mScaledSolution.getWidth() / 2, mSolutionBackground.getHeight() * BUBBLE_CENTER_Y_ESTIMATE - mScaledSolution.getHeight() / 2, mSolutionPaint);
            }
        }

        mLastDrawnSolutionFraction = fraction;
        mLastDrawnPassedFraction = passedFraction;
    }

    @Override
    public long getPeriodicEventPeriod() {
        return UPDATE_PERIOD;
    }

    private void updateSolution(boolean forceDraw) {
        float scale = (float) Math.exp((mObstaclesPassed / (double) REQUIRED_TOTAL_OBSTACLES - 1.) * 7.);
        if (scale <= 1 || forceDraw) {
            scale = Math.min(MAX_SOLUTION_SCALE, scale);
            drawSolution(scale, forceDraw);
        }
    }

    private void onObstaclePassed(Actor obstacle) {
        obstacle.setActive(false);
        mValidDistanceRun = true; // after loading or first start, to prevent cheating by closing (which is still kinda possible to prevent getting hit but this is punished by decreasing distance)
        mObstaclesPassed += mDifficulty + 1;
        mDifficultyMinTimeExtra++;
        //noinspection SuspiciousMethodCalls
        mCurrentObstacles.remove(obstacle); // not suspicious
        updateSolution(false);
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_OBSTACLE_DODGED_COUNT, 1L, 0L);
        }
    }

    private void initDistanceRun(Compacter data) {
        mDistanceRun = 0f;
        if (data != null && data.getSize() >= 3) {
            try {
                mDistanceRun = data.getFloat(2);
            } catch (CompactedDataCorruptException e) {
                Log.e("Riddle", "Error reading distance run data: " + e);
            }
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DISTANCE_RUN, (long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        updateDifficulty();
    }

    private void onReleaseCollision() {
        mDistanceRun = 0f;
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DISTANCE_RUN, (long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        updateDifficulty();
        mCollisionBreak = false;
        mRunner.setStateFrames(HitboxJumpMover.STATE_NOT_MOVING);
    }

    private boolean onDistanceRun() {
        if (mValidDistanceRun) {
            mDistanceRun += PSEUDO_RUN_SPEED * ONE_SECOND / UPDATE_PERIOD;
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DISTANCE_RUN, (long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
                mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_RUN_HIGHSCORE, (long) mDistanceRun, AchievementJumper.DISTANCE_RUN_THRESHOLD);
            }
            if (mConfig.mAchievementTypeData != null) {
                mConfig.mAchievementTypeData.putValue(AchievementJumper.KEY_TYPE_TOTAL_RUN_HIGHSCORE, (long) mDistanceRun, AchievementJumper.DISTANCE_RUN_THRESHOLD);
            }
            updateDifficulty();
            return true;
        }
        return false;
    }

    private void updateDifficulty() {
        int oldDifficulty = mDifficulty;
        mDifficulty = 0;
        for (int i = DIFFICULTIES - 1; i >= 0; i--) {
            if (mDistanceRun >= DISTANCE_RUN_THRESHOLDS[i]) {
                mDifficulty = i;
                break;
            }
        }
        if (oldDifficulty != mDifficulty) {
            mDifficultyMinTimeExtra = 0L;
            if (mDifficulty != DIFFICULTY_EASY && mBoss != null) {
                mNextObstacle = mBoss;
            }
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DIFFICULTY, (long) mDifficulty, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        mTextPaint.setColor(DIFFICULTY_COLORS[mDifficulty]);
    }

    @Override
    public boolean onPeriodicEvent() {
        boolean change = false;
        if (!mCollisionBreak) {
            mWorld.update(UPDATE_PERIOD);
            drawForeground();
            change = onDistanceRun();
            change |= onBackgroundUpdate();
            checkNextObstacle();
        }
        return change;
    }

    private void onNextObstacle() {
        Obstacle o = mNextObstacle;
        if (o != null && !mCurrentObstacles.contains(o)) {
            mCurrentObstacles.add(o);
            o.joinTheFight();
            setNextObstacleRandomly();
            if (mNextObstacle != null) {
                double minTime = mNextObstacle.getMinSpawnTime(o, mDifficulty, mDifficultyMinTimeExtra);
                mNextObstacleCounter = (long) (minTime + mRand.nextDouble() * (NEXT_OBSTACLE_MAX_TIME[mDifficulty] - minTime));
            }
        } else {
            setNextObstacleRandomly();
        }
    }

    private void setNextObstacleRandomly() {
        List<Obstacle> obstacles = mObstacles.get(mDifficulty);
        Collections.shuffle(obstacles, mRand);
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) {
                mNextObstacle = obstacle;
                break;
            }
        }
    }

    private void checkNextObstacle() {
        mNextObstacleCounter -= UPDATE_PERIOD;
        if (mNextObstacleCounter <= 0L) {
            onNextObstacle();
        }
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mStateMotionIsDown = true;
            if (mCollisionBreak) {
                onReleaseCollision();
            } else {
                mFlagDoSuperJump = !mRunner.nextJump();
                if (!mFlagDoSuperJump && mConfig.mAchievementTypeData != null) {
                    mConfig.mAchievementTypeData.increment(AchievementJumper.KEY_TYPE_JUMP_COUNT, 1L, 0L);
                }
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            mStateMotionIsDown = false;
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        Compacter cmp = new Compacter(3);
        cmp.appendData(mBackgroundSlideCounter)
                .appendData(mObstaclesPassed);
        if (mCollisionBreak) {
            cmp.appendData(0.f); // the value is still set but not valid anymore during a break
        } else {
            cmp.appendData(mDistanceRun * DISTANCE_RUN_PENALTY_ON_SAVE_FRACTION);
        }
        return cmp.compact();
    }

    @Override
    public void onReachedEndOfWorld(Actor columbus, float x, float y, int borderFlags) {

    }

    @Override
    public void onLeftWorld(Actor jesus, int borderFlags) {
        if ((borderFlags & FlatRectWorld.BORDER_FLAG_LEFT) != 0) {
            onObstaclePassed(jesus);
        }
    }

    @Override
    public void onCollision(Actor colliding1, Actor colliding2) {
        if (!mCollisionBreak && (colliding1 == mRunner || colliding2 == mRunner)) {
            mCollisionBreak = true;
            for (Obstacle obstacle : mCurrentObstacles) {
                obstacle.setActive(false);
            }
            mCurrentObstacles.clear();
            mNextObstacle = null;
            mRunner.clearJump(getTopForRelativeHeight(RELATIVE_HEIGHT_BASELINE));
            mRunner.setStateFrames(Runner.STATE_COLLISION);
            drawForeground();
            updateSolution(false);
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_COLLISION_COUNT, 1L, 0L);
                if (colliding1 == mBoss || colliding2 == mBoss) {
                    mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_COLLIDED_WITH_KNUFFBUFF, 1L, 0L);
                }
            }
        }
    }

    @Override
    public void onMoverStateChange(Actor actor) {
        if (actor == mRunner && mRunner.isFalling() && (mStateMotionIsDown || mFlagDoSuperJump)) {
            if (mRunner.nextSuperJump()) {
                if (mConfig.mAchievementGameData != null) {
                    mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_DOUBLE_JUMP_COUNT, 1L, 0L);
                }
            }
            mFlagDoSuperJump = false;
        }
    }

    private static class Obstacle extends Actor {
        private long[] mMinTimes;
        private float mStartLeft;
        private float mStartTop;
        private HitboxNewtonMover mMover;
        private float mSpeedX;
        private float mSpeedY;

        public Obstacle(HitboxRect hitbox, HitboxNewtonMover mover, Look look, long[] minTimes) {
            super(hitbox, mover, look);
            mMinTimes = minTimes;
            mStartLeft = hitbox.getBoundingRect().left;
            mStartTop = hitbox.getBoundingRect().top;
            mMover = mover;
            mSpeedX = mover.getSpeedX();
            mSpeedY = mover.getSpeedY();
            setActive(false);
        }

        public static Obstacle makeObstacle(Look look, float hitboxWidthFraction, float hitboxHeightFraction, float frameLeft, float frameTop, long[] minTimes, FlatRectWorld world, float speedX, float speedY) {
            HitboxRect hitbox = HitboxRect.makeHitbox(look, hitboxWidthFraction, hitboxHeightFraction, frameLeft, frameTop);
            HitboxNewtonMover mover = new HitboxNewtonMover();
            mover.setSpeed(speedX, speedY);
            Obstacle obstacle = new Obstacle(hitbox, mover, look, minTimes);
            world.addActor(obstacle);
            return obstacle;
        }

        public double getMinSpawnTime(Obstacle previousObstacle, int difficulty, long minTimeDelta) {
            double minTime = NEXT_OBSTACLE_PREVIOUS_MIN_TIME_WEIGHT * previousObstacle.mMinTimes[difficulty]
                + (1 - NEXT_OBSTACLE_PREVIOUS_MIN_TIME_WEIGHT) * mMinTimes[difficulty];
            minTime -= minTimeDelta;
            return Math.max(mMinTimes[difficulty + 1], minTime);
        }

        public void joinTheFight() {
            getHitbox().setLeft(mStartLeft);
            getHitbox().setTop(mStartTop);
            mMover.setSpeed(mSpeedX, mSpeedY);
            setActive(true);
            resetCurrentFrames();
        }
    }

    private static class Runner extends Actor {

        public static final int STATE_COLLISION = -1;
        private HitboxJumpMover mJump;
        private boolean mSuperJumping;
        private float mFramesOffsetX;
        private float mFramesOffsetY;

        public Runner(HitboxRect hitbox, HitboxJumpMover mover, Look defaultLook) {
            super(hitbox, mover, defaultLook);
            mJump = mover;
            setActive(true);
        }

        @Override
        protected void onUpdateChangedMoverState() {
            setStateFramesByMoverState();
        }

        public static Runner makeRunner(Look look, float hitboxWidthFraction, float hitboxHeightFraction, float frameLeft, float frameTop, FlatRectWorld world) {
            HitboxRect hitbox = HitboxRect.makeHitbox(look, hitboxWidthFraction, hitboxHeightFraction, frameLeft, frameTop);

            Runner runner = new Runner(hitbox, new HitboxJumpMover(), look);

            world.addActor(runner);
            runner.mFramesOffsetX = look.getOffsetX();
            runner.mFramesOffsetY = look.getOffsetY();
            return runner;
        }

        public boolean isFalling() {
            return mJump.getState() == HitboxJumpMover.STATE_JUMP_FALLING;
        }

        @Override
        public void putStateFrames(int state, Look look) {
            look.setOffset(mFramesOffsetX, mFramesOffsetY);
            super.putStateFrames(state, look);
        }

        private boolean nextJump() {
            if (mJump.getState() == HitboxJumpMover.STATE_LANDED || mJump.getState() == HitboxJumpMover.STATE_NOT_MOVING) {
                mSuperJumping = false;
                float deltaHeight = JUMP_RELATIVE_HEIGHT_DELTA * mCurrentLook.getHeight();
                mJump.initJump(deltaHeight, deltaHeight, JUMP_DURATION);
                setStateFrames(mJump.getState());
                return true;
            }
            return false;
        }

        private boolean nextSuperJump() {
            if (!mSuperJumping) {
                mSuperJumping = true;
                mJump.initJump((DOUBLE_JUMP_RELATIVE_HEIGHT_DELTA - JUMP_RELATIVE_HEIGHT_DELTA) * mCurrentLook.getHeight(), DOUBLE_JUMP_RELATIVE_HEIGHT_DELTA * mCurrentLook.getHeight(),
                        DOUBLE_JUMP_REST_DURATION);
                setStateFrames(HitboxJumpMover.STATE_JUMP_ASCENDING);
                return true;
            }
            return false;
        }

        public void clearJump(float frameTop) {
            mJump.stop();
            getHitbox().setTop(frameTop - mFramesOffsetY);
        }
    }

    @Override
    public Bitmap makeSnapshot() {
        Bitmap snapshot = Bitmap.createScaledBitmap(mRunnerBackground, RiddleGame.SNAPSHOT_WIDTH, RiddleGame.SNAPSHOT_HEIGHT, false);
        Canvas canvas = new Canvas(snapshot);
        Bitmap overlay = Bitmap.createScaledBitmap(mSolutionBackground, RiddleGame.SNAPSHOT_WIDTH, RiddleGame.SNAPSHOT_HEIGHT, false);
        canvas.drawBitmap(overlay, 0, 0, null);
        overlay = Bitmap.createScaledBitmap(mForeground, RiddleGame.SNAPSHOT_WIDTH, RiddleGame.SNAPSHOT_HEIGHT, false);
        canvas.drawBitmap(overlay, 0, 0, null);
        return snapshot;
    }
}
