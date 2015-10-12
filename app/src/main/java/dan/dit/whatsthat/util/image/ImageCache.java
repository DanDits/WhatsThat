package dan.dit.whatsthat.util.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntegerRes;
import android.util.Log;
import android.util.LruCache;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Basic code from http://developer.android.com/training/displaying-bitmaps/manage-memory.html
 * Created by daniel on 09.10.15.
 */
public class ImageCache {
    private final Set<SoftReference<Bitmap>> mReusableBitmaps;
    private LruCache<String, Bitmap> mMemoryCache;

    public ImageCache() {
        // If you're running on Honeycomb or newer, create a
        // synchronized HashSet of references to reusable bitmaps.
        mReusableBitmaps =
            Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

        final int sizeKB = (int) (Runtime.getRuntime().maxMemory() / 1024  / 8);
        mMemoryCache = new LruCache<String, Bitmap>(sizeKB) {

            // Notify the removed entry that is no longer being cached.
            @Override
            protected void entryRemoved(boolean evicted, String key,
                    Bitmap oldValue, Bitmap newValue) {
                if (evicted || newValue != null) {
                    // if entry is removed from cache this means that it is currently being obtained by some client
                    // and cannot be reused
                    makeReusable(oldValue);
                }
            }
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    return bitmap.getAllocationByteCount() / 1024;
                }
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void makeReusable(Bitmap toRecycle) {
        if (toRecycle != null) {
            Log.d("Image", "Image made reusable: " + toRecycle.getWidth() + "/" + toRecycle.getHeight() + " " + toRecycle.getConfig());
            mReusableBitmaps.add
                    (new SoftReference<>(toRecycle));
        }
    }

    public void freeImage(String key, Bitmap image) {
        if (key == null || image == null) {
            return;
        }
        mMemoryCache.put(key, image);
    }

    public Bitmap obtainImage(String key, Resources res, @DrawableRes int resId, int reqWidth, int reqHeight, boolean enforceDimension) {
        Bitmap cached = mMemoryCache.remove(key); // will not make cached image reusable as it is not evicted or replaced!
        if (cached != null
                && ((enforceDimension && cached.getWidth() == reqWidth && cached.getHeight() == reqHeight)
                    || (!enforceDimension && cached.getWidth() <= reqWidth && cached.getHeight() <= reqHeight))) {
            Log.d("Image", "Cache hit: "+  key + " found " + cached + " options " + reqWidth + "/" + reqHeight + " " + enforceDimension);
            return cached; // valid cached image with proper fitting dimensions
        }
        makeReusable(cached);
        return ImageUtil.loadBitmap(res, resId, reqWidth, reqHeight, enforceDimension);
    }

    public Bitmap getReusableBitmap(int width, int height, Bitmap.Config config) {
        Bitmap bitmap = null;
        Log.d("Image", "Searching for reusable bitmap: " + width + "/" + height + " config: " + config
            + " has: " + mReusableBitmaps.size());

        if (!mReusableBitmaps.isEmpty()) {
            synchronized (mReusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator
                        = mReusableBitmaps.iterator();
                Bitmap item;

                while (iterator.hasNext()) {
                    item = iterator.next().get();

                    if (null != item && item.isMutable()) {
                        // Check to see it the item can be used
                        if (item.getWidth() >= width && item.getHeight() >= height) {
                            bitmap = item;
                            if (config != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                try {
                                    bitmap.setConfig(config);
                                } catch (IllegalStateException e) {
                                    // was a native bitmap that could not be reconfigured
                                    bitmap = null;
                                }
                            } else if (item.getWidth() != width || item.getHeight() != height
                                    || (config != null && !config.equals(bitmap.getConfig()))) {
                                // we could not reconfigure and the dimensions do not fit exactly or config is different
                                Log.d("Image", "Bitmap not fitting: " + item.getWidth() + "/" + item.getHeight() + " config: " + item.getConfig());
                                bitmap = null;
                            }
                            if (bitmap != null) {
                                // Remove from reusable set so it can't be used again.
                                iterator.remove();
                                break;
                            }
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        Log.d("Image", "Reusable bitmap was cleared.");
                        iterator.remove();
                    }
                }
            }
        }
        if (bitmap != null) {
            Log.d("Image", "Found reusuable bitmap for " + width + "/" + height);
        }
        return bitmap;
    }

    public boolean addInBitmapOptions(BitmapFactory.Options options) {
        if (options == null || options.outHeight <= 0 || options.outWidth <= 0) {
            return false;
        }
        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;

        // Try to find a bitmap to use for inBitmap.
        Bitmap inBitmap = getBitmapFromReusableSet(options);

        if (inBitmap != null) {
            // If a suitable bitmap has been found, set it as the value of
            // inBitmap.
            options.inBitmap = inBitmap;
            return true;
        }
        return false;
    }

    // This method iterates through the reusable bitmaps, looking for one
    // to use for inBitmap:
    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        Bitmap bitmap = null;

        if (!mReusableBitmaps.isEmpty()) {
            synchronized (mReusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator
                        = mReusableBitmaps.iterator();
                Bitmap item;

                while (iterator.hasNext()) {
                    item = iterator.next().get();

                    if (null != item && item.isMutable()) {
                        // Check to see it the item can be used for inBitmap.
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;

                            // Remove from reusable set so it can't be used again.
                            iterator.remove();
                            break;
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }

    private static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // From Android 4.4 (KitKat) onward we can re-use if the byte size of
            // the new bitmap is smaller than the reusable bitmap candidate
            // allocation byte count.
            int width = targetOptions.outWidth / targetOptions.inSampleSize;
            int height = targetOptions.outHeight / targetOptions.inSampleSize;
            int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
            return byteCount <= candidate.getAllocationByteCount();
        }

        // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
        return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
    }

    /**
     * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }
}
