package dan.dit.whatsthat.testsubject.shopping.filter;

import dan.dit.whatsthat.testsubject.shopping.ShopArticle;

/**
 * Created by daniel on 31.07.15.
 */
public abstract class ShopArticleFilter {
    private final int mIcon;
    private boolean mActive;
    protected boolean mVisible = true;

    ShopArticleFilter(int iconResId) {
        mIcon = iconResId;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Check if the article should be shown in the list.
     * @param article The article to check.
     * @return True if the article is to be displayed. False if this filter
     * does not require the article be be shown. It might be shown by some other filter though.
     */
    public abstract boolean check(ShopArticle article);

    public int getIcon() {
        return mIcon;
    }

    public boolean isActive() {
        return mActive;
    }

    public boolean isVisible() {
        return mVisible;
    }
}
