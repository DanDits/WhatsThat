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
import dan.dit.whatsthat.util.flatworld.collision.GeneralHitboxCollider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.collision.HitboxCircle;
import dan.dit.whatsthat.util.flatworld.collision.HitboxGhostPoint;
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
    private static final long TIME_TILL_NEXT_METEOR = 5000L; //ms, temporary solution
    private static final float METEOR_BEAM_WIDTH_FRACTION_OF_SCREEN_DIAGONAL = 0.015f;
    private static final float METEOR_RADIUS_FRACTION_OF_SCREEN_DIAGONAL = 0.02f;
    private static final float METEOR_DIAGONAL_DURATION = 10000.f; //ms, time for a meteor that moves directly from top left to bottom right
    private static final float ONE_SECOND = 1000.f; // ms, one second, fixed
    private static final double CHANCE_FOR_BONUS_BEAM = 0.1;
    private static final float CANNON_BEAM_FRACTION_OF_SCREEN_DIAGONAL = 0.005f;
    private static final float CANNONBALL_RADIUS_FRACTION_OF_SCREEN_DIAGONAL = 0.03f;
    private static final float CANNONBALL_DIAGONAL_DURATION = 10000.f; //ms
    private static final long CANNON_DEFAULT_RELOAD_DURATION = Cannon.LOADING_STATES_COUNT * 1000L;
    private static final long CANNONBALL_EXPLOSION_DURATION = 2000L;
    private static final float CANNONBALL_EXPLOSION_MAX_GROWTH_FACTOR = 2.f;
    private static final long CANNON_FRAME_DURATION = 100L;//ms

    private FlatRectWorld mFlatWorld;
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
    private long mCannonReloadDuration;
    private Cannon mCannonLeft;
    private Cannon mCannonRight;
    private Paint mClearColorTypePaint;


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
        canvas.drawBitmap(mWorldBitmap, 0, 0, null);
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
        mCityLayerCanvas.drawBitmap(ImageUtil.loadBitmap(res, R.drawable.skylinecity, mCityLayer.getWidth(), mCityLayer.getHeight(), BitmapUtil.MODE_FIT_EXACT), 0, 0, null);
        initMeteorData(res);
        initCannonData(res);
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
        mMeteorBalls[COLOR_TYPE_BONUS] = ImageUtil.loadBitmap(res, R.drawable.laser_alpha, size, size, BitmapUtil.MODE_FIT_EXACT);
        Log.d("Riddle", "Meteor data initialized: beam width=" + mMeteorBeamWidthPixels + " , speed=" + mMeteorSpeedPixels + " , radius=" + mMeteorRadiusPixels);
    }

    private void initCannonData(Resources res) {
        mCannonReloadDuration = CANNON_DEFAULT_RELOAD_DURATION;
        mCannonBallRadiusPixels = (int) (mDiagonal * CANNONBALL_RADIUS_FRACTION_OF_SCREEN_DIAGONAL);
        mCannonBallRadiusPixels = Math.max(1, mCannonBallRadiusPixels);
        mCannonBallBeamWidthPixels = (int) (mDiagonal * CANNON_BEAM_FRACTION_OF_SCREEN_DIAGONAL);
        mCannonBallBeamWidthPixels = Math.max(1, mCannonBallBeamWidthPixels);
        mCannonBallSpeed = mDiagonal / (CANNONBALL_DIAGONAL_DURATION / ONE_SECOND);
        int size = (int) (mCannonBallRadiusPixels * 2);
        mCannonBall = ImageUtil.loadBitmap(res, R.drawable.laser_alpha, size, size, BitmapUtil.MODE_FIT_EXACT);

        float cannonWallOffset = ImageUtil.convertDpToPixel(5.f, mConfig.mScreenDensity);
        float cannonRadius = cannonWallOffset * 4;
        int cannonSize = (int) (cannonRadius * 2);
        Bitmap[] loadedFrames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_loaded_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};
        Bitmap[] loading1Frames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_level1_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_level1_frame2, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};
        Bitmap[] loading2Frames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_level2_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT),
                ImageUtil.loadBitmap(res, R.drawable.lazor_level2_frame2, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};

        Bitmap[] firedFrames = new Bitmap[] {ImageUtil.loadBitmap(res, R.drawable.lazor_fired_frame1, cannonSize, cannonSize, BitmapUtil.MODE_FIT_EXACT)};



        mCannonLeft = new Cannon(new HitboxCircle(cannonWallOffset + cannonRadius, mConfig.mHeight - cannonRadius - cannonWallOffset, cannonRadius),
                HitboxNoMover.INSTANCE, loadedFrames,
                (int) cannonRadius, - (int) cannonRadius);
        mCannonLeft.initLoadingLooks(loading1Frames, loading2Frames, firedFrames);
        mFlatWorld.addActor(mCannonLeft);
        mCannonRight = new Cannon(new HitboxCircle(mConfig.mWidth - cannonRadius - cannonWallOffset, mConfig.mHeight - cannonRadius - cannonWallOffset, cannonRadius),
                HitboxNoMover.INSTANCE, loadedFrames,
                - (int) cannonRadius, - (int) cannonRadius);
        mCannonRight.initLoadingLooks(loading1Frames, loading2Frames, firedFrames);
        mFlatWorld.addActor(mCannonRight);
    }

    private void setMeteorBeamWidthPixels(int beamWidthPixels) {
        mMeteorBeamWidthPixels = Math.max(1, beamWidthPixels); //at least one pixel width
        for (Paint mColorTypePaint : mColorTypePaints) {
            mColorTypePaint.setStrokeWidth(mMeteorBeamWidthPixels);
        }
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
            mNextMeteorDuration = TIME_TILL_NEXT_METEOR;//TODO make function dependant on current difficulty and maybe some randomness
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

    private class Cannon extends Actor {
        private static final int STATE_LOADED = 0;
        private static final int STATE_LOADING_1 = 1;
        private static final int STATE_LOADING_2 = 2;
        private static final int STATE_JUST_SHOT = 3;
        private static final int LOADING_STATES_COUNT = 3;

        private int mLoadedState;
        private final int mCannonShootStartOffsetX;
        private final int mCannonShootStartOffsetY;
        private long mLoadingStateCounter;

        public Cannon(Hitbox hitbox, HitboxMover mover, Bitmap[] lookLoaded, int shootStartOffsetX, int shootStartOffsetY) {
            super(hitbox, mover, new Frames(lookLoaded, CANNON_FRAME_DURATION));
            setActive(true);
            putStateFrames(STATE_LOADED, mCurrentLook);
            mCannonShootStartOffsetX = shootStartOffsetX;
            mCannonShootStartOffsetY = shootStartOffsetY;
        }

        private void initLoadingLooks(Bitmap[] loading1, Bitmap[] loading2, Bitmap[] loadingJustShot) {
            putStateFrames(STATE_LOADING_1, new Frames(loading1, CANNON_FRAME_DURATION));
            putStateFrames(STATE_LOADING_2, new Frames(loading2, CANNON_FRAME_DURATION));
            putStateFrames(STATE_JUST_SHOT, new Frames(loadingJustShot, CANNON_FRAME_DURATION));
        }

        @Override
        public boolean update(long updatePeriod) {
            boolean result = super.update(updatePeriod);
            if (mLoadedState > STATE_LOADED) {
                mLoadingStateCounter -= updatePeriod;
                if (mLoadingStateCounter <= 0L) {
                    mLoadingStateCounter = mCannonReloadDuration / LOADING_STATES_COUNT;
                    mLoadedState--;
                    boolean newlook = setStateFrames(mLoadedState);
                    Log.d("Riddle", "Loaded state: " + mLoadedState + " left cannon ? " + (this == mCannonLeft) + " new look? " + newlook);
                }
            }
            return result;
        }

        private void shoot(float targetX, float targetY) {
            if (isLoaded()) {
                Log.d("Riddle", "Started shooting to " + targetX + "/" + targetY);
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
            Log.d("Riddle", "Setting timeout for new cannon ball: " + mTimeout);
            mBaseRadius = mHitbox.getRadius();
            setActive(true);
        }

        @Override
        public boolean onCollision(Actor with) {
            if (with instanceof Meteor) {
                Log.d("Riddle", "Cannon ball and meteor collided at " + mHitbox.getCenterX() + "/" + mHitbox.getCenterY());
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
                Log.d("Riddle", "Started exploding cannon ball.");
                mTimeout = 0L;
                mExplosionDuration = CANNONBALL_EXPLOSION_DURATION;
                mExplosionLook = new CircleLook(mHitbox.getRadius(), Color.GRAY);
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
            Log.d("Riddle", "Cannon ball belong to " + mCannon + " left world: ");
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
        Log.d("Riddle", "Made meteor at " + startX + " moving to " + targetX + " of color type " + colorType);
        return new Meteor(hitbox, mover, look, startX, startY, colorType);
    }

    private class Meteor extends BeamBall {
        private final int mColorType;
        private boolean mStateDestroyed;
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
                mStateDestroyed = true;
                mFlatWorld.removeActor(this);
                return true;
            }
            return false;
        }

        @Override
        public void onLeaveWorld() {
            mFlatWorld.removeActor(this);
            mLayersCanvas[mColorType].drawLine(mStartX, mStartY, mHitbox.getCenterX(), mHitbox.getCenterY(), mClearColorTypePaint);
            mRefreshLayers = true;
        }
    }
}
