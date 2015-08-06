package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by Fabian on 03.08.2015.
 */
public class RiddleTorchlight extends RiddleGame {

    private static final int HIDDEN_COLOR = Color.BLACK;
    private static final long DRAWPAUSETIME = 50;//ms
    private static final float GLOW_RADIUS = 90.f;


    private int x;
    private int y;
    private Bitmap flame;
    private Paint paint;
    private Paint glowpaint;
    private long Lasttimedrawn;
    private Paint glowToDarkPaint;
    private Bitmap mTorchlightEffect;

    public RiddleTorchlight(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    protected int calculateGainedScore() {
        return DEFAULT_SCORE;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPaint(paint);
        canvas.drawCircle(x, y, GLOW_RADIUS, glowpaint);
        canvas.drawBitmap(mTorchlightEffect, x - mTorchlightEffect.getWidth() / 2 - 2, y - mTorchlightEffect.getHeight() / 2 - 2, glowToDarkPaint);
        canvas.drawBitmap(flame, x - flame.getWidth() / 2, y - flame.getHeight() / 2, null);

        /*int Radius = (int) GLOW_RADIUS;
        int VisibleLeft = x-Radius;
        int VisibleTop = y-Radius;


        for (int i = Math.max(VisibleLeft, 0); i< Math.min(x+Radius,mBitmap.getWidth());i++){
            for (int j = Math.max(VisibleTop,0); j< Math.min(y+Radius, mBitmap.getHeight());j++){
                float distance = (float)Math.sqrt((i-x)*(i-x)+(j-y)*(j-y));
                if (distance <= Radius) {
                    glowpaint.setColor(mBitmap.getPixel(i, j));
                    if (distance <= InnerRadius) {
                        glowpaint.setAlpha(255);
                    }else {
                        int alpha = (int) (255/ (float) (InnerRadius-Radius)*distance+((255*Radius)/ (float) (Radius-InnerRadius)));
                        glowpaint.setAlpha(alpha);
                    }
                    canvas.drawPoint(i,j,glowpaint);
                }
            }
        }*/
        /*canvas.drawBitmap(mBitmap,0,0,null);

        for (int i = 0; i<mBitmap.getWidth();i++){
            for (int j = 0; j<mBitmap.getHeight();j++){
                if (Math.sqrt((i-x)*(i-x)+(j-y)*(j-y)) > 70){
                    canvas.drawPoint(i,j,paint);
                }
            }
        }

    //    canvas.drawBitmap(mBitmap,x-mBitmap.getWidth()/2,y-mBitmap.getHeight()/2,null);*/
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        x = mConfig.mWidth/2;
        y = mConfig.mHeight/2;
        paint = new Paint();
        paint.setColor(HIDDEN_COLOR);
        glowpaint = new Paint();
        glowpaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        glowToDarkPaint = new Paint();
        mTorchlightEffect = ImageUtil.loadBitmap(res, R.drawable.torchlight_effect, (int) (GLOW_RADIUS * 2) + 5, (int) (GLOW_RADIUS * 2) + 5, true);
        glowToDarkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
        flame = ImageUtil.loadBitmap(res, R.drawable.lagerfeuer,mBitmap.getWidth()/5,mBitmap.getHeight()/5,false);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        x = (int) event.getX();
        y = (int) event.getY();

        if (event.getActionMasked()==MotionEvent.ACTION_DOWN || event.getActionMasked()==MotionEvent.ACTION_UP) {
            return true;
        } else if (event.getActionMasked()==MotionEvent.ACTION_MOVE){
            long currenttime = System.currentTimeMillis();
            if (currenttime-Lasttimedrawn > DRAWPAUSETIME){
                Lasttimedrawn = currenttime;
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



}
