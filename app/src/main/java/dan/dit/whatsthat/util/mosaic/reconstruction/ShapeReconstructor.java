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

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * This class models an abstract ShapeReconstructor. For a concrete
 * ShapeReconstructor see {@link MultiRectReconstructor}. A ShapeReconstructor
 * is able to reconstruct an image with a fragmentation shape and algorithm
 * defined by the extending reconstructor.
 * @author Daniel
 *
 */
abstract class ShapeReconstructor extends Reconstructor {
	private int rowCount;
	private int columnCount;
	private int totalHeight;
	private int totalWidth;
	private int rectHeight;
	private int rectWidth;
	private boolean[][][] raster;
	private int[][] averageRGB;
	ColorMetric mColorMetric;


	/**
	 * Creates a new ShapeReconstructor for the given image with the wanted amount of
	 * rows and columns for basic fragmentation. The ShapeConstructor does the basic analyzing
	 * and fragmentation and sets up a raster of unconnected fragments which can later be connected
	 * to form different shapes.
	 * @param source The image to be analyzed and fragmented.
	 * @param maxRows The wanted amount of maximum rows for the basic fragmentation.
	 * @param maxColumns The wanted amount of maximum columns for the basic fragmentation.
	 * @param allowCornerConnections If corner connection should be allowed. If the specific
	 * Reconstructor does not use corner connections (TOP_LEFT, BOTTOM_RIGHT,...), set this
	 * @param metric The color metric to use to compare colors for similarity.
	 */
	ShapeReconstructor(Bitmap source, int maxRows, int maxColumns,
					   boolean allowCornerConnections, ColorMetric metric) {
		if (source == null) {
			throw new NullPointerException();
		}
		if (maxRows <= 0 || maxColumns <= 0) {
			throw new IllegalArgumentException("MaxRows/Columns need to be positive and not zero.");
		}
		mColorMetric = metric == null ? ColorMetric.Euclid2.INSTANCE : metric;
		this.init(source,
				Reconstructor.getClosestCount(source.getHeight(), maxRows),
				Reconstructor.getClosestCount(source.getWidth(), maxColumns),
				allowCornerConnections);
	}
	
	private void init(Bitmap image, int maxRows, int maxColumns,
			boolean allowCornerConnections) {
		this.rowCount = maxRows;
		this.columnCount = maxColumns;
		this.totalHeight = image.getHeight();
		this.totalWidth = image.getWidth();
		this.rectHeight = this.totalHeight / maxRows;
		this.rectWidth = this.totalWidth / maxColumns;
		this.raster = new boolean[maxRows][maxColumns]
				[(allowCornerConnections) ? FragmentNeighbor.COUNT : FragmentNeighbor.SIDES.length];
		this.averageRGB = new int[maxRows][maxColumns];
		
		for (int r = 0; r < maxRows; r++) {
			for (int c = 0; c < maxColumns; c++) {
				int startX = c * this.rectWidth;
				int startY = r * this.rectHeight;
				int endX = (c + 1) * this.rectWidth;
				int endY = (r + 1) * this.rectHeight;
				this.averageRGB[r][c] = ColorAnalysisUtil.getAverageColor(image, startX, endX, startY, endY);
			}
		}
	}
	
	/**
	 * Returns the total height of the image to reconstruct.
	 * @return The total height of the image to reconstruct.
	 */
	int getTotalHeight() {
		return totalHeight;
	}

	/**
	 * Returns the total width of the image to reconstruct.
	 * @return The total widht of the image to reconstruct.
	 */
	int getTotalWidth() {
		return totalWidth;
	}
	
	/**
	 * Returns the index of the last row which is connected to the given fragment in up
	 * or down connection.
	 * @param startRow The row index of the start fragment.
	 * @param column The column index of the start fragment and end fragment.
	 * @param up If the search should go up or down. If <code>true</code> it will
	 * search up and the returned index will be lower than or equal startRow.
	 * @return The index of the last row which is connected without interruption to the
	 * given fragment.
	 */
	int getLastConnectedRow(int startRow, int column, boolean up) {
		int dirIndex = up ? FragmentNeighbor.UP.ordinal() : FragmentNeighbor.DOWN.ordinal();
		int delta = up ? -1 : 1;
		int currRow = startRow;
		while ((currRow < this.rowCount) 
				&& (currRow >= 0)
				&& this.raster[currRow][column][dirIndex]) {
			currRow += delta;
		}
		return currRow;
	}
	
