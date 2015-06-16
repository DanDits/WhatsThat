package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 11.06.15.
 */
public class CreditsView extends WebView implements StoreContainer {

    public CreditsView(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    public void refresh(FragmentActivity activity) {
        loadData(getContext().getString(R.string.credits_text), "text/html", "UTF-8");
        requestLayout();
        invalidate();
    }

    @Override
    public void stop(FragmentActivity activity) {

    }

    @Override
    public View getView() {
        return getRootView();
    }
}
