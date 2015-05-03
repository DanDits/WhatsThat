package dan.dit.whatsthat.util.image;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.ByteBuffer;

/**
 * Created by daniel on 08.04.15.
 */
public class BitmapUtil {
    public static final double CONTRAST_WEAK_THRESHOLD = 0.25; // everything below is bad
    public static final double CONTRAST_STRONG_THRESHOLD = 0.5; // everything between this and weak is ok, everything above is great

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

        // use the relative difference between neighbored frequencies scaled with a polynomial ax²+bx+c
        // the polynomial is one at very bright and very dark values, emphasizing the contrast part on brightness
        double contrast = 0.;
        final double polyA = 4. / (((double) depth - 1.) * (depth - 1.));
        final double polyB = -4. / ((double) depth - 1.);
        final double polyC = 1;
        for (int i = 1; i < depth; i++) {
            contrast += frequencies[i] * (polyA * i * i + polyB * i + polyC);

        }
        return contrast / ((double) (image.getWidth() * image.getHeight()));
    }

    public static final double calculateGreyness(Bitmap image) {
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
     * @param wantedHeight  The new height of the resized image.
     * @param wantedWidth   The new width of the resized image.
     * @return A new Bitmap with the given height and width, rendered
     * with the given options. <code>null</code> if given image is <code>null</code>.
     */
    public static Bitmap resize(Bitmap originalImage,
                                int wantedHeight, int wantedWidth) {
        if (originalImage == null) {
            return null;
        }
        if (wantedHeight <= 0 || wantedWidth <= 0) {
            return originalImage;
        }
        // Create new Image
        Bitmap resizedImage = Bitmap.createScaledBitmap(originalImage, wantedWidth, wantedHeight, true);
        return resizedImage;
    }

    /**
     * Extract the data bytes from the given png image.
     *
     * @param image The image in png format.
     * @return Data in bytes that describe the image.
     */
    public static byte[] extractDataFromBitmap(Bitmap image) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(image.getByteCount());
        image.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
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
