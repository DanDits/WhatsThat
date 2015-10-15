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

package dan.dit.whatsthat.riddle.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;

import java.util.Arrays;
import java.util.Map;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageBitmapSource;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.system.RiddleFragment;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectToast;
import dan.dit.whatsthat.util.MultistepPercentProgressListener;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.jama.Matrix;
import dan.dit.whatsthat.util.jama.SingularValueDecomposition;
import dan.dit.whatsthat.util.mosaic.data.MosaicMaker;
import dan.dit.whatsthat.util.mosaic.matching.SimpleLinearTileMatcher;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;

/**
 * Riddle for testing images and other bitmap features. NOT MEANT FOR USERS since there
 * is no riddle in just showing the image straight with no obfuscation and nothing to do!
 * Created by daniel on 17.04.15.
 */
public class RiddleDeveloper extends RiddleGame {

    public RiddleDeveloper(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
        super(riddle, image, bitmap, res, config, listener);
    }

    @Override
    protected void initAchievementData() {

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void initBitmap(Resources res, PercentProgressListener listener) {
    }

    @Override
    public boolean onMotionEvent(MotionEvent event) {
        return false;
    }

    private void makeRankApproximation(PercentProgressListener progress) {
        MultistepPercentProgressListener multiProgress = new MultistepPercentProgressListener(progress, new double[] {0.05, 0.7, 0.25});

        long totalTic = System.currentTimeMillis();

        //IndexedBitmap indexedBitmap = new IndexedBitmap(mBitmap);
        //Matrix sourceMatrix = indexedBitmap.getMatrix();
        Matrix sourceMatrix = BitmapUtil.toMatrix(mBitmap);

        boolean transposeRequired = sourceMatrix.getColumnDimension() > sourceMatrix.getRowDimension(); // so SingularValueDecomposition works...
        if (transposeRequired) {
            sourceMatrix = sourceMatrix.transpose();
        }
        multiProgress.nextStep();

        SingularValueDecomposition decomposition = new SingularValueDecomposition(sourceMatrix);
        multiProgress.nextStep();

        int rank = decomposition.rank();
        int[] rankApproximations = new int[] {1, 2, 3, 4, 5, 10, 50, 100, 150, rank -5, rank - 2, rank -1, rank};
        double[] singularValues = decomposition.getSingularValues();
        Log.d("Riddle", "Calculates singular values: " +  Arrays.toString(singularValues));
        Matrix U = decomposition.getU();
        Matrix VT = decomposition.getV().transpose();

        int index = 1;
        MultistepPercentProgressListener subProgress = new MultistepPercentProgressListener(multiProgress, rankApproximations.length);

        for (int rankApproximation : rankApproximations) {
            long tic = System.currentTimeMillis();
            Matrix resultMatrix = U.diagTimes(singularValues, rankApproximation, VT);
            if (transposeRequired) {
                resultMatrix = resultMatrix.transpose();
            }

            //IndexedBitmap resultIndexedBitmap = new IndexedBitmap(resultMatrix, indexedBitmap.getIndexedColors());
            //mGallery[index] = resultIndexedBitmap.convertToBitmap();
            BitmapUtil.toBitmap(resultMatrix);

            Log.d("Riddle", "Made rank " + rankApproximation + " approximation in " + (System.currentTimeMillis() - tic) + "ms.");
            subProgress.nextStep();
            index++;
        }
        multiProgress.nextStep();
        Log.d("Riddle", "Made rank approximation " + Arrays.toString(rankApproximations) + " of total rank " + rank + " in " + (System.currentTimeMillis() - totalTic) + "ms.");

    }

    @NonNull
    @Override
    protected String compactCurrentState() {
        return "";
    }

}
