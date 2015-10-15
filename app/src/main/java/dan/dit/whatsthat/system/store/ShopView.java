/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
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
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleGroupFilter;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.ui.ImageViewWithText;
import dan.dit.whatsthat.util.ui.LinearLayoutProgressBar;

/**
 * Created by daniel on 12.06.15.
 */
public class ShopView extends ExpandableListView implements  StoreContainer, ShopArticleHolder.OnArticleChangedListener, ShopArticleGroupFilter.OnFilterUpdateListener {
    private Button mTitleBackButton;
    private ShopArticleAdapter mAdapter;
    private ShopArticleHolder mArticleHolder;
    private final LayoutInflater mInflater;
    private ViewGroup mFilterHolder;
    private TextView mCurrency;
    private ViewGroup mChildrenFilterHolder;
    private int mExpandedGroup;

    public ShopView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setGroupIndicator(null);
        mExpandedGroup = -1;
        setOnGroupExpandListener(new OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupIndex) {
                mExpandedGroup = groupIndex;
                for (int i = 0; i < mArticleHolder.getArticlesCount(); i++) {
                    if (i != groupIndex) {
                        collapseGroup(i);
                    }
                }
            }
        });
        setOnGroupCollapseListener(new OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                if (groupPosition == mExpandedGroup) {
                    mExpandedGroup = -1;
                }
            }
        });
        setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d("Riddle", "OnChild click: " + groupPosition + " child " + childPosition + " total articles: " + mArticleHolder.getArticlesCount());
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
        mChildrenFilterHolder = (ViewGroup) getRootView().findViewById(R.id.shop_child_filters);
        //determine the set of the articles' icons
        List<Integer> riddleIconIds = new ArrayList<>();
        List<Integer> otherIconIds = new ArrayList<>();
        for (ShopArticle article : mArticleHolder.getAllArticles()) {
            int id = article.getIconResId();
            boolean isRiddleIcon = false;
            for (PracticalRiddleType type : PracticalRiddleType.ALL_PLAYABLE_TYPES) {
                if (type.getIconResId() == id) {
                    isRiddleIcon = true;
                    break;
                }
            }
            if (isRiddleIcon) {
                if (!riddleIconIds.contains(id)) {
                    riddleIconIds.add(id);
                }
            } else {
                if (!otherIconIds.contains(id)) {
                    otherIconIds.add(id);
                }
            }
        }


        List<ShopArticleFilter> riddleFilters = new ArrayList<>();
        for (Integer iconId : riddleIconIds) {
            PracticalRiddleType lastVisibleType = Riddle.getLastVisibleRiddleType(getContext());
            riddleFilters.add(new ShopArticleFilterIcon(iconId, iconId, lastVisibleType != null && lastVisibleType.getIconResId() == iconId));
        }
        ShopArticleGroupFilter riddleGroupFilter = new ShopArticleGroupFilter(mChildrenFilterHolder, R.drawable.icon_laboratory, riddleFilters, false, this);

        List<ShopArticleFilter> rootFilters = new ArrayList<>();
        rootFilters.add(new ShopArticleFilterImportant(R.drawable.icon_important));
        rootFilters.add(riddleGroupFilter);
        for (Integer iconId : otherIconIds) {
            rootFilters.add(new ShopArticleFilterIcon(iconId, iconId, false));
        }
        rootFilters.add(new ShopArticleFilterPurchased(R.drawable.icon_filter_progress_complete, true, false));
        rootFilters.add(new ShopArticleFilterPurchased(R.drawable.icon_filter_progress, false, false));
        ShopArticleGroupFilter rootFilter = new ShopArticleGroupFilter(mFilterHolder, 0, rootFilters, true, this);

        // init filters and filter views and listeners
        mArticleHolder.setFilter(rootFilter);
    }

    private void applyFilters() {
        mArticleHolder.applyFilters();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
        mArticleHolder.setOnArticleChangedListener(null);
        mArticleHolder.closeArticles();
    }

    @Override
    public View getView() {
        return getRootView();
    }

    @Override
    public void onArticleChanged(ShopArticle article) {
        Log.d("Riddle", "OnArticleChanged: " + article + " adapter= " + mAdapter);
        if (mAdapter != null) {
            applyFilters();
            updateCurrency();
            // check if the current article isnt visible anymore, if yes then make sure all other articles are collapsed
            boolean isVisible = false;
            for (int i = 0; i < mArticleHolder.getArticlesCount(); i++) {
                if (mArticleHolder.getArticle(i) == article) {
                    isVisible = true;
                    break;
                }
            }
            if (!isVisible) {
                for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                    collapseGroup(i);
                }
            }
        }
    }

    @Override
    public void onFilterUpdate() {
        applyFilters();
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
            if (mExpandedGroup == -1 || isExpanded) {
                convertView.setAlpha(1.f);
            } else {
                convertView.setAlpha(0.25f);
            }
            ShopArticle article = mArticleHolder.getArticle(groupPosition);
            ((TextView) name).setText(article.getName(getResources()));
            int imageResId = article.getIconResId();
            if (imageResId != 0) {
                ((ImageView) convertView.findViewById(R.id.shop_article_image)).setImageResource(imageResId);
            }
            ((TextView) convertView.findViewById(R.id.shop_article_descr)).setText(article.getDescription(getResources()));
            ImageViewWithText costView = ((ImageViewWithText) convertView.findViewById(R.id.shop_article_cost));
            costView.setText(article.getSpentScore(getResources()).toString());
            costView.setVisibility(costView.getText().length()  > 0 ? View.VISIBLE : View.GONE);

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