	/**
	 * Returns the index of the last column which is connected to the given fragment in left
	 * or right connection.
	 * @param startColumn The column index of the start fragment.
	 * @param row The row index of the start fragment and end fragment.
	 * @param left If the search should go left or right. If <code>true</code> it will
	 * search left and the returned index will be lower than or equal startColumn.
	 * @return The index of the last column which is connected without interruption to the
	 * given fragment.
	 */
	int getLastConnectedColumn(int startColumn, int row, boolean left) {
		int dirIndex = left ? FragmentNeighbor.LEFT.ordinal() : FragmentNeighbor.RIGHT.ordinal();
		int delta = left ? -1 : 1;
		int currColumn = startColumn;
		while ((currColumn < this.columnCount) 
				&& (currColumn >= 0)
				&& this.raster[row][currColumn][dirIndex]) {
			currColumn += delta;
		}
		return currColumn;
	}
	
	/**
	 * Returns <code>true</code> if the Fragment at the given row and column is connected
	 * to the specified neighbor. Will throw an {@link ArrayIndexOutOfBoundsException} if
	 * using a corner neighbor and this ShapeReconstructor did not allow corner connection.
	 * @param row The row index of the Fragment.
	 * @param column The column index of the Fragment.
	 * @param to The neighbor to check for a connection.
	 * @return <code>true</code> if the Fragment is connected to the neighbor.
	 */
	boolean isConnected(int row, int column, FragmentNeighbor to) {
		return this.raster[row][column][to.ordinal()];
	}
	
	/**
	 * Sets or removes the connection between two fragments. Removing of connections
	 * is not primarly supported, but possible. Information about each single Fragment
	 * is lost when connecting Fragments, so removing a connection leaves the average RGB
	 * value unchanged. If the neighbor does not exist, nothing is done. A connection is
	 * symmetric.
	 * @param row The row index of the Fragment to connect to the neighbor.
	 * @param column The column index of the Fragment to connect to the neighbor.
	 * @param neighbor The neighbor the connection should be set for.
	 * @param connected If <code>true</code>, the fragments will be connected, else
	 * the connection will be removed.
	 * @return <code>true</code> if the neighbor existed and the connection between the
	 * neighbors changed.
	 */
	boolean setConnected(int row, int column, FragmentNeighbor neighbor, boolean connected) {
		int neighborRow = row + neighbor.getRowDelta();
		int neighborColumn = column + neighbor.getColumnDelta();
		if (neighborRow >= 0 && neighborRow < this.rowCount 
				&& neighborColumn >= 0 && neighborColumn < this.columnCount
				&& this.raster[row][column][neighbor.ordinal()] != connected) {
			// the given neighbor exists for this fragment and the connection will change
			this.raster[row][column][neighbor.ordinal()] = connected;
			FragmentNeighbor neighborPartner = neighbor.getPartner();
			if (neighborPartner == null) {
				return false;
			}
			this.raster[neighborRow][neighborColumn][neighborPartner.ordinal()] = connected;
			int mixedRGB = ColorAnalysisUtil.mix(this.getAverageRGB(row, column), 
					this.getAverageRGB(neighborRow, neighborColumn), 
					this.rectWidth * this.rectHeight, this.rectWidth * this.rectHeight);
			this.averageRGB[row][column] = mixedRGB;
			this.averageRGB[neighborRow][neighborColumn] = mixedRGB;
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the height of a basic fragment rect. The same
	 * for all rects.
	 * @return The height of a basic single fragment rect.
	 */
	int getRectHeight() {
		return rectHeight;
	}

	/**
	 * Returns the width of a basic fragment rect. The same
	 * for all rects.
	 * @return The widht of a basic single fragment rect.
	 */
	int getRectWidth() {
		return rectWidth;
	}

	/**
	 * Returns the average RGB value of the fragment at the given row
	 * and column.
	 * @param row The row index of the fragment.
	 * @param column The column index of the fragment.
	 * @return The average RGB value of the fragment.
	 */
	int getAverageRGB(int row, int column) {
		return this.averageRGB[row][column];
	}
	
	/**
	 * Returns the amount of rows of the basic fragmentation. Does not change.
	 * @return The amount of rows of the basic fragmentation.
	 */
	int getRowCount() {
		return this.rowCount;
	}
	
	/**
	 * Returns the amount of columns of the basic fragmentation. Does not change.
	 * @return The amount of columns of the basic fragmentation.
	 */
	int getColumnCount() {
		return this.columnCount;
	}

}
