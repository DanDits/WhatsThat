package dan.dit.whatsthat.system.store;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by daniel on 11.06.15.
 */
public interface StoreContainer {
    void refresh(FragmentActivity activity, Button titleBackButton);
    void stop(FragmentActivity activity);
    View getView();
}
