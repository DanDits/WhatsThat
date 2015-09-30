package dan.dit.whatsthat.util.dependencies;

import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 30.09.15.
 */
public class OrDependency extends Dependency {
    private List<Dependency> mCandidates = new ArrayList<>(4);
    public OrDependency(Dependency first, Dependency second) {
        add(first);
        add(second);
    }

    public OrDependency() {}

    public OrDependency add(Dependency dep) {
        if (dep != null) {
            mCandidates.add(dep);
        }
        return this;
    }

    @Override
    public boolean isNotFulfilled() {
        boolean anyFulfilled = false;
        for (Dependency dep : mCandidates) {
            if (!dep.isNotFulfilled()) {
                anyFulfilled = true;
                break;
            }
        }
        return !anyFulfilled;
    }

    @Override
    public CharSequence getName(Resources res) {
        if (mCandidates.size() == 0) {
            return "";
        } else if (mCandidates.size() == 1) {
            return mCandidates.get(0).getName(res);
        } else {
            StringBuilder builder = new StringBuilder();
            boolean separateSymbol = false;
            for (Dependency dep : mCandidates) {
                if (separateSymbol) {
                    builder.append(' ')
                            .append(res.getString(R.string.dependency_or))
                            .append(' ');
                }
                builder.append(dep.getName(res));
                separateSymbol = true;
            }
            return builder.toString();
        }
    }
}
