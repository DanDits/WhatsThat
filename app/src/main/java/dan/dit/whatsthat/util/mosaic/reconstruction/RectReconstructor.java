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
import android.graphics.Canvas;
import android.util.Log;

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;

/**
 * This class models a specific {@link Reconstructor} which fragments
 * an image into rectangulars of equal height and width. This is a fast
 * and lightweight reconstruction, the default mosaic technique. As this
 * asserts all rects to have equal height and equal width, the given values
 * may be adjusted for the given image.
 * @author Daniel
 *
 */
public class RectReconstructor extends Reconstructor {
	protected final int mRectHeight;
	protected final int mRectWidth;
	private int[][] resultingRGBA;
	private Bitmap result;
    protected Canvas mResultCanvas;
	private int nextImageIndex;
	
	/**
	 * Creates a new {@link RectReconstructor} which fragments an image
	 * into rects. The rects height and width are the same for all rects, so
	 * if the wanted height/width (calculated by image height/width divided by rows/column)
	 * are not divisors of the image's height/width, the
	 * rect height/width will be adjusted to the next possible divisor.<br>
	 * Images to provide by giveNext() will all need to have the same height and width.
	 * @param source The image to fragment into rectangulars.
	 * @param wantedRows The wanted amount of rows.
	 * @param wantedColumns The wanted amount of columns.
	 */
	public RectReconstructor(Bitmap source, int wantedRows, int wantedColumns) {
		if (source == null) {
			throw new NullPointerException();
		}
		if (wantedRows <= 0 || wantedColumns <= 0) {
			throw new IllegalArgumentException("An image cannot be reconstructed to zero rows or columns.");
		}
		int actualRows = Reconstructor.getClosestCount(source.getHeight(), wantedRows);
		int actualColumns = Reconstructor.getClosestCount(source.getWidth(), wantedColumns);
		this.mRectHeight = source.getHeight() / actualRows;
		this.mRectWidth = source.getWidth() / actualColumns;
		this.resultingRGBA = new int[actualRows][actualColumns];
		this.nextImageIndex = 0;
		this.result = obtainBaseBitmap(this.mRectWidth * this.getColumns(), this.mRectHeight * this.getRows(),
				Bitmap.Config.ARGB_8888);
        Log.d("HomeStuff", "RectReconstructor: Source " + source.getWidth() + "/" + source.getHeight() + " result " + result.getWidth() + "/" + result.getHeight() + " actual rows/columns" + actualRows + "/" + actualColumns);
        mResultCanvas = new Canvas(result);
        evaluateResultingRGBA(source, actualRows, actualColumns);
	}

	private void evaluateResultingRGBA(Bitmap source, int rows, int columns) {
        // evaluate the fragments average colors
        for (int heightIndex = 0; heightIndex < rows; heightIndex++) {
            for (int widthIndex = 0; widthIndex < columns; widthIndex++) {
                this.resultingRGBA[heightIndex][widthIndex]
                        = evaluateRectValue(source, widthIndex * this.mRectWidth,
                        (widthIndex + 1) * this.mRectWidth,
                        heightIndex * this.mRectHeight,
                        (heightIndex + 1) * this.mRectHeight);
            }
        }
    }

    protected int evaluateRectValue(Bitmap source, int startX, int endX, int startY, int endY) {
        return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
    }
	
	/**
	 * The amount of rows of the fragmenation.
	 * @return The amount of rows of the fragmentation. Greater than or equal 1.
	 */
	private int getRows() {
		return this.resultingRGBA.length;
	}
	
	/**
	 * Returns the amount of columns of the fragmentation.
	 * @return The amount of columns of the fragmentation. Greater than or equal 1.
	 */
	private int getColumns() {
		return this.resultingRGBA[0].length;
	}
	
	@Override
	public boolean giveNext(Bitmap nextFragmentImage) {
		if (!this.hasAll()
				&& nextFragmentImage != null 
				&& nextFragmentImage.getWidth() == this.mRectWidth
				&& nextFragmentImage.getHeight() == this.mRectHeight) {

            mResultCanvas.drawBitmap(nextFragmentImage,
                    (this.nextImageIndex % this.getColumns()) * this.mRectWidth,
                    (this.nextImageIndex / this.getColumns()) * this.mRectHeight,
                    null);
			this.nextImageIndex++;
			return true;
		}
		return false;
	}

	@Override
	public MosaicFragment nextFragment() {
		if (this.hasAll()) {
			return null;
		} else {
			return new MosaicFragment(this.mRectWidth, this.mRectHeight,
					this.resultingRGBA[this.nextImageIndex / this.getColumns()]
							[this.nextImageIndex % this.getColumns()]);
		}
	}

	@Override
	public boolean hasAll() {
		return this.nextImageIndex >= this.getRows() * this.getColumns();
	}

	@Override
	public Bitmap getReconstructed() {
		if (this.hasAll()) {
			Bitmap temp = this.result;
			this.result = null;
			return temp;
		} else {
			return null;
		}
	}

    @Override
    public int estimatedProgressPercent() {
        return (int) (100 * nextImageIndex / (double) (getRows() * getColumns()));
    }
}
