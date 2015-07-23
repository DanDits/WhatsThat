package dan.dit.whatsthat.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 03.07.15.
 */
public class MultistepPercentProgressListener implements PercentProgressListener {
    private PercentProgressListener mListener;
    private List<Double> mWeights = new ArrayList<>();
    private int mCurrentStep;
    private double mCurrentWeight;

    public MultistepPercentProgressListener(PercentProgressListener listener, int steps) {
        mListener = listener;
        reset(steps);
    }

    public MultistepPercentProgressListener(PercentProgressListener listener, double[] weights) {
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
            mListener.onProgressUpdate((int) (PROGRESS_COMPLETE * mCurrentWeight + progress * mWeights.get(mCurrentStep)));
        }
    }

}
