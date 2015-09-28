package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.util.Random;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.SimpleInterpolation;
import dan.dit.whatsthat.util.flatworld.collision.GeneralHitboxCollider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.collision.HitboxCircle;
import dan.dit.whatsthat.util.flatworld.look.CircleLook;
import dan.dit.whatsthat.util.flatworld.look.Frames;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNewtonMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNoMover;
import dan.dit.whatsthat.util.flatworld.world.Actor;
import dan.dit.whatsthat.util.flatworld.world.FlatRectWorld;
import dan.dit.whatsthat.util.flatworld.world.FlatWorldCallback;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 *
 * Created by daniel on 04.09.15.
 */
public class RiddleLazor extends RiddleGame implements FlatWorldCallback {
    private static final float METEOR_BEAM_WIDTH_FRACTION_OF_SCREEN_DIAGONAL = 0.018f; //meteor trail width
    private static final float METEOR_RADIUS_FRACTION_OF_SCREEN_DIAGONAL = 0.025f; //meteor head radius
    private static final float METEOR_DIAGONAL_DURATION = 9000.f; //ms, time for a meteor that moves directly from top left to bottom right
    private static final float ONE_SECOND = 1000.f; // ms, one second, fixed
    private static final double CHANCE_FOR_BONUS_BEAM = 0.12; //chance for super beam in percent
    private static final float CANNON_BEAM_FRACTION_OF_SCREEN_DIAGONAL = 0.005f;
    private static final float CANNONBALL_RADIUS_FRACTION_OF_SCREEN_DIAGONAL = 0.03f;
    private static final float CANNONBALL_DIAGONAL_DURATION = 9000.f; //ms
    private static final long CANNON_RELOAD_DURATION_START = Cannon.LOADING_STATES_COUNT * 1600L; //for each loading state (3atm) wait x ms
    private static final long CANNON_RELOAD_DURATION_DIFFICULTY_ULTRA = Cannon.LOADING_STATES_COUNT * 500L;

    private static final long CANNONBALL_EXPLOSION_DURATION = 2000L;
    private static final float CANNONBALL_EXPLOSION_MAX_GROWTH_FACTOR = 2.3f;
    private static final int CANNONBALL_EXPLOSION_COLOR = 0xFFa6a6a6;
    private static final long CANNON_LOADED_FRAME_DURATION = 150L;//ms
    private static final long CANNON_FRAME_DURATION = 100L;//ms
    private static final float CANNON_RADIUS_FRACTION_OF_WIDTH = 0.09f;
    private static final float METEOR_EXPLOSION_RADIUS_FACTOR = 1.5f; // the factor on the radius of the meteor ball when exploding in the city

    private static final int DIFFICULTY_POINTS_GAIN_ON_METEOR_KILL = 1;
    private static final int DIFFICULTY_POINTS_LOSS_PER_DIFFICULTY = 1;
    private static final int DIFFICULTY_AT_BEGINNING = 20;
    private static final float DIFFICULTY_ULTRA_AT = 100;
    private static final float METEOR_SPAWN_TIME_START = 3800;;
    private static final float METEOR_SPAWN_TIME_DIFFICULTY_ULTRA = 300;

    private FlatRectWorld mFlatWorld;
    private int mDifficulty;
    private float mDiagonal;
    private float mMeteorRadiusPixels;
    private float mMeteorSpeedPixels;
    private int mMeteorBeamWidthPixels;
    private float mCannonBallRadiusPixels;
    private int mCannonBallBeamWidthPixels;
    private float mCannonBallSpeed;

