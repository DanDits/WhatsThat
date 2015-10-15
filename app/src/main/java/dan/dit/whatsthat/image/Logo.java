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

package dan.dit.whatsthat.image;

import android.graphics.Bitmap;

import dan.dit.whatsthat.util.image.BitmapUtil;

/**
 * Created by daniel on 21.01.15.
 */
public class Logo {
    public static final int BASE_WIDTH_HEIGHT = 1024;
    private static final double BRIGHTNESS_IS_LOGO_THRESHOLD_DEFAULT = 0.5; // threshold when a pixel is considered to be in the logo

    private Bitmap mLogo;
    private double mBrightnessThreshold;

    public Logo(Bitmap logo) {
        this(logo, BRIGHTNESS_IS_LOGO_THRESHOLD_DEFAULT);
    }

    private Logo(Bitmap logo, double brightnessThreshold) {
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
        return BitmapUtil.resize(mLogo, wantedWidth, wantedHeight);
    }

    public double getThreshold() {
        return mBrightnessThreshold;
    }
}
