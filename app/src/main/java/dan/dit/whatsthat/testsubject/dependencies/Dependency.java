package dan.dit.whatsthat.testsubject.dependencies;

import android.content.res.Resources;

/**
 * Created by daniel on 10.06.15.
 */
public abstract class Dependency {

    public abstract boolean isNotFulfilled();

    public abstract CharSequence getName(Resources res);
}
