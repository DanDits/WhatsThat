package dan.dit.whatsthat.util.flatworld.look;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 26.06.15.
 */
public class NinePatchLook extends Look {
    private static final int PADDING_LR = 30;
    private static final int PADDING_TB = 30;
    private int mHeight;
    private int mWidth;
    private final NinePatchDrawable mNinePatch;
    private TextPaint mTextPaint;
    private StaticLayout mTextLayout;
    private Rect mTextBounds;
    private Bitmap mBitmap;

    public NinePatchLook(NinePatchDrawable drawable, int screenDensity) {
        mNinePatch = drawable;
        mWidth = mNinePatch.getIntrinsicWidth();
        mHeight = mNinePatch.getIntrinsicHeight();

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(ImageUtil.convertDpToPixel(15.f, screenDensity));
        mTextBounds = new Rect();
    }

    private void setBounds(int width, int height) {
        mWidth = Math.max(width + PADDING_LR, mNinePatch.getMinimumWidth());
        mHeight = Math.max(height + PADDING_TB, mNinePatch.getMinimumHeight());
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public boolean update(long updatePeriod) {
        return false;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, Paint paint) {
        x += mOffsetX;
        y += mOffsetY;
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, x, y, paint);
        }
    }

    @Override
    public void reset() {
        mNinePatch.clearColorFilter();
        mNinePatch.setAlpha(255);
        mTextPaint.setAlpha(255);
    }

    public void setText(String text, int maxWidth) {
        String[] lines = text.split("\n");
        int max = 0;
        for (String s : lines) {
            mTextPaint.getTextBounds(s, 0, s.length(), mTextBounds);
            max = Math.max(max, mTextBounds.width());
        }
        if (maxWidth > 0) {
            max = Math.min(maxWidth, max);
        }
        mTextLayout = new StaticLayout(text, mTextPaint, max, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        setBounds(mTextLayout.getWidth(), mTextLayout.getHeight());
        updateBitmap();
    }

    private void updateBitmap() {
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas mBitmapCanvas = new Canvas(mBitmap);
        mNinePatch.setBounds(0, 0, mWidth, mHeight);
        mNinePatch.draw(mBitmapCanvas);
        drawText(mBitmapCanvas, PADDING_LR / 2, PADDING_TB / 2);
    }

    private void drawText(Canvas canvas, float x, float y) {
        if (mTextLayout != null) {
            canvas.save();
            canvas.translate(x, y);
            mTextLayout.draw(canvas);
            canvas.restore();
        }
    }


    public static NinePatchDrawable loadNinePatch(Resources res, int id){
        Bitmap bitmap = BitmapFactory.decodeResource(res, id);
        byte[] chunk = bitmap.getNinePatchChunk();
        return new NinePatchDrawable(res, bitmap,
                chunk, new Rect(), null);
    }
}
