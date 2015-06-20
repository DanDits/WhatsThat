package dan.dit.whatsthat.testsubject.dependencies;

import android.content.res.Resources;

import dan.dit.whatsthat.testsubject.wallet.WalletEntry;

/**
 * Created by daniel on 10.06.15.
 */
public class TruthDependency extends Dependency {
    private final Dependable mDependency;

    public TruthDependency(Dependable dependency) {
        mDependency = dependency;
        if (mDependency == null) {
            throw new IllegalArgumentException("No dependency given.");
        }
    }

    @Override
    public boolean isFulfilled() {
        return mDependency.getValue() == WalletEntry.TRUE;
    }

    @Override
    public CharSequence getName(Resources res) {
        return mDependency.getName(res);
    }

}
