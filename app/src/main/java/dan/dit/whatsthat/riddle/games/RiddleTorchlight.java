package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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

    int x;
    int y;
    Bitmap flame;
    private Paint paint = new Paint();

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
        canvas.drawBitmap(mBitmap,0,0,null);
        int ZentrumX = x-flame.getWidth()/2;
        int ZentrumY = y-flame.getHeight()/2;

        for (int i = 0; i<mBitmap.getWidth();i++){
            for (int j = 0; j<mBitmap.getHeight();j++){
                if (Math.sqrt((i-ZentrumX)*(i-ZentrumX)+(j-ZentrumY)*(j-ZentrumY)) > 70){
                    canvas.drawPoint(i,j,paint);
                }
            }
        }
        canvas.drawBitmap(flame,x-flame.getWidth()/2,y-flame.getHeight()/2,null);
    //    canvas.drawBitmap(mBitmap,x-mBitmap.getWidth()/2,y-mBitmap.getHeight()/2,null);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
        x = mConfig.mWidth/2;
        y = mConfig.mHeight/2;
        flame = ImageUtil.loadBitmap(res, R.drawable.lagerfeuer,mBitmap.getWidth()/5,mBitmap.getHeight()/5,false);
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        x = (int) event.getX();
        y = (int) event.getY();
        return true;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return null;
    }



}
