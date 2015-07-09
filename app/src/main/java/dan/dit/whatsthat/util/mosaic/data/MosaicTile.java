package dan.dit.whatsthat.util.mosaic.data;


import android.support.annotation.NonNull;

/**
 * This interface is a reference of anything than can be used to reconstruct a mosaic and serve as a MosaicFragment.
 * Therefore it provides an average color and the source.
 * @author Daniel
 *
 */
public interface MosaicTile<S> {

	
	/**
	 * Returns the source. This is anything that can reference
     * something to be turned into a MosaicFragment like a bitmap, an image
     * or a file.
	 * @return The source.
	 */
	@NonNull
    S getSource();
	
	/**
	 * Returns the average ARGB color of the images references
	 * by this mosaic tile.
	 * @return The average ARGB color.
	 */
	int getAverageARGB();

}