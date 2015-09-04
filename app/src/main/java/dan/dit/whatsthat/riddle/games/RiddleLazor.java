package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * //TODO implement game
 * Created by daniel on 04.09.15.
 */
public class RiddleLazor extends RiddleGame {

    public RiddleLazor(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    public void draw(Canvas canvas) {

    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {

    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        return false;
    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return "";
    }
}
