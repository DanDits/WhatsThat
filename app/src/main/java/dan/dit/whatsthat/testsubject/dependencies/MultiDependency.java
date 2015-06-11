package dan.dit.whatsthat.testsubject.dependencies;

import java.util.List;

/**
 * Created by daniel on 10.06.15.
 */
public class MultiDependency extends Dependency{
    private final List<Dependency> mDependencies;

    public MultiDependency(List<Dependency> dependencies) {
        mDependencies = dependencies;
        if (mDependencies == null) {
            throw new IllegalArgumentException("No dependencies");
        }
    }

    @Override
    public boolean isFulfilled() {
        boolean fulfilled = true;
        for (Dependency dep : mDependencies) {
            if (!dep.isFulfilled()) {
                fulfilled = false;
            }
        }
        return fulfilled;
    }
}
