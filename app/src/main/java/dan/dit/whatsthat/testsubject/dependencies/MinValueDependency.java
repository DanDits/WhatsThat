package dan.dit.whatsthat.testsubject.dependencies;

/**
 * Created by daniel on 10.06.15.
 */
public class MinValueDependency extends Dependency{
    private final Dependable mDependency;
    private final int mMinValue;

    public MinValueDependency(Dependable dependency, int minValue) {
        mDependency = dependency;
        mMinValue = minValue;
    }

    @Override
    public boolean isFulfilled() {
        return mDependency.getValue() >= mMinValue;
    }
}
