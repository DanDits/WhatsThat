package dan.dit.whatsthat.util.mosaic.reconstruction.pattern;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;
import dan.dit.whatsthat.util.mosaic.matching.SimpleLinearTileMatcher;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;

/**
 * Inspired by https://github.com/JuanPotato/Legofy
 * Created by daniel on 05.12.15.
 */
public class LegoPatternReconstructor extends PatternReconstructor {
    // lego colors see: http://www.brickjournal.com/files/PDFs/2010LEGOcolorpalette.pdf
    private static final int[] LEGO_COLOR_PALETTE_SOLID = new int[] {0xff311007, 0xff2d1678,
            0xff95b90c, 0xff8d7553, 0xfff4f4f4, 0xff017c29, 0xffa83e16, 0xfffec401, 0xff4d5e57,
            0xff9c01c6, 0xfff5c189, 0xff013517, 0xff020202, 0xffaa7e56, 0xff0158a8, 0xff5f758c,
            0xffde010e, 0xffee9dc3, 0xff87c0ea, 0xfff49b01, 0xffffff99, 0xffd67341, 0xff608266,
            0xff9c9291, 0xff80091c, 0xff019625, 0xff488cc6, 0xffd9bb7c, 0xff5c1d0d, 0xffde388b,
            0xffe76419, 0xffe4e4da, 0xff012642};
    private static final int[] LEGO_COLOR_PALETTE_TRANSPARENT = new int[] {0xaaf9ef69, 0xaaeeeeee,
            0xaae76648, 0xaa50b1e8, 0xaaec760e, 0xaaa69182, 0xaab6e0ea, 0xaa9c95c7, 0xaa99ff66,
            0xaaee9dc3, 0xaa63b26e, 0xaae02a29, 0xaaf1ed5b, 0xaacee3f6};
    private static final int[] LEGO_COLOR_PALETTE_EFFECTS = new int[] {0xff8d9496, 0xffaa7f2e,
            0xfffefcd5, 0xff493f3b};


    public static final String NAME = "Lego";
    private final Bitmap mLegoBitmap;

    public LegoPatternReconstructor(Resources res, Bitmap source, int wantedRows, int
            wantedColumns, int
            groundingColor) {
        super(source, wantedRows, wantedColumns, groundingColor);
        mLegoBitmap = ImageUtil.loadBitmap(res, R.drawable.lego_blueprint, mRectWidth,
                mRectHeight, true);
    }

    public static class Source<S> extends PatternSource<S> {
        private Paint mPaint;
        public Source(Bitmap legoBitmap) {
            mPaint = new Paint();
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
            mPaint.setShader(new BitmapShader(legoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        }
        @Override
        public int getCacheSizeHint() {
            return 25;
        }
        @Override
        protected Bitmap makePattern(int color, @NonNull Bitmap base) {
            Canvas canvas = new Canvas(base);
            mPaint.setColor(color);
            canvas.drawColor(color);
            canvas.drawPaint(mPaint);
            return base;
        }

    }

    @Override
    protected int evaluateRectValue(Bitmap source, int startX, int endX, int startY, int endY) {
        return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
    }

    @Override
    public <S> PatternSource<S> makeSource() {
        return new LegoPatternReconstructor.Source<>(mLegoBitmap);
    }

    @Override
    public <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric) {
        List<MosaicTile<S>> tiles = new ArrayList<>();
        for (int value : LEGO_COLOR_PALETTE_SOLID) {
            tiles.add(new VoidTile<S>(value));
        }
        for (int value : LEGO_COLOR_PALETTE_TRANSPARENT) {
            tiles.add(new VoidTile<S>(value));
        }
        for (int value : LEGO_COLOR_PALETTE_EFFECTS) {
            tiles.add(new VoidTile<S>(value));
        }
        return new SimpleLinearTileMatcher<>(tiles, useAlpha, metric);
    }

    private static class VoidTile<S> implements MosaicTile<S> {

        private final int mLegoColor;

        public VoidTile(int color) {
            mLegoColor = color;
        }
        @Override
        public S getSource() {
            return null;
        }

        @Override
        public int getAverageARGB() {
            return mLegoColor;
        }
    }

}
