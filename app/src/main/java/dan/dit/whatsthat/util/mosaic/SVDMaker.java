package dan.dit.whatsthat.util.mosaic;

import android.graphics.Bitmap;

import java.util.List;

import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.image.IndexedBitmap;
import dan.dit.whatsthat.util.jama.Matrix;
import dan.dit.whatsthat.util.jama.SingularValueDecomposition;
import dan.dit.whatsthat.util.mosaic.data.MosaicMaker;

/**
 * Created by daniel on 17.10.15.
 */
public class SVDMaker {
    private final Matrix mSourceMatrix;
    private final double[] mSingularValues;
    private final Matrix mU;
    private final Matrix mVTransposed;
    private final boolean mTransposeRequired;
    private final List<Integer> mIndexedBitmapColors;
    private boolean mUseIndexedBitmap;
    private final int mRank;

    public SVDMaker(Bitmap base, boolean useIndexedBitmap, MosaicMaker.ProgressCallback callback) {
        mUseIndexedBitmap = useIndexedBitmap;
        Matrix sourceMatrix;
        callback.onProgressUpdate(5);
        if (mUseIndexedBitmap) {
            IndexedBitmap indexedBitmap = new IndexedBitmap(base);
            sourceMatrix = indexedBitmap.getMatrix();
            mIndexedBitmapColors = indexedBitmap.getIndexedColors();
        } else {
            sourceMatrix = BitmapUtil.toMatrix(base);
            mIndexedBitmapColors = null;
        }
        callback.onProgressUpdate(20);
        mTransposeRequired = sourceMatrix.getColumnDimension() > sourceMatrix.getRowDimension(); //
        // so SingularValueDecomposition works...
        if (mTransposeRequired) {
            sourceMatrix = sourceMatrix.transpose();
        }
        mSourceMatrix = sourceMatrix;
        callback.onProgressUpdate(25);

        SingularValueDecomposition decomposition = new SingularValueDecomposition(sourceMatrix);
        callback.onProgressUpdate(80);
        mRank = decomposition.rank();
        mSingularValues = decomposition.getSingularValues();
        mU = decomposition.getU();
        mVTransposed = decomposition.getV().transpose();
    }

    public int getMaxRank() {
        return mRank;
    }

    public Bitmap getRankApproximation(int rank) {

        Matrix resultMatrix = mU.diagTimes(mSingularValues, rank, mVTransposed);
        if (mTransposeRequired) {
            resultMatrix = resultMatrix.transpose();
        }

        Bitmap result;
        if (mUseIndexedBitmap) {
            IndexedBitmap resultIndexedBitmap = new IndexedBitmap(resultMatrix, mIndexedBitmapColors);
            result = resultIndexedBitmap.convertToBitmap();
        } else {
            result = BitmapUtil.toBitmap(resultMatrix);
        }
        return result;
    }
}
