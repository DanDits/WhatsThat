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

package dan.dit.whatsthat.util.mosaic.reconstruction;


import android.graphics.Bitmap;

import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * This class models an abstract Reconstructor which is used to reconstruct
 * a provided image. How the image is fragmented can be defined by the
 * Reconstructor. A typical implementation is a fragmentation into
 * rects (or squares) as provided by {@link RectReconstructor}. This
 * class allows to keep the fragmentation and merging progress close together
 * and easily allow different fragmentations.
 * @author Daniel
 *
 */
public abstract class Reconstructor {

	protected static Bitmap obtainBaseBitmap(int width, int height, Bitmap.Config config) {
        Bitmap base = ImageUtil.CACHE.getReusableBitmap(width, height, config);
        if (base != null) {
            return base;
        }
        return Bitmap.createBitmap(width, height, config);
    }

	/**
	 * Gives the Reconstructor the next image for the next missing Fragment.
	 * The given image must match the requirements of the Fragment provided by 
	 * nextFragment(). In most cases this will mean a direct match for height
	 * and value and the best approximation for the average RGB color.
	 * @param nextFragmentImage The image which will be used for the next fragment.
	 * @return <code>true</code> if the given image was accepted by the
	 * Reconstructor. If <code>false</code> you need to find another image,
	 * use nextFragment() to check the requirements of the image.
	 */
	public abstract boolean giveNext(Bitmap nextFragmentImage);
	
	/**
	 * Returns the next MosaicFragment which specifies some parameters of the
	 * next image to provide like the image's height and width and the
	 * average RGB it should have.
	 * @return The next MosaicFragment. <code>null</code> if hasAll() is <code>true</code>.
	 */
	public abstract MosaicFragment nextFragment();
	
	/**
	 * Returns <code>true</code> if all images needed were provided. Can
	 * If this is <code>true</code>, nextFragment will return <code>null</code>.
	 * Can only change when giveNext() returned <code>true</code>.
	 * @return If all images were provided and the reconstructor has
	 * enough information to build the new image.
	 */
	public abstract boolean hasAll();
	
	/**
	 * Returns the reconstructed image if the reconstructor hasAll() required
	 * images. The reconstructor can decide to build the image here or simply return it
	 * here. After this method returned a valid Bitmap, future calls to this method
	 * can return
	 * <code>null</code>, as the reference to the created image can be deleted. The policy
	 * for this behavior is to be defined by the implementing Reconstructor.
	 * @return The newly build image or <code>null</code> if the reconstructor has 
	 * not all required images or if this method was already successfully invoked and the
	 * Reconstructor does not save the image.
	 */
	public abstract Bitmap getReconstructed();
	
	/**
	 * Returns the closest value next to wantedCount which divides the given imageDimension.
	 * @param imageDimension The dimension that must be divided. Must be positive. (Image height or width).
	 * @param wantedCount The wanted amount of rows or columns.
	 * @return A divisor of imageDimension (so in range 1 to imageDimension (inclusive)) which is closest
	 * to wantedCount.
	 * @throws IllegalArgumentException If a parameter is negative or zero.
	 */
	static int getClosestCount(int imageDimension, int wantedCount) {
		if (imageDimension <= 0 || wantedCount <= 0) {
			throw new IllegalArgumentException("Image and wanted rect dimension must be greater than zero");
		} else {
			if (wantedCount > imageDimension) {
				return imageDimension;
			} else {
				return findClosestDivisor(imageDimension, wantedCount);
			}
		}
	}
	
	/**
	 * Returns the closest divisor to the given number. Not really fast, especially if
	 * toNumber is prime. Expects valid parameters.
	 * @param toNumber The number that has to be divided.
	 * @param wantedDivisor The wanted divisior for the given number. Must be greater than zero and
	 * smaller than or equal toNumber.
	 * @return A proper divisor in range 1 to toNumber (both inclusive) of toNumber which
	 * is closest to wantedDivisior;
	 */
	private static int findClosestDivisor(int toNumber, int wantedDivisor) {
		int currDivisor = wantedDivisor;
		int delta = 0;
		while (toNumber % currDivisor != 0) {
			// makes the divisor greater/smaller by one each iteration, circulating near wantedDivisor
			// will terminate if divisor gets equal to one or to toNumber
			delta++;
			currDivisor = (delta % 2 == 0) ? (currDivisor - delta) : (currDivisor + delta);
		}
		return currDivisor;
	}

	public abstract int estimatedProgressPercent();
	
}
