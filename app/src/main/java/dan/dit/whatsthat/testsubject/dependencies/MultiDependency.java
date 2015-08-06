package dan.dit.whatsthat.testsubject.dependencies;

import android.content.res.Resources;

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
    public boolean isNotFulfilled() {
        boolean fulfilled = true;
        for (Dependency dep : mDependencies) {
            if (dep.isNotFulfilled()) {
                fulfilled = false;
            }
        }
        return !fulfilled;
    }

    @Override
    public CharSequence getName(Resources res) {
        String sequence = "";
        for (Dependency dep : mDependencies) {
            sequence += dep.getName(res);
        }
        return sequence;
    }
}
