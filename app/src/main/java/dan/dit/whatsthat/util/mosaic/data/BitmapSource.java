package dan.dit.whatsthat.util.mosaic.data;

import android.graphics.Bitmap;

/**
 * An interface for a MosaicMaker to provide the actual bitmap in a certain size
 * for a MosaicTile. The source should cache the given bitmap and potentially prepare to give
 * out the same image multiple times in various sizes.
 * Created by daniel on 01.07.15.
 */
public interface BitmapSource<S> {

    /**
     * Retrieves the bitmap associated with the given MosaicTile in the given width
     * and height. The result bitmap's dimensions must exactly match the given dimensions.
     * If the bitmap is no longer accessible the result can be null.
     * @param forTile The tile to retrieve the image for.
     * @param requiredWidth The returned bitmap's width.
     * @param requiredHeight The returned bitmap's height.
     * @return A bitmap of exactly the given dimensions or null if not found for any reasons.
     */
    Bitmap getBitmap(MosaicTile<S> forTile, int requiredWidth, int requiredHeight);
}
