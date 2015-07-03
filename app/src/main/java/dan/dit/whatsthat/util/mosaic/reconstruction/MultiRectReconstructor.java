package dan.dit.whatsthat.util.mosaic.reconstruction;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.ColorMetric;

/**
 * This class implements a ShapeReconstructor. The given image
 * is split into rects like the with ordinary {@link RectReconstructor},
 * but in comparision to the RectReconstructor, the rects of this
 * reconstructor will have different widths and heights. To determine
 * when to merge rects to bigger ones, the mergeFactor is used.<br>The
 * merged rects will keep the given apect ratio, so for example if the given
 * parameters height/wantedRows equal width/wantedColumns, this will only produce squares.
 * Once getReconstructed() returned a valid image, it will return <code>null</code>
 * for future calls.<br>
 * This Reconstructor will turn out to be slower than the RectReconstructor since it needs different
 * sized images, and resizing is pretty expensive (even though the caching helps a lot).
 * @author Daniel
 *
 */
public class MultiRectReconstructor extends ShapeReconstructor {
	private Bitmap resultImage;
	private Canvas mResultCanvas;
	private int lastGivenColumn;
	private int lastGivenRow;
	private MosaicFragment next;
    private MosaicFragment mFragment;
    private int mFragmentCount;

	
	/**
	 * Creates a new MultiRectReconstructor which is capable of reconstructing
	 * the given image into multi sized rects. The basic splitting is defined by
	 * maxRows and maxColumns which need to be positive. No rectangle will be smaller than
	 * imageHeight/maxRows to imageWidth/maxColumns pixel.
	 * @param source The image to reconstruct.
	 * @param maxRows The amount of rows the basic fragmentation will have.
	 * @param maxColumns The amount of columns the basic fragmentation will have.
	 * @param mergeFactor A factor which influences the merging of rects. A factor of <code>0</code>
	 * indicates strict merging which will only allow rects to merge if they have exactly the same
	 * average color and a factor of <code>1</code> will make the reconstructor 
	 * very tolerant and merge rects easily. If out of bounds, factor will be adjusted to the
	 * corresponding bound.
	 * @param useAlpha If the alpha value of images should be taken into account.
	 * @throws IllegalArgumentException If maxRows or maxColumns lower than or equal zero.
	 * @throws NullPointerException If image is <code>null</code>.
	 */
	public MultiRectReconstructor(Bitmap source, int maxRows,
			int maxColumns, double mergeFactor, boolean useAlpha, ColorMetric metric) {
		super(source, maxRows, maxColumns, false, metric);
        mFragment = new MosaicFragment(0, 0, 0);
        this.init(ColorAnalysisUtil.factorToSimilarityBound(mergeFactor), useAlpha);
		this.lastGivenColumn = -1;
		this.lastGivenRow = -1;
	}