    private Bitmap[] mVisibleTypeLayers;
    private Canvas[] mLayersCanvas;
    private Paint[] mColorTypePaints;
    private Bitmap[] mMeteorBalls;
    private Canvas mWorldCanvas;
    private Bitmap mWorldBitmap;
    private Paint mWorldBackgroundPaint;
    private long mNextMeteorDuration;
    private Random mRand;
    private boolean mRefreshLayers;
    private Bitmap mVisibleLayer;
    private Canvas mVisibleLayerCanvas;
    private Paint mClearPaint;
    private Paint[] mVisibleLayerPaints;
    private float mCityOffsetY;
    private Bitmap mCityLayer;
    private Canvas mCityLayerCanvas;
    private Bitmap mCannonBall;
    private Cannon mCannonLeft;
    private Cannon mCannonRight;
    private Paint mClearColorTypePaint;
    private Bitmap mCityDestructionMask;
    private Canvas mCityDestructionCanvas;
    private Paint mCityDestructionOverlayPaint;
    private Paint mMeteorDestructionPaint;
    private Paint mDifficultyTextPaint;
    private SimpleInterpolation mMeteorSpawnTimeInterpolator;
    private SimpleInterpolation mReloadTimeInterpolator;
    private Bitmap mProtectedCity;
    private Bitmap mGenerator;
    private boolean mProtected;


