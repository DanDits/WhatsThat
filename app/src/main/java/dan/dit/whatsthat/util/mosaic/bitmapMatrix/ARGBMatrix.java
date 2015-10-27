package dan.dit.whatsthat.util.mosaic.bitmapMatrix;

import android.graphics.Bitmap;
import android.graphics.Color;

import dan.dit.whatsthat.util.jama.Matrix;

/**
 * Created by daniel on 22.10.15.
 */
public class ARGBMatrix implements BitmapMatrix {

    private boolean mTransposeRequired;
    private Matrix mMatrix;

    public ARGBMatrix(Bitmap source) {
        if (source == null) {
            throw new IllegalArgumentException("No bitmap source given.");
        }
        setMatrix(source);
    }

    public ARGBMatrix(Matrix matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("No matrix given.");
        }
        mMatrix = matrix;
    }

    private void setMatrix(Bitmap source) {
        mTransposeRequired = source.getWidth() > source.getHeight();
        final int rows = Math.max(source.getWidth(), source.getHeight());
        final int columns = Math.min(source.getWidth(), source.getHeight());
        Matrix matrix = new Matrix(rows, columns);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                matrix.set(y, x, pixelToValue(source.getPixel(mTransposeRequired ? y : x,
                        mTransposeRequired ? x : y)));
            }
        }
        mMatrix = matrix;
    }

    @Override
    public boolean updateMatrix(Matrix matrix) {
        if (matrix == null) {
            return false;
        }
        if (mTransposeRequired) {
            matrix = matrix.transpose();
        }
        mMatrix = matrix;
        return true;
    }

    // tried 1024 and 512, same quality, bot not really well for lower rank approximations
    private static final long OFFSET_FACTOR = 512; // at least 256 since this is (2^8) the
    // number of different values per color
    private static double pixelToValue(int pixelARGB) {
        return (double) (Color.alpha(pixelARGB) * OFFSET_FACTOR * OFFSET_FACTOR * OFFSET_FACTOR
                + Color.red(pixelARGB) * OFFSET_FACTOR * OFFSET_FACTOR
                + Color.green(pixelARGB) * OFFSET_FACTOR
                + Color.blue(pixelARGB));
    }

    private static int valueToPixel(double value) {
        long valueL = (long) value;
        return Color.argb(
                toColorValue((valueL / (OFFSET_FACTOR * OFFSET_FACTOR * OFFSET_FACTOR)) %
                                OFFSET_FACTOR),
                toColorValue((valueL / (OFFSET_FACTOR * OFFSET_FACTOR)) % OFFSET_FACTOR),
                toColorValue((valueL / OFFSET_FACTOR) % OFFSET_FACTOR),
                toColorValue(valueL % OFFSET_FACTOR));
    }

    private static int toColorValue(long value) {
        return value <= 255L ? (value >= 0L ? (int) value : 0) : 255;
    }

    @Override
    public Bitmap convertToBitmap() {
        Bitmap result = Bitmap.createBitmap(mMatrix.getColumnDimension(), mMatrix.getRowDimension(),
                Bitmap.Config.ARGB_8888);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                result.setPixel(x, y, valueToPixel(mMatrix.get(y, x)));
            }
        }
        return result;
    }

    @Override
    public Matrix getMatrix() {
        return mMatrix;
    }
}
