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
	private int averageRGB;
	
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

	void reset(int width, int height, int averageRGB) {
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
