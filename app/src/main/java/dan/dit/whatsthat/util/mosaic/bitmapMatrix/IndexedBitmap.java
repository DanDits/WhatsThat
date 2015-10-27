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

package dan.dit.whatsthat.util.mosaic.bitmapMatrix;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.util.jama.Matrix;

/**
 * Created by daniel on 03.07.15.
 */
public class IndexedBitmap implements BitmapMatrix {

    private Matrix mMatrix;
    private List<Integer> mColors;
    private boolean mTransposeRequired;

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
        SparseArray<Integer> colorToIndex = new SparseArray<>();
        mTransposeRequired = source.getWidth() > source.getHeight();
        int columns = Math.min(source.getWidth(), source.getHeight());
        int rows = Math.max(source.getWidth(), source.getHeight());
        mMatrix = new Matrix(rows, columns);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int color = source.getPixel(mTransposeRequired ? y : x, mTransposeRequired ? x : y);
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
}
