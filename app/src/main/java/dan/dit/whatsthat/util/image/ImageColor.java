package dan.dit.whatsthat.util.image;

/**
 * Enum constants expressing the available RGB data of a pixel of an image.<br>
 * The numeric data for each constant is in range 0 to 255 or 00 to FF.
 * 
 * @author Daniel
 * 
 */
public enum ImageColor {

	/**
	 * The color red.
	 */
	RED,

	/**
	 * The color green.
	 */
	GREEN,

	/**
	 * The color blue.
	 */
	BLUE,

	/**
	 * The alpha.
	 */
	ALPHA;
	
	/**
	 * Contains the constants RED, GREEN, BLUE
	 */
	public static final ImageColor[] RGB = {RED, GREEN, BLUE};
	
	/**
	 * Contains the constants RED, GREEN, BLUE, ALPHA
	 */
	public static final ImageColor[] RGBA = {RED, GREEN, BLUE, ALPHA};
	
	private ImageColor() {
	}
}
