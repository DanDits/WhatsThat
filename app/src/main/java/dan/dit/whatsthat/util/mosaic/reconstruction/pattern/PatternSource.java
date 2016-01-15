package dan.dit.whatsthat.util.mosaic.reconstruction.pattern;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.LruCache;

import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.mosaic.data.BitmapSource;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * Created by daniel on 05.12.15.
 */
public abstract class PatternSource<S> implements BitmapSource<S> {

    private static final int DEFAULT_CACHE_SIZE = 10;
    private final LruCache<Integer, Bitmap> mCache;

    public PatternSource() {
        mCache = new LruCache<>(getCacheSizeHint());
    }

    public int getCacheSizeHint() {
        return DEFAULT_CACHE_SIZE;
    }

    public Bitmap getBitmap(MosaicTile<S> forTile, int requiredWidth, int requiredHeight) {
        int keyColor = forTile.getAverageARGB();
        return makePattern(keyColor, obtainBitmap(keyColor, requiredWidth, requiredHeight));
    }

    protected abstract Bitmap makePattern(int color, @NonNull Bitmap base);

    protected @NonNull
    Bitmap obtainBitmap(int key, int width, int height) {
        Bitmap cached = mCache.get(key);
        if (cached != null && cached.getWidth() == width && cached.getHeight() == height) {
            return cached;
        }
        ImageUtil.CACHE.makeReusable(cached);
        cached = ImageUtil.CACHE.getReusableBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (cached == null) {
            cached = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Bitmap old = mCache.put(key, cached);
        ImageUtil.CACHE.makeReusable(old);
        return cached;
    }
}
