package dan.dit.whatsthat.obfuscation;

import android.graphics.Bitmap;

import dan.dit.whatsthat.util.image.ImageMultiCache;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 21.01.15.
 */
public class Logo {
    private static final String IMAGE_CACHE_KEY_LOGO = "LOGO"; // Key for ImageMultiCache
    public static final double BRIGHTNESS_IS_LOGO_THRESHOLD_DEFAULT = 0.5; // threshold when a pixel is considered to be in the logo

    private Bitmap mLogo;
    private double mBrightnessThreshold;

    public Logo(Bitmap logo) {
        this(logo, BRIGHTNESS_IS_LOGO_THRESHOLD_DEFAULT);
    }

    public Logo(Bitmap logo, double brightnessThreshold) {
        mLogo = logo;
        mBrightnessThreshold = brightnessThreshold;
        if (mLogo == null) {
            throw new IllegalArgumentException("Given logo bitmap is null.");
        }
        if (brightnessThreshold < 0 || brightnessThreshold > 1) {
            mBrightnessThreshold = BRIGHTNESS_IS_LOGO_THRESHOLD_DEFAULT;
        }
        ImageMultiCache.INSTANCE.remove(IMAGE_CACHE_KEY_LOGO); // a new logo means we need to remove the old one(s)
    }

    public Bitmap getSized(int wantedWidth, int wantedHeight) {
        Bitmap logo = null;
        logo= ImageMultiCache.INSTANCE.get(IMAGE_CACHE_KEY_LOGO, wantedWidth, wantedHeight);
        if (logo == null || logo.getHeight() != wantedHeight || logo.getWidth() != wantedWidth) {
            logo= ImageUtil.resize(mLogo, wantedHeight, wantedWidth);
            ImageMultiCache.INSTANCE.add(IMAGE_CACHE_KEY_LOGO, logo);
        }
        return logo;
    }

    public double getThreshold() {
        return mBrightnessThreshold;
    }
}
