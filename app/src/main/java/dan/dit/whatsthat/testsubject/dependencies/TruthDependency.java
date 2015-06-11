package dan.dit.whatsthat.testsubject.dependencies;

import dan.dit.whatsthat.testsubject.wallet.WalletEntry;

/**
 * Created by daniel on 10.06.15.
 */
public class TruthDependency extends Dependency {
    private final Dependable mDependency;

    public TruthDependency(Dependable dependency) {
        mDependency = dependency;
    }

    @Override
    public boolean isFulfilled() {
        return mDependency.getValue() == WalletEntry.TRUE;
    }

}
