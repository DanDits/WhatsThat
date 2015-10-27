package dan.dit.whatsthat.util.mosaic.bitmapMatrix;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import dan.dit.whatsthat.util.jama.Matrix;

/**
 * Created by daniel on 22.10.15.
 */
public class SplitRgbBitmap implements BitmapMatrix {
    private Matrix mMatrix;
    private boolean mTransposeRequired;

    public SplitRgbBitmap(Bitmap bitmap) {
        mTransposeRequired = bitmap.getWidth() > bitmap.getHeight() * 3;
        final int width = Math.min(bitmap.getWidth(), bitmap.getHeight() * 3);
        final int height = Math.max(bitmap.getWidth(), bitmap.getHeight() * 3);
        Matrix matrix = new Matrix(height, width);
        for (int i = 0; i < bitmap.getHeight(); i++) { //
            Log.d("HomeStuff", "Reached row " + i + "/" + height);
            for (int j = 0; j < bitmap.getWidth(); j++) {
                final int color = bitmap.getPixel(j, i);
                if (mTransposeRequired) {
                    int column = i * 3;
                    matrix.set(j, column, Color.red(color));
                    matrix.set(j, column + 1, Color.green(color));
                    matrix.set(j, column + 2, Color.blue(color));
                } else {
                    int row = i * 3;
                    matrix.set(row, j, Color.red(color));
                    matrix.set(row + 1, j, Color.green(color));
                    matrix.set(row + 2, j, Color.blue(color));
                }
            }
        }
        mMatrix = matrix;
    }

    public SplitRgbBitmap(Matrix matrix) {
        mMatrix = matrix;
        if (matrix == null) {
            throw new IllegalArgumentException("No valid matrix given.");
        }
    }

    public Bitmap convertToBitmap() {
        Bitmap result = mTransposeRequired ?
                Bitmap.createBitmap(mMatrix.getRowDimension(), mMatrix.getColumnDimension() / 3,
                        Bitmap.Config.ARGB_8888)
                : Bitmap.createBitmap(mMatrix.getColumnDimension(), mMatrix
                    .getRowDimension() / 3, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int color;
                if (mTransposeRequired) {
                    color = Color.argb(255,
                            getColorValue(y, x * 3),
                            getColorValue(y, x * 3 + 1),
                            getColorValue(y, x * 3 + 2));
                } else {
                    color = Color.argb(255,
                            getColorValue(y * 3, x),
                            getColorValue(y * 3 + 1, x),
                            getColorValue(y * 3 + 2, x));
                }
                result.setPixel(x, y, color);
            }
        }
        return result;
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

    private int getColorValue(int row, int column) {
        int value = (int) mMatrix.get(row, column);
        return value < 255 ? (value > 0 ? value : 0) : 255;
    }

    public Matrix getMatrix() {
        return mMatrix;
    }
}
