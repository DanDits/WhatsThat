package dan.dit.whatsthat.testsubject.shopping;

/**
 * Created by daniel on 31.07.15.
 */
public abstract class ShopArticleFilter {
    private final int mIcon;
    private boolean mActive;

    ShopArticleFilter(int iconResId) {
        mIcon = iconResId;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public abstract boolean check(ShopArticle article);

    public int getIcon() {
        return mIcon;
    }

    public boolean isActive() {
        return mActive;
    }
}
