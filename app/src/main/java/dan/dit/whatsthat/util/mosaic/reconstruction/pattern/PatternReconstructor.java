package dan.dit.whatsthat.util.mosaic.reconstruction.pattern;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;
import dan.dit.whatsthat.util.mosaic.reconstruction.RectReconstructor;

/**
 * Created by daniel on 05.12.15.
 */
public abstract class PatternReconstructor extends RectReconstructor {
    public static final Paint CLEAR_PAINT = new Paint();
    static {
        CLEAR_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }
    public PatternReconstructor(Bitmap source, int wantedRows, int
            wantedColumns, int groundingColor) {
        super(source, wantedRows, wantedColumns);
        mResultCanvas.drawPaint(CLEAR_PAINT);
        mResultCanvas.drawColor(groundingColor);
    }

    // this is invoked by parent constructor, not best practice as subclass constructor not yet
    // initialized (missing members ...!)
    protected abstract int evaluateRectValue(Bitmap source, int startX, int endX, int startY, int
            endY);

    public abstract <S> PatternSource<S> makeSource();

    public abstract <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric);
}
