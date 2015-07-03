package dan.dit.whatsthat.util.image;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * This class is an utility class. It offers RGB convertion
 * methods and helping methods to compare and store the rgb data.
 * @author Daniel
 *
 */
public final class ColorAnalysisUtil {

	
	private ColorAnalysisUtil() {	
	}

		/* To extract colors from ARGB int:
		case RED:
			return (rgb >> 16) & 0xFF;
		case GREEN:
			return (rgb >> 8) & 0xFF;
		case BLUE:
			return rgb & 0xFF;
		case ALPHA:
			return (rgb >> 24) & 0xFF;*/

	public static int interpolateColorLinear(int fromColor, int toColor, float fraction) {
		float antiFraction = 1.f - fraction;
		return Color.argb((int) ((Color.alpha(toColor) * fraction + Color.alpha(fromColor) * antiFraction)),
				(int) ((Color.red(toColor) * fraction + Color.red(fromColor) * antiFraction)),
				(int) ((Color.green(toColor) * fraction + Color.green(fromColor) * antiFraction)),
				(int) ((Color.blue(toColor) * fraction + Color.blue(fromColor) * antiFraction)));
	}

	public static int colorMultiples(int color, float multiple) {
		return Color.argb((int) (Color.alpha(color) * multiple),
				(int) (Color.red(color) * multiple),
				(int) (Color.green(color) * multiple),
				(int) (Color.blue(color) * multiple));
	}
	
	/**
	 * Compresses the given values into a single integer. This integer could then be read by fromRGB() and the
	 * given colors could be re-extracted.<br>
	 * If a value is not in range from 0 to 255, the data will be corrupt.
	 * @param red The red amount.
	 * @param green The green amount.
	 * @param blue The blue amount.
	 * @param alpha The alpha amount.
	 * @return A single integer storing the given information. Blue in the first 8 bits, 
	 * green in the next 8 bits and red in 
	 * the next 8 bits.
	 */
	public static int toRGB(int red, int green, int blue, int alpha) {
		return blue | (green << 8) | (red << 16) | (alpha << 24);
	}

	
	/**
	 * Returns the standard norm of the given color in the 3 or 4 dimensional
	 * color space. This is the distance to the color
	 * (0,0,0(,0)) in the color space.
	 * @param rgb The RGBA color to check.
	 * @param useAlpha If <code>true</code> the alpha will be taken into account and
	 * the distance will be measured in 4 dimensional color space.
	 * @return The distance of this rgb color to the zero color (black).
	 */
	public static double norm(int rgb, boolean useAlpha) {
		double result = 0;
		int currColValue;

        currColValue = Color.red(rgb);
        result += currColValue * currColValue;

        currColValue = Color.green(rgb);
        result += currColValue * currColValue;

        currColValue = Color.blue(rgb);
        result += currColValue * currColValue;

        if (useAlpha) {
            currColValue = Color.red(rgb);
            result += currColValue * currColValue;
        }
		return Math.sqrt(result);
	}

    public static double factorToSimilarityBound(double factor) {
        // makes the factor in range [0,1]
        double inBoundFactor = Math.max(0.0, Math.min(1.0, factor));
        // uses a function to transform the given value to a more fitting result.
        // For the in bound merge factor x it evaluates to : f(x)= e^(a*x^b)-1
        // with a=Log(13/10) and b = Log(a/Log(101/100))/Log(2) (makes a maximum for f(1)=0.3
        // , it is f(0.5)=0.01 and the strictly monotonic ascending behavior of the e-function
        return Math.pow(Math.E, 0.26236 * Math.pow(inBoundFactor, 4.72068)) - 1.0;
    }
	
