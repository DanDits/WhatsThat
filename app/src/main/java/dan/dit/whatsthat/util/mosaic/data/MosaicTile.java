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
    S getSource();
	
	/**
	 * Returns the average ARGB color of the images references
	 * by this mosaic tile.
	 * @return The average ARGB color.
	 */
	int getAverageARGB();

}