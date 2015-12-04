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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by daniel on 08.04.15.
 */
public class BitmapUtil {
    /**
     * The resulting bitmap fits inside the required
     * dimensions but does not necessarily gets resized to fit one dimension exactly.
     * The aspect ratio is the same to the original image.
     */
    public static final int MODE_FIT_NO_GROW = 0;

    /**
     * The resulting bitmap fits inside the given
     * dimensions and is resized so that one dimension fits exactly
     * and the aspect ratio is the same to the original image.
     */
    public static final int MODE_FIT_INSIDE = 1;

    /**
     * The resulting bitmap fits inside the given
     * dimensions and is resized so that one dimension fits exactly
     * and the aspect ratio is almost the same to the original image.
     * This can result in the resulting bitmap fitting the given dimensions
     * exactly even though the given bitmap did not.
     */
    public static final int MODE_FIT_INSIDE_GENEROUS = 2;

    /**
     * The resulting bitmap fits inside the required
     * dimensions and is resized so that both dimensions fit exactly to
     * the given dimensions.
     */
    public static final int MODE_FIT_EXACT = 3;

    public static final double CONTRAST_WEAK_THRESHOLD = 0.3; // everything below is bad
    public static final double CONTRAST_STRONG_THRESHOLD = 0.6; // everything between this and weak is ok, everything above is great

    public static final double GREYNESS_STRONG_THRESHOLD = 0.15; // everything below is very grey (0 would be black and white)
    public static final double GREYNESS_MEDIUM_THRESHOLD = 0.3; // everything between this and STRONG is medium grey, everything above is getting very colorful

