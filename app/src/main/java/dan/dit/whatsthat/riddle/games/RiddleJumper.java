package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementJumper;
import dan.dit.whatsthat.riddle.types.Types;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.sortiment.SortimentHolder;
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
    //fixed constants
    private static final float RELATIVE_HEIGHT_BASELINE = 1.f;
    private static final long ONE_SECOND = 1000L;
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

    private static final int DIFFICULTIES = 4;
    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;
    public static final int DIFFICULTY_ULTRA = 3;
    private static final float DISTANCE_RUN_START_FURTHER_FEATURE = meterToDistanceRun(200);
    private static final int[] DISTANCE_RUN_THRESHOLDS = new int[] {0, (int) (meterToDistanceRun(150)), (int) (meterToDistanceRun(400)), (int) (meterToDistanceRun(800))};
    private static final long[] NEXT_OBSTACLE_MIN_TIME_SMALL = new long[] {930L, 740L, 580L, 570L, 540L};
    private static final long[] NEXT_OBSTACLE_MIN_TIME_BIG = new long[] {1000L, 1200L, 1075L, 1025L, 905L};
    private static final long[] NEXT_OBSTACLE_MAX_TIME = new long[] {2000L, 1700L, 1400L, 1250L, 1150L}; //ms, maximum time until the next obstacle appears
    private static final long[] NEXT_OBSTACLE_MIN_TIME_SMALL_WIDTH = new long[] {1200L, 1100L, 1100L, 1025L, 860L};
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
    private static final float[] CLEAR_MIND_SIZE_FRACTION = new float[] {0.05f, 0.13f, 0.3f, 0.7f};
    private static final int FOGGED_MIND_COLOR = Color.DKGRAY;
    private static final int[] MIND_CLEARED_EVERY_K_OBSTACLES = new int[] {2, 3, 5, 8};
    private static final int MAX_COLLISIONS_FOR_SCORE_BONUS = 1;


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
    private Paint mDistanceTextPaint;
    private List<List<Obstacle>> mObstacles;
    private int mDifficulty;
    private long mDifficultyMinTimeExtra;
    private boolean mStateMotionIsDown;
    private boolean mFlagDoSuperJump;
    private float mObstaclesSpeed;
    private Obstacle mBoss;
    private Bitmap mClearMindBackground;
    private Canvas mClearMindCanvas;
    private Bitmap[] mClearMind;
    private List<Float> mClearMindX;
    private List<Float> mClearMindY;
    private List<Integer> mClearMindType;
    private Paint mSolutionPaint;
    private Paint mClearMindPaint;
    private Bitmap mBitmapScaled;
    private Paint mCollisionBreakTextPaint;
    private String[] mCollisionBreakTexts;
    private String mNewHighscoreText;
    private String mOldHighscoreText;
    private String mCurrentDistanceRunMeterText;
    private int mLastDrawnDistanceRun;
    private float mDistanceRunStart;

    public RiddleJumper(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_RUN_STARTED, 1L, 0L);
        }
    }

    @Override
    protected void calculateGainedScore(int[] scores) {
        int collisions = mConfig.mAchievementGameData != null ? mConfig.mAchievementGameData.getValue(AchievementJumper.KEY_GAME_COLLISION_COUNT, 0L).intValue() : 0;
        int bonus = (collisions <= MAX_COLLISIONS_FOR_SCORE_BONUS ? Types.SCORE_MEDIUM : 0);
        super.calculateGainedScore(scores);
        scores[3] += bonus;
        scores[0] += bonus;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mRunnerBackground, 0, 0, null);
        canvas.drawBitmap(mSolutionBackground, 0, 0, null);
        canvas.drawBitmap(mForeground, 0, 0, null);
        int currDistanceRun = distanceRunToMeters(mDistanceRun);
        // this optimization is only done to reduce amount of times the strings are concatenated since this allocates a StringBuilder
        if (currDistanceRun != mLastDrawnDistanceRun || mCurrentDistanceRunMeterText == null) {
            mLastDrawnDistanceRun = currDistanceRun;
            mCurrentDistanceRunMeterText = Integer.toString(currDistanceRun) + "m";
        }
        drawTextCenteredX(canvas, mCurrentDistanceRunMeterText, canvas.getWidth() / 2.f, 35.f, mDummyRect, mDistanceTextPaint);
    }

    private Rect mDummyRect;
    private static void drawTextCenteredX(Canvas canvas, String text, float x, float y, Rect dummyRect, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), dummyRect);
        canvas.drawText(text, x - dummyRect.exactCenterX(), y, paint);
    }

    private static void setFittingTextSizeX(String text, int maxWidth, Rect dummyRect, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), dummyRect);
        float currWidth = dummyRect.width();
        float newTextSize = paint.getTextSize() * maxWidth / currWidth; // assume linear correlation between text size and width
        paint.setTextSize(newTextSize);
    }

    public static int distanceRunToMeters(float distanceRun) {
        return (int) (distanceRun / ONE_SECOND);
    }

    public static float meterToDistanceRun(int meter) {
        return meter * ONE_SECOND;
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mDummyRect = new Rect();
        mRand = new Random();
        mBitmapScaled = BitmapUtil.attemptBitmapScaling(mBitmap, (int) (mBitmap.getWidth() * MAX_SOLUTION_SCALE), (int) (mBitmap.getHeight() * MAX_SOLUTION_SCALE), false);
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mDistanceTextPaint = new Paint();
        mDistanceTextPaint.setAntiAlias(true);
        mDistanceTextPaint.setTextSize(ImageUtil.convertDpToPixel(22, mConfig.mScreenDensity));
        mCollisionBreakTextPaint = new Paint();
        mCollisionBreakTextPaint.setFakeBoldText(true);
        mCollisionBreakTextPaint.setAntiAlias(true);
        mCollisionBreakTexts = res.getStringArray(R.array.riddle_jumper_collision_break);
        mNewHighscoreText = res.getString(R.string.riddle_jumper_new_highscore);
        mOldHighscoreText = res.getString(R.string.riddle_jumper_old_highscore);

        mSolutionBackgroundHeight = (int) (mConfig.mHeight / Types.Jumper.BITMAP_ASPECT_RATIO);
        listener.onProgressUpdate(20);
        mObstaclesSpeed = - mConfig.mWidth / ((float) OBSTACLES_RIGHT_LEFT_DURATION / ONE_SECOND);
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
        initSolution(res, cmp);
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
                null};
        monsterTeufel[3] = monsterTeufel[1];
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

    private boolean onBackgroundUpdate(long updateTime) {
        mBackgroundSlideCounter += updateTime;
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

    private void drawForeground() {
        mForegroundCanvas.drawPaint(mClearPaint);
        mWorld.draw(mForegroundCanvas, null);
        if (mCollisionBreak) {
            Canvas canvas = mForegroundCanvas;
            if (mDifficulty < mCollisionBreakTexts.length) {
                drawTextCenteredX(canvas, mCollisionBreakTexts[mDifficulty], canvas.getWidth() / 2.f, mSolutionBackground.getHeight() / 2.f, mDummyRect, mCollisionBreakTextPaint);
            }
            if (mConfig.mAchievementGameData != null) {
                long currentHighscore = mConfig.mAchievementTypeData.getValue(AchievementJumper.KEY_TYPE_TOTAL_RUN_HIGHSCORE, 0L);
                if (mDistanceRun >= currentHighscore) {
                    // new highscore, set it directly so that the threshold is not displayed and no confusion appears
                    updateHighscore((long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
                    Paint paint = mDistanceTextPaint;
                    int oldColor = paint.getColor();
                    paint.setColor(0xffdc9912);
                    drawTextCenteredX(canvas, String.format(mNewHighscoreText, distanceRunToMeters(mDistanceRun)), canvas.getWidth() / 2.f, mSolutionBackground.getHeight() / 2.f + 2 * mDummyRect.height(), mDummyRect, paint);
                    paint.setColor(oldColor);
                } else {
                    drawTextCenteredX(canvas, String.format(mOldHighscoreText, distanceRunToMeters(currentHighscore)), canvas.getWidth() / 2.f, mSolutionBackground.getHeight() / 2.f + mDummyRect.height(), mDummyRect, mDistanceTextPaint);
                }
            }
        }
    }

    private float getTopForRelativeHeight(float relativeHeight) {
        return mConfig.mHeight - mRunnerHeight * relativeHeight;
    }

    private void initSolution(Resources res, Compacter cmp) {
        mSolutionPaint = new Paint();
        mSolutionPaint.setAntiAlias(true);
        mSolutionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mSolutionBackground = Bitmap.createBitmap(mConfig.mWidth, mSolutionBackgroundHeight, Bitmap.Config.ARGB_8888);
        mSolutionBackgroundCanvas = new Canvas(mSolutionBackground);
        mThoughtbubble = ImageUtil.loadBitmap(res, R.drawable.gedankenblase, (int) (BUBBLE_SCALE * mConfig.mWidth), (int) (BUBBLE_SCALE * mSolutionBackgroundHeight), true);

        mClearMind = new Bitmap[CLEAR_MIND_SIZE_FRACTION.length];
        for (int i = 0; i < CLEAR_MIND_SIZE_FRACTION.length; i++) {
            float fraction = CLEAR_MIND_SIZE_FRACTION[i];
            int clearMindSize = (int) (Math.min(mSolutionBackground.getWidth(), mSolutionBackground.getHeight()) * fraction);
            mClearMind[i] = ImageUtil.loadBitmap(res, R.drawable.explosion1, clearMindSize, clearMindSize, false);
        }
        mClearMindX = new LinkedList<>();
        mClearMindY = new LinkedList<>();
        mClearMindType = new LinkedList<>();
        mClearMindBackground = Bitmap.createBitmap(mSolutionBackground.getWidth(), mSolutionBackground.getHeight(), mSolutionBackground.getConfig());
        mClearMindCanvas = new Canvas(mClearMindBackground);
        mClearMindCanvas.drawColor(FOGGED_MIND_COLOR);
        mMindRect = new Rect();
        mSourceRect = new Rect();
        mClearMindPaint = new Paint();
        mClearMindPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mSolutionBackgroundCanvas.drawPaint(mClearPaint);
        mSolutionBackgroundCanvas.drawBitmap(mThoughtbubble, mSolutionBackground.getWidth() / 2 - mThoughtbubble.getWidth() / 2, mSolutionBackground.getHeight() / 2 - mThoughtbubble.getHeight() / 2, null);
        mSolutionBackgroundCanvas.drawBitmap(mClearMindBackground, 0, 0, mSolutionPaint);
        if (cmp != null && cmp.getSize() > 5) {
            Compacter xData = new Compacter(cmp.getData(3));
            Compacter yData = new Compacter(cmp.getData(4));
            Compacter typeData = new Compacter(cmp.getData(5));
            try {
                int count = Math.min(xData.getSize(), Math.min(yData.getSize(), typeData.getSize()));
                for (int i = 0; i < count; i++) {
                    clearMind(xData.getFloat(i), yData.getFloat(i), typeData.getInt(i));
                }
            } catch (CompactedDataCorruptException e) {
                Log.e("Riddle", "Clear mind data corrupt for RiddleJumper: " + e);
            }
        }
    }

    private Rect mSourceRect;
    private Rect mMindRect;
    private void clearMind(float x, float y, int type) {
        mClearMindX.add(x);
        mClearMindY.add(y);
        type = Math.min(mClearMind.length - 1, type);
        mClearMindType.add(type);

        // update the mind canvas by exploding the part out
        Bitmap clearMind = mClearMind[type];
        mMindRect.set((int) x, (int) y, (int) (x + clearMind.getWidth() + 0.5f), (int) (y + clearMind.getHeight() + 0.5f));
        mClearMindCanvas.drawBitmap(clearMind, x, y, mClearMindPaint);

        // redraw relevant part of the thought bubble
        mSourceRect.set(mMindRect);
        mSourceRect.offset(-mSolutionBackground.getWidth() / 2 + mThoughtbubble.getWidth() / 2, -mSolutionBackground.getHeight() / 2 + mThoughtbubble.getHeight() / 2);
        mSolutionBackgroundCanvas.drawBitmap(mThoughtbubble, mSourceRect, mMindRect, null);

        // redraw relevant part of the original bitmap
        mSourceRect.set(mMindRect);
        mSourceRect.offset(-mSolutionBackground.getWidth() / 2 + mBitmapScaled.getWidth() / 2, (int) (-mSolutionBackground.getHeight() * BUBBLE_CENTER_Y_ESTIMATE + mBitmapScaled.getHeight() / 2));
        mSolutionBackgroundCanvas.drawBitmap(mBitmapScaled, mSourceRect, mMindRect, mSolutionPaint);

        // overdraw relevant part of solution background with mind canvas
        mSolutionBackgroundCanvas.drawBitmap(mClearMindBackground, mMindRect, mMindRect, mSolutionPaint);
    }

    private void onObstaclePassed(Actor obstacle) {
        obstacle.setActive(false);
        mValidDistanceRun = true; // after loading or first start, to prevent cheating by closing (which is still kinda possible to prevent getting hit but this is punished by decreasing distance)
        mObstaclesPassed++;
        mDifficultyMinTimeExtra++;
        //noinspection SuspiciousMethodCalls
        mCurrentObstacles.remove(obstacle); // not suspicious
        if (mObstaclesPassed % MIND_CLEARED_EVERY_K_OBSTACLES[mDifficulty] == 0) {
            int type = mDifficulty;
            float x = mRand.nextFloat() * (mClearMindBackground.getWidth() - mClearMind[type].getWidth());
            float y = mRand.nextFloat() * (mClearMindBackground.getHeight() * 2 * BUBBLE_CENTER_Y_ESTIMATE);
            clearMind(x, y, type);
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_OBSTACLE_DODGED_COUNT, 1L, 0L);
        }
    }

    private void initDistanceRun(Compacter data) {
        if (TestSubject.getInstance().hasFeature(SortimentHolder.ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE)) {
            mDistanceRunStart = DISTANCE_RUN_START_FURTHER_FEATURE;
        } else {
            mDistanceRunStart = 0.f;
        }
        mDistanceRun = mDistanceRunStart;
        if (data != null && data.getSize() > 2) {
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
        updateCollisionBreakPaint();
    }

    private void onReleaseCollision() {
        mDistanceRun = mDistanceRunStart;
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DISTANCE_RUN, (long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        updateDifficulty();
        mCollisionBreak = false;
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.increment(AchievementJumper.KEY_GAME_RUN_STARTED, 1L, 0L);
        }
        mRunner.setStateFrames(HitboxJumpMover.STATE_NOT_MOVING);
    }

    private void updateHighscore(Long distanceRun, Long threshold) {
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_RUN_HIGHSCORE, distanceRun, threshold);
        }
        if (mConfig.mAchievementTypeData != null) {
            mConfig.mAchievementTypeData.putValue(AchievementJumper.KEY_TYPE_TOTAL_RUN_HIGHSCORE, distanceRun, threshold);
        }
    }

    private boolean onDistanceRun(long updateTime) {
        if (mValidDistanceRun) {
            mDistanceRun += PSEUDO_RUN_SPEED * ONE_SECOND / updateTime;
            if (mConfig.mAchievementGameData != null) {
                mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DISTANCE_RUN, (long) mDistanceRun, AchievementProperties.UPDATE_POLICY_ALWAYS);
            }
            updateHighscore((long) mDistanceRun, AchievementJumper.DISTANCE_RUN_THRESHOLD);
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
            updateCollisionBreakPaint();
        }
        if (mConfig.mAchievementGameData != null) {
            mConfig.mAchievementGameData.putValue(AchievementJumper.KEY_GAME_CURRENT_DIFFICULTY, (long) mDifficulty, AchievementProperties.UPDATE_POLICY_ALWAYS);
        }
        mDistanceTextPaint.setColor(DIFFICULTY_COLORS[mDifficulty]);
    }

    private void updateCollisionBreakPaint() {
        mCollisionBreakTextPaint.setColor(DIFFICULTY_COLORS[mDifficulty]);
        if (mDifficulty < mCollisionBreakTexts.length) {
            setFittingTextSizeX(mCollisionBreakTexts[mDifficulty], mConfig.mWidth - 30, mDummyRect, mCollisionBreakTextPaint);
        }
    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updateTime) {
        if (!mCollisionBreak) {
            mWorld.update(updateTime);
            drawForeground();
            onDistanceRun(updateTime);
            onBackgroundUpdate(updateTime);
            checkNextObstacle(updateTime);
        }
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
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obstacle = obstacles.get(i);
            if (!obstacle.isActive()) {
                mNextObstacle = obstacle;
                break;
            }
        }
    }

    private void checkNextObstacle(long updateTime) {
        mNextObstacleCounter -= updateTime;
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
        Compacter cmp = new Compacter(5);
        cmp.appendData(mBackgroundSlideCounter)
                .appendData(mObstaclesPassed);
        if (mCollisionBreak) {
            cmp.appendData(0.f); // the value is still set but not valid anymore during a break
        } else {
            cmp.appendData(mDistanceRun * DISTANCE_RUN_PENALTY_ON_SAVE_FRACTION);
        }
        Compacter clearMindX = new Compacter(mClearMindX.size());
        Compacter clearMindY = new Compacter(mClearMindY.size());
        Compacter clearType = new Compacter(mClearMindType.size());
        for (Float xData : mClearMindX) {
            clearMindX.appendData(xData);
        }
        for (Float yData : mClearMindY) {
            clearMindY.appendData(yData);
        }
        for (Integer type : mClearMindType) {
            clearType.appendData(type);
        }
        cmp.appendData(clearMindX.compact());
        cmp.appendData(clearMindY.compact());
        cmp.appendData(clearType.compact());
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
        int width = SNAPSHOT_DIMENSION.getWidthForDensity(mConfig.mScreenDensity);
        int height = SNAPSHOT_DIMENSION.getHeightForDensity(mConfig.mScreenDensity);
        Bitmap snapshot = Bitmap.createScaledBitmap(mRunnerBackground, width, height, false);
        Canvas canvas = new Canvas(snapshot);
        Bitmap overlay = Bitmap.createScaledBitmap(mSolutionBackground, width, height, false);
        canvas.drawBitmap(overlay, 0, 0, null);
        overlay = Bitmap.createScaledBitmap(mForeground, width, height, false);
        canvas.drawBitmap(overlay, 0, 0, null);
        return snapshot;
    }
}
