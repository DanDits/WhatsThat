package dan.dit.whatsthat.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.mosaic.data.BitmapSource;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * Provides a source for the mosaic library. This is a cache of bitmaps
 * for images identified by their hash. Should be closed as soon as it is no longer needed since
 * this can and will take up lots of memory.
 * Created by daniel on 01.07.15.
 */
public class ImageBitmapSource implements BitmapSource<String> {
    private final Map<String, Image> mImages;
    private Resources mRes;
    private LruCache<String, List<Bitmap>> mBitmapCache;
    private Dimension mDimension = new Dimension(0, 0);

    public ImageBitmapSource(Resources res, Map<String, Image> images) {
        mRes = res;
        mImages = images;
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 10;

        mBitmapCache = new LruCache<String, List<Bitmap>>(cacheSize) {
            @Override
            protected int sizeOf(String key, List<Bitmap> bitmaps) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                int size = 0;
                for (Bitmap bitmap : bitmaps) {
                    size += bitmap.getByteCount() / 1024;
                }
                return size;
            }
        };
    }

    @Override
    public Bitmap getBitmap(MosaicTile<String> forTile, int requiredWidth, int requiredHeight) {
        String hash = forTile.getSource();
        List<Bitmap> cachedForImage = mBitmapCache.get(hash);
        if (cachedForImage != null) {
            Bitmap bigger = null;
            for (Bitmap bitmap : cachedForImage) {
                if (bitmap.getWidth() == requiredWidth &&
                         bitmap.getHeight() == requiredHeight) {
                    return bitmap; // found exact match, yeah!
                } else if (bitmap.getWidth() >= requiredWidth && bitmap.getHeight() >= requiredHeight ) {
                    //Log.d("HomeStuff", "Found bigger bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight() + " instead of " + requiredWidth + "x" + requiredHeight);
                    bigger = bitmap;
                }
            }
            if (bigger != null) {
                Bitmap result = BitmapUtil.resize(bigger, requiredWidth, requiredHeight);
                if (result != null) {
                    cachedForImage.add(result);
                    mBitmapCache.put(hash, cachedForImage);
                    return result;
                }
            }
        }
        // found no exact match, no bigger image or could not resize it, so load a new one
        Image image = mImages.get(hash);
        if (image != null) {
            mDimension.set(requiredWidth, requiredHeight);
            Bitmap result = image.loadBitmap(mRes, mDimension, true);
            if (result != null) {
                if (cachedForImage == null) {
                    cachedForImage = new LinkedList<>();
                }
                cachedForImage.add(result);
                mBitmapCache.put(hash, cachedForImage);
                return result;
            }
        }
        return null;
    }
}
