package dan.dit.whatsthat.testsubject.dependencies;

import android.content.res.Resources;

/**
 * Created by daniel on 10.06.15.
 */
public interface Dependable {
    CharSequence getName(Resources res);
    int getValue();
}