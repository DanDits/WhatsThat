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

package dan.dit.whatsthat.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps a PercentProgressListener. Allows to divide the progress to be divided into
 * multiple blocks. Each block can be weighted differently. If the amount of steps is given, each
 * block will be weighted uniformly.
 * To advance to the next step you need to manually invoke nextStep(). This can but does not have
 * to be invoked after the last step was completed. Each step allows invoking this progress listener
 * from a fresh range of 0% to 100%. This progress will be added to the already reached weighted
 * progress when notifying the wrapped listener.
 * Created by daniel on 03.07.15.
 */
public class MultistepPercentProgressListener implements PercentProgressListener {
    private PercentProgressListener mListener;
    private List<Double> mWeights = new ArrayList<>();
    private int mCurrentStep;
    private double mCurrentWeight;

    /**
     * Creates a new MultistepPercentProgressListener dividing the progress into the given amount
     * of steps. These steps will be uniformly weighted.
     * @param listener The listener to wrap.
     * @param steps The amount of steps. Must be positive.
     */
    public MultistepPercentProgressListener(@NonNull PercentProgressListener listener, int steps) {
        mListener = listener;
        reset(steps);
    }

    /**
     * Creates a new MultistepPercentProgressListener dividing the progress into the given weights.
     * There must be at least one weight and each array entry must be a value between zero and
     * one (inclusive). Should sum up to 1, this is NOT checked.
     * @param listener The listener to wrap.
     * @param weights The weights for each step.
     */
    public MultistepPercentProgressListener(@NonNull PercentProgressListener listener,
                                            @NonNull double[] weights) {
        mListener = listener;
        reset(weights);
    }

    private MultistepPercentProgressListener reset(int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("No steps given");
        }
        reset();
        // equally distributed
        for (int i = 0; i < steps; i++) {
            mWeights.add(1./ (double) steps);
        }
        return this;
    }

    private MultistepPercentProgressListener reset(double[] weights) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("No weights given.");
        }
        reset();
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] >= 0. && weights[i] <= 1.) {
                mWeights.add(weights[i]);
            } else {
                throw new IllegalArgumentException("Negative or weight > 1 given: " + weights[i]);
            }
        }
        return this;
    }

    private MultistepPercentProgressListener reset() {
        mWeights.clear();
        mCurrentStep = 0;
        mCurrentWeight = 0.;
        return this;
    }

    /**
     * Advances the multistep listener to the next step. Updates the progress of the wrapped
     * listener, but does not necessarily increase it, if the last step was already on 100%.
     */
    public void nextStep() {
        mCurrentWeight += mWeights.get(mCurrentStep);
        mCurrentStep++;
        notifyProgress(0);
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (mCurrentStep >= mWeights.size()) {
            return;
        }
        notifyProgress(progress);
    }

    private void notifyProgress(int progress) {
        if (mCurrentStep >= mWeights.size()) {
            mListener.onProgressUpdate(PROGRESS_COMPLETE);
        } else {
            mListener.onProgressUpdate((int) (PROGRESS_COMPLETE * mCurrentWeight
                    + Math.min(progress, PercentProgressListener.PROGRESS_COMPLETE) *
                        mWeights.get(mCurrentStep)));
        }
    }

}