	/**
	 * Mixes the given rgb values. To take the size of the underlying
	 * picture in account, the pixel amount needs to be supplied.
	 * @param rgb1 The first RGB value.
	 * @param rgb2 The second RGB value.
	 * @param pixels1 The pixels of the image the first RGB value refers to.
	 * @param pixels2 The pixels of the image the second RGB value refers to.
	 * @return The mixed RGBA value or <code>-1</code> if a pixels value is lower than zero.
	 */
	public static int mix(int rgb1, int rgb2, int pixels1, int pixels2) {
		int red;
		int green;
		int blue;
		int alpha;
		long currColValue1;
		long currColValue2;
		long totalPixels = pixels1 + pixels2;
		if (pixels1 < 0 || pixels2 < 0) {
			return -1;
		}
		long newCol;
		//red
		currColValue1 = Color.red(rgb1);
		currColValue2 = Color.red(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		red = (int) newCol;
		//green
		currColValue1 = Color.green(rgb1);
		currColValue2 = Color.green(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		green = (int) newCol;
		//blue
		currColValue1 = Color.blue(rgb1);
		currColValue2 = Color.blue(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		blue = (int) newCol;
		//alpha
		currColValue1 = Color.alpha(rgb1);
		currColValue2 = Color.alpha(rgb2);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		alpha = (int) newCol;
		return ColorAnalysisUtil.toRGB(red, green, blue, alpha);
	}

	public static int getAverageColor(Bitmap image) {
		int width = image.getWidth();
		int height = image.getHeight();
		long averageRed = 0, averageGreen = 0, averageBlue = 0, averageAlpha = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgba = image.getPixel(x, y);
				averageRed += Color.red(rgba);
				averageGreen += Color.green(rgba);
				averageBlue += Color.blue(rgba);
				averageAlpha += Color.alpha(rgba);
			}
		}
		long pixels = width * height;
		return Color.argb((int) (averageAlpha / pixels), (int) (averageRed / pixels), (int) (averageGreen / pixels), (int) (averageBlue / pixels));
	}

	public static int getAverageColor(Bitmap image, int fromX, int toX, int fromY, int toY) {
		long averageRed = 0, averageGreen = 0, averageBlue = 0, averageAlpha = 0;
        fromX = Math.max(0, fromX);
        fromY = Math.max(0, fromY);
        toX = Math.min(image.getWidth() - 1, toX);
        toY = Math.min(image.getHeight() - 1, toY);
		for (int x = fromX; x < toX; x++) {
			for (int y = fromY; y < toY; y++) {
				int rgba = image.getPixel(x, y);
				averageRed += Color.red(rgba);
				averageGreen += Color.green(rgba);
				averageBlue += Color.blue(rgba);
				averageAlpha += Color.alpha(rgba);
			}
		}
		long pixels = (toX - fromX) * (toY - fromY);
		if (pixels <= 0L) {
			pixels = 1L;
		}
        return Color.argb((int) (averageAlpha / pixels), (int) (averageRed / pixels), (int) (averageGreen / pixels), (int) (averageBlue / pixels));
	}

	/**
	 * Returns a String representation of the given average color in HEX format, optionally with the alpha.
	 * @param rgb The rgb to visualize.
	 * @param useAlpha If alpha value should be visualized.
	 * @return A formatted HEX String visualizing the given rgb.
	 */
	public static String visualizeRGB(int rgb, boolean useAlpha) {
		String output = "";
		String curr;
		if (useAlpha) {
			curr = Integer.toHexString(Color.alpha(rgb)).toUpperCase();
			if (curr.length() == 1) {
				output += "0" + curr;
			} else {
				output += curr;
			}	
			output += " ";
		}
		curr = Integer.toHexString(Color.red(rgb)).toUpperCase();
		if (curr.length() == 1) {
			output += "0" + curr;
		} else {
			output += curr;
		}
		output += " ";
		curr = Integer.toHexString(Color.green(rgb)).toUpperCase();
		if (curr.length() == 1) {
			output += "0" + curr;
		} else {
			output += curr;
		}
		output += " ";
		curr = Integer.toHexString(Color.blue(rgb)).toUpperCase();
		if (curr.length() == 1) {
			output += "0" + curr;
		} else {
			output += curr;
		}
		return output;
	}
	
	
	/**
	 * Calculates the brightness of the given rgb value using the alpha channel as
	 * a human would recognize it. Note that a fully transparent color would be considered to be bright.
	 * @param rgb The rgb value
	 * @return The brightness where 1 is very bright and 0 is very dark.
	 */
	public static double getBrightnessWithAlpha(int rgb) {
        // formula: (255-alpha)/255 + alpha*(0.299*red+0.587*green+0.114*blue)/(255*255)
        // white 255/255/255 is always considered to be very bright(=1), no matter the alpha
        return 1. +((rgb >> 24) & 0xFF) * (-1./255. + 0.299/65025.0 * ((rgb >> 16) & 0xFF) + 0.587/65025.0 * ((rgb >> 8) & 0xFF) + 0.114/65025.0 * (rgb & 0xFF));
	}

    /**
     * Calculates the brightness of the given rgb value as a human would recognize it.
     * @param rgb The rgb value.
     * @return The brightness where 1 is very bright and 0 is very dark.
     */
    public static double getBrightnessNoAlpha(int rgb) {
        // formula: (0.299*red+0.587*green+0.114*blue)/255
        return 0.299/255. * ((rgb >> 16) & 0xFF) + 0.587/255. * ((rgb >> 8) & 0xFF) + 0.114/255. * (rgb & 0xFF);
    }
	
	/**
	 * Calculates the "greyness" of the given RGB color, which is a way
	 * to measure the distance from the grey colors with red=green=blue.
	 * @param red The red value.
	 * @param green The green value.
	 * @param blue The blue value.
	 * @return Greyness of 0 means that red=green=blue which is the most grey and 1 means
     * the least grey, like (255,0,0).
	 */
	public static double getGreyness(int red, int green, int blue) {
		double mean = (red + green + blue) / 3.0;
		// square of the euclid distance from the straight line from 0/0/0 to 255/255/255 divided by the maximum possible distance
        // which is (255/3-255)²+2*(255/3)² for a color in the corner of the color square
		return ((mean - red) * (mean - red) + (mean - blue) * (mean - blue) + (mean - green) * (mean - green)) / 43350.;
	}
}
