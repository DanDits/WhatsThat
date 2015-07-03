package dan.dit.whatsthat.util.mosaic.reconstruction;


import dan.dit.whatsthat.util.image.ColorAnalysisUtil;

/**
 * This class models an immutable Fragment of a mosaic image
 * that is to be constructed. Used by {@link Reconstructor} to
 * specify what kind of BufferedImage is requested next.
 * @author Daniel
 *
 */
public class MosaicFragment {
	private int width;
	private int height;
	protected int averageRGB;
	
	/**
	 * Creates a new Fragment, storing the given widht, height
	 * and average RGB.
	 * @param width The width of the RGB.
	 * @param height The height of the RGB.
	 * @param averageRGB The average RGB.
	 */
	public MosaicFragment(int width, int height, int averageRGB) {
		reset(width, height, averageRGB);
	}

	protected void reset(int width, int height, int averageRGB) {
		this.width = width;
		this.height = height;
        this.averageRGB = averageRGB;
	}

	/**
	 * Returns the width of the Fragment. The image provided with giveNext()
	 * must have the same width as the next Fragment.
	 * @return The width of the Fragment.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the height of the Fragment. The image provided with giveNext()
	 * must have the same height as the next Fragment.
	 * @return The height of the Fragment.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the average RGB of this Fragment. The image provided with
	 * giveNext() must or should be the best matching image measured by
	 * the image's average RBB.
	 * @return The average RGB of this Fragment.
	 */
	public int getAverageRGB() {
		return averageRGB;
	}
	
	@Override
	public String toString() {
		return "Fragment (hxw=" + this.height + "x" + this.width 
				+ " rgb=" + ColorAnalysisUtil.visualizeRGB(this.averageRGB, false) + ")";
	}
}
