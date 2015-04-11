package dan.dit.whatsthat.intro;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 11.04.15.
 */
public class IntroAlienView extends SurfaceView {

    private SurfaceHolder mHolder;
    private Bitmap mAlien;

    public IntroAlienView(Context context) {
        super(context);
        init();
    }

    public IntroAlienView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }




    public void init() {
        mHolder = getHolder();

        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas c = holder.lockCanvas(null);
                onDraw(c);
                holder.unlockCanvasAndPost(c);

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {

            }
        });
        mAlien = BitmapFactory.decodeResource(getResources(), R.drawable.abduction);

    }



    @Override

    protected void onDraw(Canvas canvas) {

        canvas.drawBitmap(mAlien, 10, 10, null);

    }
}
