package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.ui.LinearLayoutProgressBar;

/**
 * Created by daniel on 12.06.15.
 */
public class ShopView extends ExpandableListView implements  StoreContainer, ShopArticleHolder.OnArticleChangedListener {
    private Button mTitleBackButton;
    private ShopArticleAdapter mAdapter;
    private ShopArticleHolder mArticleHolder;
    private final LayoutInflater mInflater;

    public ShopView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setGroupIndicator(null);
        setOnGroupExpandListener(new OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupIndex) {
                for (int i = 0; i < mArticleHolder.getArticlesCount(); i++) {
                    if (i != groupIndex) {
                        collapseGroup(i);
                    }
                }
            }
        });
        setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                if (groupPosition >= 0 && groupPosition < mArticleHolder.getArticlesCount()) {
                    mArticleHolder.getArticle(groupPosition).getSubProduct(mInflater, childPosition).onClick();
                    return true;
                }
                return false;
            }
        });
    }

    private void updateTitleBackButton() {
        mTitleBackButton.setText(getContext().getString(R.string.store_category_shop, mArticleHolder.getCurrentScore()));
    }

    @Override
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        mTitleBackButton = titleBackButton;
        if (mAdapter == null) {
            mArticleHolder = TestSubject.getInstance().getShopSortiment();
            mAdapter = new ShopArticleAdapter();
            setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
        mArticleHolder.setOnArticleChangedListener(this);
        updateTitleBackButton();
    }

    @Override
    public void stop(FragmentActivity activity) {
        mArticleHolder.setOnArticleChangedListener(null);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onArticleChanged(ShopArticle article) {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            updateTitleBackButton();
        }
    }

    private class ShopArticleAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return mArticleHolder.getArticlesCount();
        }

        @Override
        public int getChildrenCount(int i) {
            return mArticleHolder.getArticle(i).getSubProductCount();
        }

        @Override
        public Object getGroup(int i) {
            return mArticleHolder.getArticle(i);
        }

        @Override
        public Object getChild(int i, int i1) {
            return mArticleHolder.getArticle(i).getSubProduct(mInflater, i1);
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
            View name = convertView == null ? null : convertView.findViewById(R.id.shop_article_name);
            if (name == null) {
                convertView = mInflater.inflate(R.layout.shop_article, null);
                name = convertView.findViewById(R.id.shop_article_name);
            }
            ShopArticle article = mArticleHolder.getArticle(groupPosition);
            ((TextView) name).setText(article.getName(getResources()));
            int imageResId = article.getIconResId();
            if (imageResId != 0) {
                ((ImageView) convertView.findViewById(R.id.shop_article_image)).setImageResource(imageResId);
            }
            ((TextView) convertView.findViewById(R.id.shop_article_descr)).setText(article.getDescription(getResources()));
            ((TextView) convertView.findViewById(R.id.shop_article_cost)).setText(article.getSpentScore(getResources()));

            LinearLayoutProgressBar progressListener = ((LinearLayoutProgressBar) convertView.findViewById(R.id.progress_bar));
            int progressPercent = article.getPurchaseProgressPercent();
            if (progressPercent >= PercentProgressListener.PROGRESS_COMPLETE) {
                progressListener.onProgressUpdate(0);
                convertView.setBackgroundColor(progressListener.getStartColor());
            } else {
                convertView.setBackgroundColor(progressListener.getEndColor());
                progressListener.onProgressUpdate(progressPercent);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ShopArticle article = mArticleHolder.getArticle(groupPosition);
            SubProduct product = article.getSubProduct(mInflater, childPosition);
            if (product != null) {
                convertView = product.getView();
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int childPosition) {
            ShopArticle article = mArticleHolder.getArticle(i);
            return article != null && article.isClickable(childPosition);
        }
    }
}