    public RiddleLazor(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mVisibleLayer, 0, 0, null);
        canvas.drawBitmap(mCityLayer, 0, mCityOffsetY, null);
        drawTextCenteredX(canvas, String.valueOf(mDifficulty), mCityLayer.getWidth() / 2, mCityOffsetY + mCityLayer.getHeight() / 2, mDummyRect, mDifficultyTextPaint);
        canvas.drawBitmap(mCityDestructionMask, 0, mCityOffsetY, mCityDestructionOverlayPaint);
        if (mProtected) {
            canvas.drawBitmap(mProtectedCity, 0, mCityOffsetY, null);
        }
        canvas.drawBitmap(mWorldBitmap, 0, 0, null);
    }

    private Rect mDummyRect;
    private static void drawTextCenteredX(Canvas canvas, String text, float x, float y, Rect dummyRect, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), dummyRect);
        canvas.drawText(text, x - dummyRect.exactCenterX(), y, paint);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mDiagonal = (float) Math.sqrt(mConfig.mWidth * mConfig.mWidth + mConfig.mHeight * mConfig.mHeight);
        mCityOffsetY = mBitmap.getHeight();
        mRand = new Random();
        mFlatWorld = new FlatRectWorld(new RectF(0, 0, mConfig.mWidth, mConfig.mHeight), new GeneralHitboxCollider(), this);
        mWorldBackgroundPaint = new Paint();
        mWorldBackgroundPaint.setColor(Color.BLACK);
        mWorldBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mWorldBitmap = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight, mBitmap.getConfig());
        mWorldCanvas = new Canvas(mWorldBitmap);

        mVisibleLayer = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mVisibleLayerCanvas = new Canvas(mVisibleLayer);
        mVisibleLayerPaints = new Paint[COLOR_TYPES_COUNT];
        Paint visibleLayerPaint = new Paint();
        visibleLayerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        for (int i = 0; i < COLOR_TYPES_COUNT - 1; i++) {
            mVisibleLayerPaints[i] = visibleLayerPaint;
        }
        Paint visibleAlphaLayerPaint = new Paint();
        visibleAlphaLayerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        mVisibleLayerPaints[COLOR_TYPE_BONUS] = visibleAlphaLayerPaint;

        mVisibleTypeLayers = new Bitmap[COLOR_TYPES_COUNT];
        mLayersCanvas = new Canvas[COLOR_TYPES_COUNT];
        mClearColorTypePaint = new Paint();
        mClearColorTypePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));


        mColorTypePaints = new Paint[COLOR_TYPES_COUNT];
        for (int type : COLOR_TYPES) {
            Paint typePaint = new Paint();
            typePaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR));
            if (type == COLOR_TYPE_BONUS) {
                typePaint.setColorFilter(new PorterDuffColorFilter(colorTypeToColor(type), PorterDuff.Mode.DST_IN));
            } else {
                typePaint.setColorFilter(new PorterDuffColorFilter(colorTypeToColor(type), PorterDuff.Mode.MULTIPLY));
            }
            mColorTypePaints[type] = typePaint;
            mVisibleTypeLayers[type] = Bitmap.createBitmap(mVisibleLayer.getWidth(), mVisibleLayer.getHeight(), Bitmap.Config.ARGB_8888);
            mLayersCanvas[type] = new Canvas(mVisibleTypeLayers[type]);
        }
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mCityLayer = Bitmap.createBitmap(mConfig.mWidth, mConfig.mHeight - mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCityLayerCanvas = new Canvas(mCityLayer);
        mProtectedCity = ImageUtil.loadBitmap(res, R.drawable.skylinecity_protected, mCityLayer.getWidth(), mCityLayer.getHeight(), BitmapUtil.MODE_FIT_EXACT);
        mGenerator = ImageUtil.loadBitmap(res, R.drawable.atomkraftwerk, mCityLayer.getWidth(), mCityLayer.getHeight(), BitmapUtil.MODE_FIT_INSIDE);
        Bitmap city = ImageUtil.loadBitmap(res, R.drawable.skylinecity, mCityLayer.getWidth(), mCityLayer.getHeight(), BitmapUtil.MODE_FIT_EXACT);
        makeCityLayer(city);
        mCityDestructionMask = Bitmap.createBitmap(mCityLayer.getWidth(), mCityLayer.getHeight(), mCityLayer.getConfig());
        mCityDestructionCanvas = new Canvas(mCityDestructionMask);
        mCityDestructionCanvas.drawColor(Color.BLACK);
        mCityDestructionOverlayPaint = new Paint();
        mCityDestructionOverlayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mMeteorDestructionPaint = new Paint();
        mMeteorDestructionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mDummyRect = new Rect();
        mDifficultyTextPaint = new Paint();
        mDifficultyTextPaint.setTextSize(ImageUtil.convertDpToPixel(20.f, mConfig.mScreenDensity));
        mDifficultyTextPaint.setAntiAlias(true);
        mDifficultyTextPaint.setColor(Color.YELLOW);

        initDifficulty();
        initMeteorData(res);
        initCannonData(res);

        mMeteorSpawnTimeInterpolator = new SimpleInterpolation.LinearInterpolation(0.f, METEOR_SPAWN_TIME_START, DIFFICULTY_ULTRA_AT, METEOR_SPAWN_TIME_DIFFICULTY_ULTRA);
        mReloadTimeInterpolator = new SimpleInterpolation.LinearInterpolation(0, CANNON_RELOAD_DURATION_START, DIFFICULTY_ULTRA_AT, CANNON_RELOAD_DURATION_DIFFICULTY_ULTRA);
    }

    private void makeCityLayer(Bitmap cityBitmap) {
        if (cityBitmap != null) {
            mCityLayerCanvas.drawBitmap(cityBitmap, 0, 0, null);
        }
        if (mGenerator != null) { // only null if drawable deleted or serious bug happens (like R file corrupt)
            mCityLayerCanvas.drawBitmap(mGenerator, mCityLayer.getWidth() / 2 - mGenerator.getWidth() / 2, 0, null);
        }
    }

    private void initDifficulty() {
        mDifficulty = DIFFICULTY_AT_BEGINNING;
        onDifficultyUpdated();
    }

    private void initMeteorData(Resources res) {
        mMeteorBeamWidthPixels = (int) (mDiagonal * METEOR_BEAM_WIDTH_FRACTION_OF_SCREEN_DIAGONAL);
        setMeteorBeamWidthPixels(mMeteorBeamWidthPixels);
        mMeteorSpeedPixels = mDiagonal / (METEOR_DIAGONAL_DURATION / ONE_SECOND);
        mMeteorRadiusPixels = mDiagonal * METEOR_RADIUS_FRACTION_OF_SCREEN_DIAGONAL;
        mMeteorRadiusPixels = Math.max(mMeteorRadiusPixels, 1);
        int size = (int) (mMeteorRadiusPixels * 2);
        mMeteorBalls = new Bitmap[COLOR_TYPES_COUNT];
        mMeteorBalls[COLOR_TYPE_RED] = ImageUtil.loadBitmap(res, R.drawable.laser_red, size, size, BitmapUtil.MODE_FIT_EXACT);
        mMeteorBalls[COLOR_TYPE_GREEN] = ImageUtil.loadBitmap(res, R.drawable.laser_green, size, size, BitmapUtil.MODE_FIT_EXACT);
        mMeteorBalls[COLOR_TYPE_BLUE] = ImageUtil.loadBitmap(res, R.drawable.laser_blue, size, size, BitmapUtil.MODE_FIT_EXACT);
        mMeteorBalls[COLOR_TYPE_BONUS] = ImageUtil.loadBitmap(res, R.drawable.lazor_white, size, size, BitmapUtil.MODE_FIT_EXACT);
        Log.d("Riddle", "Meteor data initialized: beam width=" + mMeteorBeamWidthPixels + " , speed=" + mMeteorSpeedPixels + " , radius=" + mMeteorRadiusPixels);
    }

    private void initCannonData(Resources res) {
        mCannonBallRadiusPixels = (int) (mDiagonal * CANNONBALL_RADIUS_FRACTION_OF_SCREEN_DIAGONAL);
        mCannonBallRadiusPixels = Math.max(1, mCannonBallRadiusPixels);
        mCannonBallBeamWidthPixels = (int) (mDiagonal * CANNON_BEAM_FRACTION_OF_SCREEN_DIAGONAL);
        mCannonBallBeamWidthPixels = Math.max(1, mCannonBallBeamWidthPixels);
        mCannonBallSpeed = mDiagonal / (CANNONBALL_DIAGONAL_DURATION / ONE_SECOND);
        int size = (int) (mCannonBallRadiusPixels * 2);
        mCannonBall = ImageUtil.loadBitmap(res, R.drawable.laser_alpha, size, size, BitmapUtil.MODE_FIT_EXACT);

        float cannonWallOffsetX = ImageUtil.convertDpToPixel(2.f, mConfig.mScreenDensity);
        float cannonRadius = CANNON_RADIUS_FRACTION_OF_WIDTH * mConfig.mWidth;
        int cannonSize = (int) (cannonRadius * 2);
        Bitmap[] loadedFrames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_loaded_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_loaded_frame2, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_loaded_frame3, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                null};
        loadedFrames[3] = loadedFrames[1];
        Bitmap[] loading1Frames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_level1_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_level1_frame2, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};
        Bitmap[] loading2Frames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_level2_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_level2_frame2, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};

        Bitmap[] firedFrames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_fired_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};

        mCannonLeft = new Cannon(new HitboxCircle(cannonRadius + cannonWallOffsetX, mConfig.mHeight, cannonRadius),
                HitboxNoMover.INSTANCE, loadedFrames,
                0, - (int) cannonRadius);
        mCannonLeft.initLoadingLooks(loading1Frames, loading2Frames, firedFrames);
        mFlatWorld.addActor(mCannonLeft);
        mCannonRight = new Cannon(new HitboxCircle(mConfig.mWidth - cannonRadius - cannonWallOffsetX, mConfig.mHeight, cannonRadius),
                HitboxNoMover.INSTANCE, loadedFrames,
                 0, - (int) cannonRadius);
        mCannonRight.initLoadingLooks(loading1Frames, loading2Frames, firedFrames);
        mFlatWorld.addActor(mCannonRight);
    }

    private void setMeteorBeamWidthPixels(int beamWidthPixels) {
        mMeteorBeamWidthPixels = Math.max(1, beamWidthPixels); //at least one pixel width
        for (Paint mColorTypePaint : mColorTypePaints) {
            mColorTypePaint.setStrokeWidth(mMeteorBeamWidthPixels);
        }
        mMeteorDestructionPaint.setStrokeWidth(mMeteorBeamWidthPixels);
        mClearColorTypePaint.setStrokeWidth(mMeteorBeamWidthPixels);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float targetX = event.getX();
            float targetY = event.getY();
            Cannon cannon = findAvailableCannon(targetX, targetY);
            if (cannon != null) {
                cannon.shoot(targetX, targetY);
            }
        }
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return "";
    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updatePeriod) {
        mRefreshLayers = false;
        mFlatWorld.update(updatePeriod);
        checkedRefreshLayers();
        mWorldCanvas.drawPaint(mWorldBackgroundPaint);
        mFlatWorld.draw(mWorldCanvas, null);
        updateMeteorsController(updatePeriod);
    }

    private void checkedRefreshLayers() {
        if (mRefreshLayers) {
            mVisibleLayerCanvas.drawPaint(mClearPaint);
            for (int type : COLOR_TYPES) {
                mVisibleLayerCanvas.drawBitmap(mVisibleTypeLayers[type], 0, 0, mVisibleLayerPaints[type]);
            }
        }
    }

    private void updateMeteorsController(long updatePeriod) {
        mNextMeteorDuration -= updatePeriod;
        if (mNextMeteorDuration <= 0L) {
            //spawn new meteor
            mNextMeteorDuration = (long) mMeteorSpawnTimeInterpolator.interpolate(Math.min(mDifficulty, DIFFICULTY_ULTRA_AT));
            mFlatWorld.addActor(makeMeteor(mRand.nextInt(mConfig.mWidth), 0, mRand.nextInt(mConfig.mWidth), mConfig.mHeight, nextColorType()));
        }
    }

    private int nextColorType() {
        if (mRand.nextDouble() < CHANCE_FOR_BONUS_BEAM) {
            return COLOR_TYPE_BONUS;
        } else {
            return mRand.nextInt(COLOR_TYPES_COUNT - 1);
        }
    }

    @Override
    public void onReachedEndOfWorld(Actor columbus, float x, float y, int borderFlags) {
        if ((borderFlags & FlatRectWorld.BORDER_FLAG_BOTTOM) != 0) {
            columbus.onReachedEndOfWorld();
        }
    }

    @Override
    public void onLeftWorld(Actor jesus, int borderFlags) {
        jesus.onLeaveWorld();
    }

    @Override
    public void onMoverStateChange(Actor actor) {

    }

    @Override
    public void onCollision(Actor colliding1, Actor colliding2) {
        colliding1.onCollision(colliding2);
        colliding2.onCollision(colliding1);
    }

    private static class BeamBallLook extends Look {
        private final Bitmap mBall;
        private BeamBall mBeamBall;
        private final Paint mPaint;

        protected BeamBallLook(Bitmap ball, Paint paint) {
            mPaint = paint;
            mBall = ball;
        }

        public static Paint makePaint(int color, int widthPixels) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setAntiAlias(true); // or better not because PIXEL ART?!
            paint.setStrokeWidth(widthPixels);
            return paint;
        }

        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public boolean update(long updatePeriod) {
            return false;
        }

        @Override
        public void draw(Canvas canvas, float x, float y, Paint paint) {
            float ballCenterX = mBeamBall.mHitbox.getCenterX();
            float ballCenterY = mBeamBall.mHitbox.getCenterY();
            canvas.drawLine(mBeamBall.mStartX, mBeamBall.mStartY, ballCenterX, ballCenterY, mPaint);
            canvas.drawBitmap(mBall, ballCenterX - mBall.getWidth() / 2, ballCenterY - mBall.getHeight() / 2, paint);
        }

        @Override
        public void reset() {
        }
    }
    private abstract class BeamBall extends Actor {

        protected final HitboxCircle mHitbox;
        protected final int mStartX;
        protected final int mStartY;

        protected BeamBall(HitboxCircle hitbox, HitboxNewtonMover mover, BeamBallLook defaultLook, int startX, int startY) {
            super(hitbox, mover, defaultLook);
            mHitbox = hitbox;
            mStartX = startX;
            mStartY = startY;
        }
    }

    private void addToDrawLog(int paintId, int paintGeometry, int mainX, int mainY, int secondX, int secondY) {

    }

    //TODO make explosion also show the corresponding layer for the current explosion size (only if generator working)
    //TODO difficulty counter, generator that generates shield at certain value (protects from white beams total reconstruction and city destruction)
    // TODO save and load for this make a meteor log that registers changes to background layer,
    // TODO save cannon and current meteor states too so no punishment is needed like in jumper

    //TODO make green beam clear green part of bonus layer too (e.t.c)
    private class Cannon extends Actor {
        private static final int STATE_LOADED = 0;
        private static final int STATE_LOADING_2 = 1;
        private static final int STATE_LOADING_1 = 2;
        private static final int STATE_JUST_SHOT = 3;
        private static final int LOADING_STATES_COUNT = 3;

        private int mLoadedState;
        private final int mCannonShootStartOffsetX;
        private final int mCannonShootStartOffsetY;
        private long mLoadingStateCounter;

        public Cannon(Hitbox hitbox, HitboxMover mover, Bitmap[] lookLoaded, int shootStartOffsetX, int shootStartOffsetY) {
            super(hitbox, mover, new Frames(lookLoaded, CANNON_LOADED_FRAME_DURATION));
            setActive(true);
            putStateFrames(STATE_LOADED, mCurrentLook);
            mCannonShootStartOffsetX = shootStartOffsetX;
            mCannonShootStartOffsetY = shootStartOffsetY;
            offsetLook(mCurrentLook);
        }

        private Look offsetLook(Look look) {
            look.setOffset(0, -getHitbox().getBoundingRect().height() / 2);
            return look;
        }

        private void initLoadingLooks(Bitmap[] loading1, Bitmap[] loading2, Bitmap[] loadingJustShot) {
            putStateFrames(STATE_LOADING_1, offsetLook(new Frames(loading1, CANNON_FRAME_DURATION)));
            putStateFrames(STATE_LOADING_2, offsetLook(new Frames(loading2, CANNON_FRAME_DURATION)));
            putStateFrames(STATE_JUST_SHOT, offsetLook(new Frames(loadingJustShot, CANNON_FRAME_DURATION)));
        }

        @Override
        public boolean update(long updatePeriod) {
            boolean result = super.update(updatePeriod);
            if (mLoadedState > STATE_LOADED) {
                mLoadingStateCounter -= updatePeriod;
                if (mLoadingStateCounter <= 0L) {
                    mLoadingStateCounter = (long) (mReloadTimeInterpolator.interpolate(Math.min(mDifficulty, DIFFICULTY_ULTRA_AT)) / LOADING_STATES_COUNT);
                    mLoadedState--;
                    setStateFrames(mLoadedState);
                }
            }
            return result;
        }

        private void shoot(float targetX, float targetY) {
            if (isLoaded()) {
                mLoadedState = STATE_JUST_SHOT;
                setStateFrames(mLoadedState);
                int startX = (int) (getHitbox().getCenterX() + mCannonShootStartOffsetX);
                int startY = (int) (getHitbox().getCenterY() + mCannonShootStartOffsetY);
                HitboxCircle ballHitbox = new HitboxCircle(startX, startY, mCannonBallRadiusPixels);
                HitboxNewtonMover mover = new HitboxNewtonMover(targetX - startX, targetY - startY, mCannonBallSpeed);
                long timeout = (long) (Math.sqrt(distSquared(startX, startY, targetX, targetY)) / mCannonBallSpeed * ONE_SECOND);
                BeamBallLook look = new BeamBallLook(mCannonBall, BeamBallLook.makePaint(Color.CYAN, mCannonBallBeamWidthPixels));
                CannonBall ball = new CannonBall(this, ballHitbox, mover, look, startX, startY, timeout);
                mFlatWorld.addActor(ball);
            }
        }

        public boolean isLoaded() {
            return mLoadedState == STATE_LOADED;
        }
    }

    private float distSquared(float x1, float y1, float x2, float y2) {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    }

    private Cannon findAvailableCannon(float atX, float atY) {
        float distToLeft2 = distSquared(atX, atY, mCannonLeft.getHitbox().getCenterX(), mCannonLeft.getHitbox().getCenterY());
        float distToRight2 = distSquared(atX, atY, mCannonRight.getHitbox().getCenterX(), mCannonRight.getHitbox().getCenterY());
        if (mCannonLeft.isLoaded() && mCannonRight.isLoaded()) {
            return distToLeft2 < distToRight2 ? mCannonLeft : mCannonRight;
        } else if (mCannonLeft.isLoaded()) {
            return mCannonLeft;
        } else if (mCannonRight.isLoaded()) {
            return mCannonRight;
        }
        return null;
    }

    private class CannonBall extends BeamBall {

        private final Cannon mCannon;
        private long mTimeout;
        private long mExplosionDuration;
        private CircleLook mExplosionLook;
        private final float mBaseRadius;

        private CannonBall(Cannon cannon, HitboxCircle hitbox, HitboxNewtonMover mover, BeamBallLook defaultLook, int startX, int startY, long timeout) {
            super(hitbox, mover, defaultLook, startX, startY);
            defaultLook.mBeamBall = this;
            mCannon = cannon;
            mTimeout = timeout;
            mBaseRadius = mHitbox.getRadius();
            setActive(true);
        }

        @Override
        public boolean onCollision(Actor with) {
            if (with instanceof Meteor) {
                startExplode();
                return true;
            }
            return false;
        }

        @Override
        public boolean update(long updatePeriod) {
            boolean result = super.update(updatePeriod);
            if (mTimeout > 0L) {
                mTimeout -= updatePeriod;
                if (mTimeout <= 0L) {
                    startExplode();
                }
            }
            if (mExplosionDuration > 0L) {
                mExplosionDuration -= updatePeriod;
                if (mExplosionDuration <= 0L) {
                    mFlatWorld.removeActor(this);
                } else {
                    expandExplosion();
                }
            }
            return result;
        }

        private void startExplode() {
            if (mExplosionDuration == 0L) {
                mTimeout = 0L;
                mExplosionDuration = CANNONBALL_EXPLOSION_DURATION;
                mExplosionLook = new CircleLook(mHitbox.getRadius(), CANNONBALL_EXPLOSION_COLOR);
                setMover(HitboxNoMover.INSTANCE);
                putStateFrames(0, mExplosionLook);
                setStateFrames(0);
            }
        }

        private void expandExplosion() {
            float currRadius = mBaseRadius
                    * (1.f +
                        (CANNONBALL_EXPLOSION_MAX_GROWTH_FACTOR - 1.f)
                                * (1.f - mExplosionDuration / (float) CANNONBALL_EXPLOSION_DURATION));
            mExplosionLook.setRadius(currRadius);
            mHitbox.setRadius(currRadius);
        }

        @Override
        public void onLeaveWorld() {
            mFlatWorld.removeActor(this);
        }
    }

    private static final int COLOR_TYPE_RED = 0;
    private static final int COLOR_TYPE_GREEN = 1;
    private static final int COLOR_TYPE_BLUE = 2;
    private static final int COLOR_TYPE_BONUS = 3; // bonus always as last type index as it is treated differently in some cases
    private static final int COLOR_TYPES_COUNT = 4;
    private static final int[] COLOR_TYPES = new int[] {COLOR_TYPE_RED, COLOR_TYPE_GREEN, COLOR_TYPE_BLUE, COLOR_TYPE_BONUS};
    public static int colorTypeToColor(int colorType) {
        switch (colorType) {
            case COLOR_TYPE_RED:
                return 0xFFFF0000;
            case COLOR_TYPE_GREEN:
                return 0xFF00FF00;
            case COLOR_TYPE_BLUE:
                return 0xFF0000FF;
            case COLOR_TYPE_BONUS:
                return 0xFFFFFFFF;
        }
        return 0;
    }

    protected Meteor makeMeteor(int startX, int startY, int targetX, int targetY, int colorType) {
        HitboxCircle hitbox = new HitboxCircle(startX, startY, mMeteorRadiusPixels);
        HitboxNewtonMover mover = new HitboxNewtonMover(targetX - startX, targetY - startY, mMeteorSpeedPixels);
        BeamBallLook look = new BeamBallLook(mMeteorBalls[colorType], BeamBallLook.makePaint(colorTypeToColor(colorType), mMeteorBeamWidthPixels));
        return new Meteor(hitbox, mover, look, startX, startY, colorType);
    }

    private class Meteor extends BeamBall {
        private final int mColorType;
        private Meteor(HitboxCircle hitbox, HitboxNewtonMover mover, BeamBallLook defaultLook, int startX, int startY, int colorType) {
            super(hitbox, mover, defaultLook, startX, startY);
            setActive(true);
            defaultLook.mBeamBall = this;
            mColorType = colorType;
        }

        @Override
        public boolean onCollision(Actor with) {
            if (with instanceof CannonBall) {
                mLayersCanvas[mColorType].drawLine(mStartX, mStartY, mHitbox.getCenterX(), mHitbox.getCenterY(), mColorTypePaints[mColorType]);
                mRefreshLayers = true;
                mFlatWorld.removeActor(this);
                onMeteorDestroyed();
                return true;
            }
            return false;
        }

        @Override
        public void onReachedEndOfWorld() {
            if (!mProtected) {
                // exploding at bottom if not protected
                mCityDestructionCanvas.drawLine(mHitbox.getCenterX(), mHitbox.getCenterY() - mCityOffsetY, mStartX, mStartY - mCityOffsetY, mMeteorDestructionPaint);
                mCityDestructionCanvas.drawCircle(mHitbox.getCenterX(), mHitbox.getCenterY() - mCityOffsetY, mHitbox.getRadius() * METEOR_EXPLOSION_RADIUS_FACTOR, mMeteorDestructionPaint);
            }
            onLeaveWorld();
        }

        @Override
        public void onLeaveWorld() {
            mFlatWorld.removeActor(this);
            clearTrail();
        }

        private void clearTrail() {
            if (mColorType == COLOR_TYPE_BONUS && !mProtected) {
                //clear from all layers if not protected
                for (int type : COLOR_TYPES) {
                    mLayersCanvas[type].drawLine(mStartX, mStartY, mHitbox.getCenterX(), mHitbox.getCenterY(), mClearColorTypePaint);
                }
            } else {
                mLayersCanvas[mColorType].drawLine(mStartX, mStartY, mHitbox.getCenterX(), mHitbox.getCenterY(), mClearColorTypePaint);
            }
            mRefreshLayers = true;
            onMeteorClearedItsTrail();
        }
    }

    private void onMeteorClearedItsTrail() {
        mDifficulty -= DIFFICULTY_POINTS_LOSS_PER_DIFFICULTY * (int) ((mDifficulty - 1.) / 10. + 1.);
        if (mDifficulty < 0) {
            mDifficulty = 0;
        }
        onDifficultyUpdated();
    }

    private void onDifficultyUpdated() {
        if (mDifficulty >= DIFFICULTY_AT_BEGINNING && !mProtected) {
            setCityProtected(true);
        } else if (mDifficulty < DIFFICULTY_AT_BEGINNING && mProtected) {
            setCityProtected(false);
        }
    }

    private void onMeteorDestroyed() {
        mDifficulty += DIFFICULTY_POINTS_GAIN_ON_METEOR_KILL;
        onDifficultyUpdated();
    }

    private void setCityProtected(boolean protect) {
        if (!mProtected && protect) {
            mProtected = true;
        } else if (mProtected && !protect) {
            mProtected = false;
        }
    }
}