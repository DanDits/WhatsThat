package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;
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

    private static final int HIDDEN_COLOR = 0xff110f0f;
    private static final long DRAWPAUSETIME = 300;//ms


    int x;
    int y;
    Bitmap flame;
    private Paint paint;
    private Paint glowpaint;
    long Lasttimedrawn;

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
        int Radius = 90;
        int InnerRadius = 30;
        int VisibleLeft = x-Radius;
        int VisibleTop = y-Radius;
        int Left = x-flame.getWidth()/2;
        int Top = y-flame.getHeight()/2;


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
        }
        canvas.drawBitmap(flame,Left,Top,null);

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
        flame = ImageUtil.loadBitmap(res, R.drawable.lagerfeuer,mBitmap.getWidth()/5,mBitmap.getHeight()/5,false);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        x = (int) event.getX();
        y = (int) event.getY();
        Log.d("Fabi", "Bewegt zu " + x + "/" + y);

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
        return null;
    }



}
