package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.flatworld.collision.GeneralHitboxCollider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;
import dan.dit.whatsthat.util.flatworld.world.Actor;
import dan.dit.whatsthat.util.flatworld.world.FlatRectWorld;
import dan.dit.whatsthat.util.flatworld.world.FlatWorld;
import dan.dit.whatsthat.util.flatworld.world.FlatWorldCallback;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Idea: Spawn random not burning torch somewhere. Task is to bring fire to the torch by moving
 * finger from the start mother torch to that torch. Moving too fast (take care of dpi) or touching
 * some trap (wind, water) clears the moved fire and maybe some burning torches nearby. Burning torches reveal
 * underlying image. Traps need to be remembered as they stay at the same position. No traps directly
 * near a torch. Getting mother fire creates a trap, lighting a torch clears one!? (how to prevent unsolvable
 * trap inflation)?
 * Unlock: Burning torches can be made burning stronger.
 * Created by Fabian on 03.08.2015.
 */
public class RiddleTorchlight extends RiddleGame implements FlatWorldCallback {

    private static final int HIDDEN_COLOR = Color.BLACK;
    private static final long DRAW_ON_MOVE_PAUSE_TIME = 50; //ms
    private static final float GLOW_RADIUS = 90.f;


    private int mTouchX;
    private int mTouchY;
    private Bitmap mFlame;
    private Paint mBasePaint;
    private Paint mGlowPaint;
    private long mLastTimeMoveDrawn;
    private Paint mGlowToDarkPaint;
    private Bitmap mTorchlightEffect;
    private FlatWorld mWorld;

    public RiddleTorchlight(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPaint(mBasePaint);
        canvas.drawCircle(mTouchX, mTouchY, GLOW_RADIUS, mGlowPaint);
        canvas.drawBitmap(mTorchlightEffect, mTouchX - mTorchlightEffect.getWidth() / 2 - 2, mTouchY - mTorchlightEffect.getHeight() / 2 - 2, mGlowToDarkPaint);
        canvas.drawBitmap(mFlame, mTouchX - mFlame.getWidth() / 2, mTouchY - mFlame.getHeight() / 2, null);

    }

    @Override
    public boolean requiresPeriodicEvent() {
        return true;
    }

    @Override
    public void onPeriodicEvent(long updatePeriod) {
        mWorld.update(updatePeriod);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        mTouchX = mConfig.mWidth / 2;
        mTouchY = mConfig.mHeight / 2;
        mBasePaint = new Paint();
        mBasePaint.setColor(HIDDEN_COLOR);
        mGlowPaint = new Paint();
        mGlowPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        mGlowToDarkPaint = new Paint();
        mTorchlightEffect = ImageUtil.loadBitmap(res, R.drawable.torchlight_effect, (int) (GLOW_RADIUS * 2) + 5, (int) (GLOW_RADIUS * 2) + 5, true);
        mGlowToDarkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
        mFlame = ImageUtil.loadBitmap(res, R.drawable.lagerfeuer,mBitmap.getWidth()/5,mBitmap.getHeight()/5,false);
        initWorld(res);
    }

    private void initWorld(Resources res) {
        mWorld = new FlatRectWorld(new RectF(0, 0, mConfig.mWidth, mConfig.mHeight), new GeneralHitboxCollider(), this);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        mTouchX = (int) event.getX();
        mTouchY = (int) event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_UP) {
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE){
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastTimeMoveDrawn > DRAW_ON_MOVE_PAUSE_TIME){
                mLastTimeMoveDrawn = currentTime;
                return true;
            }else {
                return false;
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
    public void onReachedEndOfWorld(Actor columbus, float x, float y, int borderFlags) {

    }

    @Override
    public void onLeftWorld(Actor jesus, int borderFlags) {

    }

    @Override
    public void onMoverStateChange(Actor actor) {

    }

    @Override
    public void onCollision(Actor colliding1, Actor colliding2) {

    }

    private class Torch extends Actor {

        public Torch(Hitbox hitbox, HitboxMover mover, Look defaultLook) {
            super(hitbox, mover, defaultLook);
        }
    }


}
