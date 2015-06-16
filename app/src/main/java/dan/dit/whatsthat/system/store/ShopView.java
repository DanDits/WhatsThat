package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * Created by daniel on 12.06.15.
 */
public class ShopView extends ListView implements  StoreContainer {
    public ShopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void refresh(FragmentActivity activity) {
        Log.d("HomeStuff", "Refreshing shop view.");
    }

    @Override
    public void stop(FragmentActivity activity) {

    }

    @Override
    public View getView() {
        return this;
    }
}
