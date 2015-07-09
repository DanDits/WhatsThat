package dan.dit.whatsthat.util.image;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.util.jama.Matrix;

/**
 * Created by daniel on 03.07.15.
 */
public class IndexedBitmap {

    private Matrix mMatrix;
    private List<Integer> mColors;

    public IndexedBitmap(Bitmap source) {
        long tic = System.currentTimeMillis();
        makeIndexedMatrix(source);
        Log.d("HomeStuff", "Made indexed bitmap of " + source.getWidth() + "x" + source.getHeight() + " in " + (System.currentTimeMillis() - tic) + "ms with " + mColors.size() + " colors.");
    }

    public IndexedBitmap(Matrix matrix, List<Integer> colors) {
        mMatrix = matrix;
        mColors = colors;
        if (mMatrix == null || mColors == null) {
            throw new IllegalArgumentException("No matrix or colors given.");
        }
    }

    private void makeIndexedMatrix(Bitmap source) {
        mColors = new ArrayList<>();
        Map<Integer, Integer> colorToIndex = new HashMap<>();
        mMatrix = new Matrix(source.getHeight(), source.getWidth());

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int color = source.getPixel(x, y);
                Integer index = colorToIndex.get(color);
                if (index == null) {
                    index = mColors.size();
                    mColors.add(color);
                    colorToIndex.put(color, index);
                }
                mMatrix.set(y, x, index.doubleValue());
            }
        }
    }

    public Matrix getMatrix() {
        return mMatrix;
    }

    public Bitmap convertToBitmap() {
        Bitmap result = Bitmap.createBitmap(mMatrix.getColumnDimension(), mMatrix.getRowDimension(), Bitmap.Config.ARGB_8888);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int index = (int) Math.round(mMatrix.get(y, x));
                index = Math.max(0, Math.min(index, mColors.size() - 1));
                result.setPixel(x, y, mColors.get(index));
            }
        }
        return result;
    }

    public List<Integer> getIndexedColors() {
        return mColors;
    }
}
