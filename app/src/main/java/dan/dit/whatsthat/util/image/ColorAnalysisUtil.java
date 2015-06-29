package dan.dit.whatsthat.util.image;

import android.graphics.Color;

/**
 * This class is an utility class. It offers RGB convertion
 * methods and helping methods to compare and store the rgb data.
 * @author Daniel
 *
 */
public final class ColorAnalysisUtil {
	/**
	 * This value indicates the worst similarity possible for a similarity
	 * comparison of two rgb values if alpha is not used. Equal to 3*255*255.
	 */
	public static final int WORST_SIMILARITY = 3 * 255 * 255;
	
	/**
	 * This value indicates the worst similarity possible for a similarity
	 * comparision of two rgb values if alpha is used. Equal to 4*255*255.
	 */
	public static final int WORST_SIMILARITY_ALPHA = 4 * 255 * 255;
	
	private ColorAnalysisUtil() {	
	}
	
	/**
	 * Extracts from the given integer the given image color RED, GREEN, BLUE or ALPHA.
	 * The returned value is in range (inclusive) 0 to 255.
	 * @param rgb The rgb values are stored in this integer. BLUE in the rightmost 8 bits,
	 * GREEN in the next 8 bits, RED in the next 8 bits and ALPHA in the next 8 bits.
	 * @param color The color data to be extracted. Must be a valid enum constant.
	 * @return The requested color data from the rgb, a value in range of 0 to 255.
	 */
	public static int fromRGB(int rgb, ImageColor color) {
		switch (color) {
		case RED:
			return (rgb >> 16) & 0xFF;
		case GREEN:
			return (rgb >> 8) & 0xFF;
		case BLUE:
			return rgb & 0xFF;
		case ALPHA:
			return (rgb >> 24) & 0xFF;
		default:
			return -1;
		}
	}

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
	 * Changes the given Color to the given value, keeping the rest of the values.
	 * @param rgb The base RGBA color.
	 * @param value The new value for the given image color.
	 * @param col The image color to change.
	 * @return The new RGBA color.
	 */
	public static int toRGB(int rgb, int value, ImageColor col) {
		int red = col == ImageColor.RED ? value : (rgb >> 16) & 0xFF;
		int green = col == ImageColor.GREEN ? value : (rgb >> 8) & 0xFF;
		int blue = col == ImageColor.BLUE ? value : rgb & 0xFF;
		int alpha = col == ImageColor.ALPHA ? value : (rgb >> 24) & 0xFF;
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
		for (ImageColor col : (useAlpha) ? ImageColor.RGBA : ImageColor.RGB) {
			currColValue = ColorAnalysisUtil.fromRGB(rgb, col);
			result += currColValue * currColValue;
		}
		return Math.sqrt(result);
	}
	
	/**
	 * Returns the similarity of the two rgb colors, optionally taking the alpha
	 * into account.
	 * @param rgb1 The first RGBA value.
	 * @param rgb2 The second RGBA value.
	 * @param useAlpha If alpha should be used for measuring.
	 * @return A measure for the similarity of the rgb colors with 0 indicating that
	 * the two colors are equal and a higher result indicating a lower similarity. Upper
	 * bounds are the constants WORST_SIMILARITY if useAlpha is <code>false</code> or
	 * WORST_SIMILARITY_ALPHA if alpha is used.
	 */
	public static int similarity(int rgb1, int rgb2, boolean useAlpha) {
		int result = 0;
		int currColValue1;
		int currColValue2;
		for (ImageColor col : (useAlpha) ? ImageColor.RGBA : ImageColor.RGB) {
			currColValue1 = ColorAnalysisUtil.fromRGB(rgb1, col);
			currColValue2 = ColorAnalysisUtil.fromRGB(rgb2, col);
			result += (currColValue1 - currColValue2) * (currColValue1 - currColValue2);
		}
		return result;
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
		currColValue1 = ColorAnalysisUtil.fromRGB(rgb1, ImageColor.RED);
		currColValue2 = ColorAnalysisUtil.fromRGB(rgb2, ImageColor.RED);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		red = (int) newCol;
		//green
		currColValue1 = ColorAnalysisUtil.fromRGB(rgb1, ImageColor.GREEN);
		currColValue2 = ColorAnalysisUtil.fromRGB(rgb2, ImageColor.GREEN);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		green = (int) newCol;
		//blue
		currColValue1 = ColorAnalysisUtil.fromRGB(rgb1, ImageColor.BLUE);
		currColValue2 = ColorAnalysisUtil.fromRGB(rgb2, ImageColor.BLUE);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		blue = (int) newCol;
		//alpha
		currColValue1 = ColorAnalysisUtil.fromRGB(rgb1, ImageColor.ALPHA);
		currColValue2 = ColorAnalysisUtil.fromRGB(rgb2, ImageColor.ALPHA);
		newCol = (currColValue1 * pixels1 + currColValue2 * pixels2) / totalPixels;
		alpha = (int) newCol;
		return ColorAnalysisUtil.toRGB(red, green, blue, alpha);
	}

	/**
	 * Returns a String representation of the given average color in HEX format, optionally with the alpha.
	 * @param rgb The rgb to visualiize.
	 * @param useAlpha If alpha value should be visulized.
	 * @return A formatted HEX String visualizing the given rgb.
	 */
	public static String visualizeRGB(int rgb, boolean useAlpha) {
		String output = "";
		String curr;
		if (useAlpha) {
			curr = Integer.toHexString(ColorAnalysisUtil.fromRGB(rgb, ImageColor.ALPHA)).toUpperCase();
			if (curr.length() == 1) {
				output += "0" + curr;
			} else {
				output += curr;
			}	
			output += " ";
		}
		curr = Integer.toHexString(ColorAnalysisUtil.fromRGB(rgb, ImageColor.RED)).toUpperCase();
		if (curr.length() == 1) {
			output += "0" + curr;
		} else {
			output += curr;
		}
		output += " ";
		curr = Integer.toHexString(ColorAnalysisUtil.fromRGB(rgb, ImageColor.GREEN)).toUpperCase();
		if (curr.length() == 1) {
			output += "0" + curr;
		} else {
			output += curr;
		}
		output += " ";
		curr = Integer.toHexString(ColorAnalysisUtil.fromRGB(rgb, ImageColor.BLUE)).toUpperCase();
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
