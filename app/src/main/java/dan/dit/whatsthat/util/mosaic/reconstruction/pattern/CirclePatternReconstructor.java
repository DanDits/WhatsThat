package dan.dit.whatsthat.util.mosaic.reconstruction.pattern;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;
import dan.dit.whatsthat.util.mosaic.matching.TrivialMatcher;

/**
 * Created by daniel on 05.12.15.
 */
public class CirclePatternReconstructor extends PatternReconstructor {
    public static final String NAME = "Circles";
    private double[] mRaster;
    private double mAverageBrightness;

    public static class Source<S> extends PatternSource<S> {
        private Paint mPaint;
        private Bitmap mPatternBitmap;
        private Canvas mCanvas;
        public Source() {
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
        }
        @Override
        protected Bitmap makePattern(int color, @NonNull Bitmap base) {
            Canvas canvas = mCanvas;
            canvas.drawColor(Color.TRANSPARENT);
            mPaint.setColor(color);
            canvas.drawCircle(base.getWidth() / 2, base.getHeight() / 2,
                    Math.min(base.getWidth() / 2, base.getHeight() / 2), mPaint);
            return base;
        }

        protected @NonNull
        Bitmap obtainBitmap(int key, int width, int height) {
            if (mPatternBitmap != null && mPatternBitmap.getWidth() == width && mPatternBitmap
                    .getHeight() == height) {
                return mPatternBitmap;
            }
            ImageUtil.CACHE.makeReusable(mPatternBitmap);
            mPatternBitmap = super.obtainBitmap(key, width, height);
            mCanvas = new Canvas(mPatternBitmap);
            return mPatternBitmap;
        }
    }

    public CirclePatternReconstructor(Bitmap source, int wantedRows, int wantedColumns, int groundingColor) {
        super(source, wantedRows, wantedColumns, groundingColor);
    }

    public <S> PatternSource<S> makeSource() {
        return new Source<>();
    }

    @Override
    public <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric) {
        return new TrivialMatcher<>();
    }

    @Override
    protected int evaluateRectValue(Bitmap source, int startX, int endX, int startY, int endY) {
        ensureAverageBrightness(source);
        fillRaster(source, startX, endX, startY, endY);
        int width = endX - startX;
        int height = endY - startY;
        return calculateColor(mRaster, mAverageBrightness, width, height,
                0, 0, Math.max(width, height) + 1);
    }

    private void fillRaster(Bitmap source, int startX, int endX, int startY, int endY) {
        int width = endX - startX;
        int height = endY - startY;
        if (mRaster == null || mRaster.length != width * height) {
            mRaster = new double[width * height];
        }
        int index = 0;
        for (int j = startY; j < endY; j++) {
            for (int i = startX; i < endX; i++) {
                mRaster[index++] = ColorAnalysisUtil.getBrightnessWithAlpha(source.getPixel(i, j));
            }
        }
    }

    private void ensureAverageBrightness(Bitmap source) {
        if (mAverageBrightness > 0.) {
            return;
        }
        mAverageBrightness = 0.;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                mAverageBrightness += ColorAnalysisUtil.getBrightnessWithAlpha(source.getPixel(x,
                        y));
            }
        }
        mAverageBrightness /= source.getWidth() * source.getHeight();
        mAverageBrightness = Math.max(mAverageBrightness, Double.MIN_VALUE); // to ensure it is
        // positive and brightness not calculated over and over if it would be exactly zero
    }

    public static int calculateColor(double[] raster, double averageBrightness,
                                     int bitmapWidth, int bitmapHeight,
                                     float x, float y, float r) {
        // by default this calculates the average brightness of the area [x-r,x+r][y-r,y+r]
        int left = (int)(x - r);
        int right = (int)(x + r);
        int top = (int)(y - r);
        int bottom = (int)(y + r);
        double brightness = 0;
        double consideredPoints = 0;
        // do not only consider pixels within the circle but within the square defined by the circle bounds
        for (int i = Math.max(0, left); i <= Math.min(right, bitmapWidth - 1); i++) {
            for (int j = Math.max(0, top); j <= Math.min(bottom, bitmapHeight - 1); j++) {
                int rasterIndex = j * bitmapWidth + i;
                if (rasterIndex >= 0 && rasterIndex < raster.length) {
                    brightness += raster[rasterIndex];
                    consideredPoints++;
                }
            }
        }
        // 1 = very bright -> white
        brightness /= consideredPoints;
        // logistic filter 1/(1+e^(-kx)) to minimize grey values and emphasize bright and dark ones
        // use higher k for less grey values
        brightness = 1. / (1. + Math.exp(-15. * (brightness - averageBrightness)));
        int grey = (int) (255. * brightness);
        return ColorAnalysisUtil.toRGB(grey, grey, grey, 255);
    }
}
