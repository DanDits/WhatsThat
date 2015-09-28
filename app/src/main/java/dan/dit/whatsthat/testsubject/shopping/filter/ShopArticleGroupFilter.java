package dan.dit.whatsthat.testsubject.shopping.filter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import dan.dit.whatsthat.testsubject.shopping.ShopArticle;

/**
 * Created by daniel on 28.09.15.
 */
public class ShopArticleGroupFilter extends ShopArticleFilter {
    private final ViewGroup mFilterHolder;
    private final List<ShopArticleFilter> mChildFilters;
    private final OnFilterUpdateListener mListener;

    public interface OnFilterUpdateListener {
        void onFilterUpdate();
    }

    public ShopArticleGroupFilter(ViewGroup childContainer, int iconResId, List<ShopArticleFilter> childFilters, boolean root,
                           OnFilterUpdateListener listener) {
        super(iconResId);
        mFilterHolder = childContainer;
        mChildFilters = childFilters;
        mVisible = !root;
        setActive(root);
        mListener = listener;
        initFilters();
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (active) {
            mFilterHolder.setVisibility(View.VISIBLE);
        } else {
            mFilterHolder.setVisibility(View.GONE);
        }
    }

    private void initFilters() {
        mFilterHolder.removeAllViews();
        for (ShopArticleFilter filter : mChildFilters) {
            if (!filter.isVisible()) {
                continue;
            }
            ImageView image = new ImageView(mFilterHolder.getContext());
            image.setImageResource(filter.getIcon());
            applyFilterToImage(image, filter);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int index = mFilterHolder.indexOfChild(view);
                    List<ShopArticleFilter> filters = mChildFilters;
                    if (filters != null && index >= 0 && index < filters.size()) {
                        ShopArticleFilter filter = filters.get(index);
                        filter.setActive(!filter.isActive());
                        ImageView image = (ImageView) view;
                        applyFilterToImage(image, filter);
                        mListener.onFilterUpdate();
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

    @Override
    public boolean check(ShopArticle article) {
        for (ShopArticleFilter filter : mChildFilters) {
            if (filter.isActive() && filter.check(article)) {
                return true;
            }
        }
        return false;
    }
}
