package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TabHost;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.util.ui.UiStyleUtil;

/**
 * Created by daniel on 02.09.15.
 */
public class WorkshopView extends FrameLayout implements StoreContainer {
    private static final String TAB_BUNDLE_CREATOR = "bundle_creator";
    private static final String TAB_MOSAIC = "mosaic";
    private TabHost mTabHost;
    private BundleCreator mBundleCreator;

    public WorkshopView(Context context) {
        super(context);
    }

    public WorkshopView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorkshopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        titleBackButton.setText(R.string.store_category_workshop);
        if (mTabHost == null) {
            mTabHost = (TabHost) getRootView().findViewById(android.R.id.tabhost);
            initializeTabHost(activity);
        }
    }

    private void initializeTabHost(FragmentActivity activity) {
        mTabHost.setup();
        TabFactory factory = new TabFactory(activity);
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_BUNDLE_CREATOR).setIndicator(getResources().getString(R.string.workshop_tab_bundle_creator)));
        addTab(mTabHost, factory, mTabHost.newTabSpec(TAB_MOSAIC).setIndicator(getResources().getString(R.string.workshop_tab_mosaic)));

        UiStyleUtil.setTabHostSelector(mTabHost, R.drawable.tab_widget_selector_alien);
    }

    private void addTab(TabHost tabHost, TabFactory factory, TabHost.TabSpec tabSpec) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(factory);
        tabHost.addTab(tabSpec);
    }

    private class TabFactory implements TabHost.TabContentFactory {

        private final LayoutInflater mInflater;

        public TabFactory(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View createTabContent(String tag) {
            if (tag.equals(TAB_BUNDLE_CREATOR)) {
                mBundleCreator = new BundleCreator(mInflater);
                return mBundleCreator.getView();
            } else if (tag.equals(TAB_MOSAIC)) {
                return mInflater.inflate(R.layout.workshop_mosaic, null);
            }
            return null;
        }

    }

    @Override
    public void stop(FragmentActivity activity) {

    }

    @Override
    public View getView() {
        return this;
    }
}
