/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.util.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.util.LruCache;

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

                    if (item != null && item.isMutable()) {
                        // Check to see it the item can be used
                        if (canReconfigure(item, config, width, height)) {
                            bitmap = item;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                try {
                                    bitmap.reconfigure(width, height, config == null ? item.getConfig() : config);
                                } catch (Exception e) {
                                    bitmap = null; // happens when the bitmap is a native bitmap that can't be reconfigured
                                    iterator.remove();
                                }
                            } // else the config and dimensions already match exactly

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
            options.inMutable = true;
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

    private static boolean canReconfigure(Bitmap toConfigure, Bitmap.Config targetConfig, int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int byteCount = width * height * getBytesPerPixel(targetConfig == null ? toConfigure.getConfig() : targetConfig);
            return byteCount <= toConfigure.getAllocationByteCount();
        }
        return width == toConfigure.getWidth() && height == toConfigure.getHeight()
                && (targetConfig == null || targetConfig.equals(toConfigure.getConfig()));
    }

    private static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {

        if (targetOptions.inSampleSize == 1 //TODO some bug where inBitmap fails!?!
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