	/**
	 * Inits the reconstructor and merges the rects. This connects ShapeFragments, but
	 * ensures that only rects are created, though the sizes may be of any kind, so
	 * also a complete row of pixels could be merged if it was of equal color.
	 * When ShapeFragments are connected, their average color will be mixed and both ShapeFragments
	 * will have this mixed color as a new average color.
	 * @param mergeFactor The factor for merging, a value between 0 and 1, inclusive.
	 * If the similarity between two color vectors divided by the maximum similarity is lower than
	 * this factor, the rects will be able to be merged.
	 * @param useAlpha If alpha should be considered for similarity comparisions.
	 */
	private void init(double mergeFactor, boolean useAlpha) {
		int rows = this.getRowCount();
		int columns = this.getColumnCount();
		int columnCandidatesStartRow;
		int columnCandidatesEndRow;
		int columnCandidatesColumn;
		int rowCandidatesStartColumn;
		int rowCandidatesEndColumn;
		int rowCandidatesRow;
        final double maxSim = mColorMetric.maxValue(useAlpha);
		
		for (int r = 0; r < rows - 1; r++) {
			for (int c = 0; c < columns - 1; c++) {
				if (!this.isConnected(r, c, FragmentNeighbor.UP)
						&& !this.isConnected(r, c + 1, FragmentNeighbor.UP)) {
					// this one is not connected to up and the right one is not connected to up too
					columnCandidatesStartRow = r;
					columnCandidatesColumn = c + 1;
					columnCandidatesEndRow = r;
					// test if the column right of the current square could be added
					boolean columnTestOk;
					do {
						double simFactor = mColorMetric.getDistance(this.getAverageRGB(r, c),
								this.getAverageRGB(columnCandidatesEndRow, columnCandidatesColumn),
								useAlpha) / maxSim;
						// if simFactor is greater than mergeFactor for the first time, this terminates the loop
						columnTestOk = simFactor <= mergeFactor;
						columnCandidatesEndRow++;
					} while (columnTestOk && columnCandidatesEndRow < rows
							&& this.isConnected(columnCandidatesEndRow, c, FragmentNeighbor.UP));
							
					// could all at the right side be added to this rect and is there a corner ?
					if (columnTestOk && columnCandidatesEndRow < rows) {
						// test bottom right corner that should be added
						boolean cornerTestOk = (mColorMetric.getDistance(this.getAverageRGB(r, c),
								this.getAverageRGB(columnCandidatesEndRow, columnCandidatesColumn),
								useAlpha) / maxSim) <= mergeFactor;
						if (cornerTestOk) {
							// test if the row on the down side of the current square could be added
							boolean rowTestOk;
							rowCandidatesStartColumn = this.getLastConnectedColumn(c, r, true);
							rowCandidatesRow = this.getLastConnectedRow(r, rowCandidatesStartColumn, false) + 1;
							rowCandidatesEndColumn = rowCandidatesStartColumn;
							do {
								double simFactor = mColorMetric.getDistance(
										this.getAverageRGB(rowCandidatesRow, rowCandidatesEndColumn),
										this.getAverageRGB(r, c), useAlpha) / maxSim;
								rowTestOk = simFactor <= mergeFactor;
								rowCandidatesEndColumn++;
							} while (rowTestOk 
									&& this.isConnected(rowCandidatesRow - 1, rowCandidatesEndColumn, 
											FragmentNeighbor.LEFT));
							if (rowTestOk) {
								// the column at the right side of the current square can be added, 
								// the bottom right corner
								// and also the row below the square, so we got all we need to make a new square
								// setup connections
								for (int i = columnCandidatesStartRow; i < columnCandidatesEndRow; i++) {
									if (i != columnCandidatesStartRow) {
										this.setConnected(i, columnCandidatesColumn, FragmentNeighbor.UP, true);
									}
									this.setConnected(i, columnCandidatesColumn, FragmentNeighbor.LEFT, true);
								}
								for (int i = rowCandidatesStartColumn; i < rowCandidatesEndColumn; i++) {
									if (i != rowCandidatesStartColumn) {
										this.setConnected(rowCandidatesRow, i, FragmentNeighbor.LEFT, true);
									}
									this.setConnected(rowCandidatesRow, i, FragmentNeighbor.UP, true);
								}
								this.setConnected(columnCandidatesEndRow, rowCandidatesEndColumn, 
										FragmentNeighbor.UP, true);
								this.setConnected(columnCandidatesEndRow, rowCandidatesEndColumn, 
										FragmentNeighbor.LEFT, true);
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean giveNext(Bitmap nextFragmentImage) {
		if (this.next != null 
				&& nextFragmentImage != null && nextFragmentImage.getWidth() == this.next.getWidth()
				&& nextFragmentImage.getHeight() == this.next.getHeight()) {
			// given image was valid and has correct height and width
			if (this.resultImage == null) {
				// if resulting image not yet created, do so now
				this.resultImage = Bitmap.createBitmap(this.getTotalWidth(), this.getTotalHeight(),
						Bitmap.Config.ARGB_8888);
				mResultCanvas = new Canvas(resultImage);
			}
			mResultCanvas.drawBitmap(nextFragmentImage,
                    lastGivenColumn * getRectWidth(),
                    lastGivenRow * getRectHeight(),
                    null);

			this.next = null; // clear next to show that the lastGiven was satisfied
            mFragmentCount++;
			return true;
		}
        // no image expected or given image did not match requirements
		return false;
	}

	@Override
	public MosaicFragment nextFragment() {
		if (this.lastGivenColumn < 0 || this.lastGivenRow < 0) {
			this.lastGivenColumn = 0;
			this.lastGivenRow = 0;
			this.next = this.unite(this.lastGivenRow, this.lastGivenColumn);
			return this.next;
		} else if (this.next == null) {
			do {
				this.lastGivenColumn++;
				if (this.lastGivenColumn == this.getColumnCount()) {
					this.lastGivenRow++;
					if (this.lastGivenRow < this.getRowCount()) {
						this.lastGivenColumn = 0;
					}
				}
			} while (this.lastGivenRow < this.getRowCount()
					&& (this.isConnected(this.lastGivenRow, this.lastGivenColumn, FragmentNeighbor.LEFT)
					|| this.isConnected(this.lastGivenRow, this.lastGivenColumn, FragmentNeighbor.UP)));
			
			if (this.lastGivenRow >= this.getRowCount()) {
				// reached the last row, end, we got all images
				return null;
			} else {
				this.next = this.unite(this.lastGivenRow, this.lastGivenColumn);
				return this.next;
			}
		} else {
			return this.next;
		}
	}

	/**
	 * Unites all Fragments that are connected to the given Fragment indexed by row/column, 
	 * which must be the top left corner of the rect. As only rects are
	 * created (and only rects need to be united), this is a pretty fast straightforward
	 * method.
	 * @param row The row index.
	 * @param column The column index.
	 * @return A fragment of height equal to the sum of heights of all connected fragments
	 * in a column and of width equal to the sum of widths of all connected fragments
	 * in a row. The average color is the mixed average color of all rects.
	 */
	private MosaicFragment unite(int row, int column) {
		// calculate width of the Fragment rect
		int rectColumnCount = 0;
		int currColumn = column;
		do {
			rectColumnCount++;
			currColumn++;
		} while (currColumn < this.getColumnCount() && this.isConnected(row, currColumn, FragmentNeighbor.LEFT));

		// calculate height of the Fragment rect
		int rectRowCount = 0;
		int currRow = row;
		do {
			rectRowCount++;
			currRow++;
		} while (currRow < this.getRowCount() && this.isConnected(currRow, column, FragmentNeighbor.UP));
		// as all connected ShapeFragments have equal average RGB, I can simly use the corners one
        mFragment.reset(rectColumnCount * this.getRectWidth(), rectRowCount * this.getRectHeight(),
				this.getAverageRGB(row, column));
        return mFragment;
	}
	
	@Override
	public boolean hasAll() {
		return this.nextFragment() == null;
	}

	@Override
	public Bitmap getReconstructed() {
		if (this.hasAll()) {
			Bitmap result = this.resultImage;
			this.resultImage = null;
			return result;
		} else {
			return null;
		}
	}

    @Override
    public int estimatedProgressPercent() {
        return (int) (100 * mFragmentCount / (double) (getColumnCount() * getRowCount()));
    }
}
