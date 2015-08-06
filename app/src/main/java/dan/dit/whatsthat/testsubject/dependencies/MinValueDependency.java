package dan.dit.whatsthat.testsubject.dependencies;

import android.content.res.Resources;

/**
 * Created by daniel on 10.06.15.
 */
public class MinValueDependency extends Dependency{
    private final Dependable mDependency;
    private final int mMinValue;

    public final int getMinValue() {
        return mMinValue;
    }

    public MinValueDependency(Dependable dependency, int minValue) {
        mDependency = dependency;
        mMinValue = minValue;
        if (mDependency == null) {
            throw new IllegalArgumentException("No dependency given.");
        }
    }

    @Override
    public boolean isNotFulfilled() {
        return mDependency.getValue() < mMinValue;
    }

    @Override
    public CharSequence getName(Resources res) {
        return mDependency.getName(res);
    }

    @Override
    public String toString() {
        return "DEP: value (" + mDependency.getValue() + ") >= " + mMinValue;
    }
}