    public static double calculateContrast(Bitmap image) {
        final int depth = 64;
        int[] frequencies = new int[depth];
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgba = image.getPixel(x, y);
                int value = (int) ((depth - 1) * ColorAnalysisUtil.getBrightnessWithAlpha(rgba));
                frequencies[value]++;
            }
        }

        //wolfram alpha: interpolating polynomial | {{0, 1}, {11, 0.2}, {32, 0}, {52, 0.2}, {63, 1}}
        //1 - 0.12162 x + 0.00562105 x^2 - 0.000117161 x^3 + 9.298497201723005*^-7 x^4
        double contrast = 0.;
        for (int i = 1; i < depth; i++) {
            contrast += frequencies[i] * (1. + i * (-0.12162 + i * (0.00562105 + i * (-0.000117161 + i * 9.298497201723005E-7))));

        }
        return contrast / ((double) (image.getWidth() * image.getHeight()));
    }

    public static double calculateGreyness(Bitmap image) {
        // calculate average greyness of pixels
        double greyness = 0.;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getPixel(x, y);
                greyness += ColorAnalysisUtil.getGreyness(Color.red(rgb), Color.green(rgb), Color.blue(rgb));
            }
        }
        return greyness / ((double) (image.getWidth() * image.getHeight()));
    }

    public static Bitmap improveContrast(Bitmap originalImage) {
        Bitmap result = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(), originalImage.getConfig());
        // http://de.wikipedia.org/wiki/Punktoperator_%28Bildverarbeitung%29, Histogrammäqualisation

        // calculate relative frequencies of occurances of certain brightness values
        final int depth = 256;
        int[] frequencies = new int[depth];
        for (int x = 0; x < originalImage.getWidth(); x++) {
            for (int y = 0; y < originalImage.getHeight(); y++) {
                int oldRgba = originalImage.getPixel(x, y);
                int value = (int) ((depth - 1) * ColorAnalysisUtil.getBrightnessWithAlpha(oldRgba));
                // use alpha=255 to restore value without rounding error, save alpha in blue
                result.setPixel(x, y, ColorAnalysisUtil.toRGB(value, value, Color.alpha(oldRgba), 255));
                frequencies[value]++;
            }
        }

        // accumulate frequencies
        for (int i = 1; i < depth; i++) {
            frequencies[i] = frequencies[i] + frequencies[i - 1];
        }

        // Histogrammhyperbolisation
        final double power = 3./2.;
        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int rgba = result.getPixel(x, y);
                int value = Color.red(rgba);
                value = Math.max(Math.min(depth - 1, value), 0);
                value = (int) (value * Math.pow(frequencies[value] / ((double) (result.getWidth() * result.getHeight())), power));
                value = Math.max(Math.min(depth - 1, value), 0);
                result.setPixel(x, y, ColorAnalysisUtil.toRGB(value, value, value, Color.blue(rgba)));
            }
        }
        return result;
    }

    /**
     * Resizes the given image to the wanted height and width
     * using the specified render options.
     *
     * @param originalImage The image that will be resized.
     * @param wantedWidth   The new width of the resized image.
     * @param wantedHeight  The new height of the resized image.
     * @return A new Bitmap with the given height and width, rendered
     * with the given options. <code>null</code> if given image is <code>null</code>.
     */
    public static Bitmap resize(Bitmap originalImage,
                                int wantedWidth, int wantedHeight) {
        if (originalImage == null) {
            return null;
        }
        if (wantedHeight <= 0 || wantedWidth <= 0) {
            return originalImage;
        }
        // Create new Image
        return Bitmap.createScaledBitmap(originalImage, wantedWidth, wantedHeight, true);
    }

    public static class ByteBufferHolder {
        private ByteBuffer mBuffer;

        public byte[] array() {
            return mBuffer == null ? null : mBuffer.array();
        }
    }

    /**
     * Extract the data bytes from the given png image.
     *
     * @param buffer A buffer holder to use to copy pixel data into, can be uninitialized to allocate new storage.
     * @param image The image in png format.
     */
    public static void extractDataFromBitmap(ByteBufferHolder buffer, Bitmap image) {
        int requiredCapacity = image.getByteCount();
        if (buffer.mBuffer == null || buffer.mBuffer.capacity() < requiredCapacity) {
            // we need a new or bigger buffer
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long maxRemainingBytes = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / 2; // we only talk half
            if (requiredCapacity < maxRemainingBytes) {
                buffer.mBuffer = ByteBuffer.allocate(requiredCapacity);
                buffer.mBuffer.mark();
                image.copyPixelsToBuffer(buffer.mBuffer);
                return;
            } else {
                // we dont have so much memory left, panic mode
                if (buffer.mBuffer == null) {
                    buffer.mBuffer = ByteBuffer.allocate((int) maxRemainingBytes);
                    buffer.mBuffer.mark();
                } else {
                    buffer.mBuffer.reset();
                }
                for (int i = 0; i < Math.min(image.getHeight(), Math.max(maxRemainingBytes, buffer.mBuffer.capacity())) / 4; i++) {
                    int x = Math.min(i, image.getWidth() - 1);
                    int y = i;
                    if (y >= image.getHeight()) {
                        break;
                    }
                    int pixel = image.getPixel(x, y);
                    buffer.mBuffer.put((byte) ((pixel & 0xFF000000) >> 6));
                    buffer.mBuffer.put((byte) ((pixel & 0x00FF0000) >> 4));
                    buffer.mBuffer.put((byte) ((pixel & 0x0000FF00) >> 2));
                    buffer.mBuffer.put((byte) ((pixel & 0x000000FF)));
                }
                Log.e("Image", "Too little memory to exract all data from bitmap: " + maxRemainingBytes + ", required: " + requiredCapacity);
                return;
            }
        }
        buffer.mBuffer.reset();
        image.copyPixelsToBuffer(buffer.mBuffer);
    }

    public static Bitmap attemptBitmapScaling(Bitmap result, int reqWidth, int reqHeight, int mode) {
        if (reqWidth <= 0 || reqHeight <= 0) {
            return result;
        }
        int imageWidth = result.getWidth();
        int imageHeight = result.getHeight();
        if (imageWidth == reqWidth && imageHeight == reqHeight) {
            return result;
        }
        if (mode == MODE_FIT_NO_GROW) {
            if (imageWidth <= reqWidth && imageHeight <= reqHeight) {
                return result; // we already fit inside, do not grow
            }
            // else scale down to fit inside, keeping aspect ratio
        }
        //      for fitting similar aspect ratios, calculate how bad it is to forced scale the image to desired dimensions
        if (mode == MODE_FIT_EXACT
                || (mode == MODE_FIT_INSIDE_GENEROUS && ImageUtil.areAspectRatiosSimilar(reqWidth, reqHeight, result.getWidth(), result.getHeight()))) {
            // scale the image exactly to required dimensions, will most likely break the aspect ratio but not too hard
            return Bitmap.createScaledBitmap(result, reqWidth, reqHeight, true);
        }
        // scale the bitmap so that bitmaps dimensions are smaller or equal to required dimensions, keeping aspect ratio
        double scalingFactor = Math.min(reqHeight / ((double) result.getHeight()), reqWidth / ((double) result.getWidth()));
        return Bitmap.createScaledBitmap(result, (int) (result.getWidth() * scalingFactor), (int) (result.getHeight() * scalingFactor), true);
    }

    public static Bitmap attemptBitmapScaling(Bitmap result, int reqWidth, int reqHeight, boolean enforceDimension) {
        if (reqWidth <= 0 || reqHeight <= 0) {
            return result;
        }
        if (result.getWidth() == reqWidth && result.getHeight() == reqHeight) {
            return result;
        } else {
            // calculate how bad it is to forced scale the image to desired dimensions
            if (enforceDimension || ImageUtil.areAspectRatiosSimilar(reqWidth, reqHeight, result.getWidth(), result.getHeight())) {
                // scale the image exactly to required dimensions, will most likely break the aspect ratio but not too hard
                result = Bitmap.createScaledBitmap(result, reqWidth, reqHeight, true);
            } else {
                // scale the bitmap so that bitmaps dimensions are smaller or equal to required dimensions, keeping aspect ratio
                double scalingFactor = Math.min(reqHeight / ((double) result.getHeight()), reqWidth / ((double) result.getWidth()));
                result = Bitmap.createScaledBitmap(result, (int) (result.getWidth() * scalingFactor), (int) (result.getHeight() * scalingFactor), true);
            }
            return result;
        }
    }
}
