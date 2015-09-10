package dan.dit.whatsthat.image;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 09.09.15.
 */
public class BundleManager {

    private final Activity mActivity;
    private ExpandableListView mView;

    public BundleManager(Activity activity) {
        mActivity = activity;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = (ExpandableListView) inflater.inflate(R.layout.workshop_bundle_manager, null);
    }

    public View getView() {
        return mView;
    }

    public void onBundleCreated(String origin, String bundleName) {
        //TODO update adapter
    }


    private class BundlesAdapter extends BaseExpandableListAdapter {
        private final LayoutInflater mInflater;
        private Context mContext;

        public BundlesAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getGroupCount() {
            return 0;
        }

        @Override
        public int getChildrenCount(int i) {
            return 0;
        }

        @Override
        public Object getGroup(int i) {
            return null;
        }

        @Override
        public Object getChild(int i, int i1) {
            return null;
        }

        @Override
        public long getGroupId(int i) {
            return i + 1;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1 + 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int childPosition) {
            return false;
        }
    }
}
