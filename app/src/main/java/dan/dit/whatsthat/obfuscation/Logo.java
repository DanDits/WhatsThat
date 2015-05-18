package dan.dit.whatsthat.obfuscation;

import android.graphics.Bitmap;

import dan.dit.whatsthat.util.image.BitmapUtil;

/**
 * Created by daniel on 21.01.15.
 */
public class Logo {
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
    }

    public Bitmap getSized(int wantedWidth, int wantedHeight) {
        return BitmapUtil.resize(mLogo, wantedHeight, wantedWidth);
    }

    public double getThreshold() {
        return mBrightnessThreshold;
    }
}
