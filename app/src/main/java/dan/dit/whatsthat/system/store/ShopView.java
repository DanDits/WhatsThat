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

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleFilter;
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleFilterImportant;
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleFilterIcon;
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleFilterPurchased;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.testsubject.shopping.sortiment.LevelUpArticle;
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
    private ViewGroup mFilterHolder;
    private TextView mCurrency;

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
        mTitleBackButton.setText(getContext().getString(R.string.store_category_shop));
    }

    private void updateCurrency() {
        mCurrency.setText(String.valueOf(mArticleHolder.getCurrentScore()));
    }

    @Override
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        mTitleBackButton = titleBackButton;
        mCurrency = (TextView) getRootView().findViewById(R.id.currency);
        if (mAdapter == null) {
            mAdapter = new ShopArticleAdapter();
            mArticleHolder = TestSubject.getInstance().getShopSortiment();
            initFilters();
            setAdapter(mAdapter);
        } else {
            applyFilters();
        }
        mArticleHolder.setOnArticleChangedListener(this);
        updateTitleBackButton();
        updateCurrency();
    }

    private void initFilters() {
        mFilterHolder = (ViewGroup) getRootView().findViewById(R.id.shop_filters);

        //determine the set of the articles' icons
        List<Integer> iconIds = new ArrayList<>();
        for (ShopArticle article : mArticleHolder.getAllArticles()) {
            int id = article.getIconResId();
            if (!iconIds.contains(id)) {
                iconIds.add(id);
            }
        }

        // add icon filters and filter for not purchased articles only
        List<ShopArticleFilter> filters = new ArrayList<>();
        for (Integer iconId : iconIds) {
            PracticalRiddleType lastVisibleType = Riddle.getLastVisibleRiddleType(getContext());
            filters.add(new ShopArticleFilterIcon(iconId, iconId, lastVisibleType != null && lastVisibleType.getIconResId() == iconId));
        }
        filters.add(new ShopArticleFilterPurchased(R.drawable.icon_filter_progress_complete, true, false));
        filters.add(new ShopArticleFilterPurchased(R.drawable.icon_filter_progress, false, false));
        filters.add(new ShopArticleFilterImportant());

        // init filters and filter views and listeners
        mArticleHolder.setFilters(filters);
        mFilterHolder.removeAllViews();
        for (ShopArticleFilter filter : filters) {
            if (!filter.isVisible()) {
                continue;
            }
            ImageView image = new ImageView(getContext());
            image.setImageResource(filter.getIcon());
            applyFilterToImage(image, filter);
            image.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int index = mFilterHolder.indexOfChild(view);
                    List<ShopArticleFilter> filters = mArticleHolder.getFilters();
                    if (filters != null && index >= 0 && index < filters.size()) {
                        ShopArticleFilter filter = filters.get(index);
                        filter.setActive(!filter.isActive());
                        ImageView image = (ImageView) view;
                        applyFilterToImage(image, filter);
                        applyFilters();
                    }
                }
            });
            mFilterHolder.addView(image);
        }
    }

    private void applyFilterToImage(ImageView image, ShopArticleFilter filter) {
        if (filter.isActive()) {
            image.setImageAlpha(255);
        } else {
            image.setImageAlpha(70);
        }
    }

    private void applyFilters() {
        mArticleHolder.applyFilters();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void stop(FragmentActivity activity) {
        mArticleHolder.setOnArticleChangedListener(null);
        mArticleHolder.closeArticles();
    }

    @Override
    public View getView() {
        return getRootView();
    }

    @Override
    public void onArticleChanged(ShopArticle article) {
        if (mAdapter != null) {
            applyFilters();
            updateCurrency();
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
            TextView costView = ((TextView) convertView.findViewById(R.id.shop_article_cost));
            costView.setText(article.getSpentScore(getResources()));
            if (costView.getText().length()  > 0) {
                costView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.think_currency_small, 0);
            } else {
                costView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

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