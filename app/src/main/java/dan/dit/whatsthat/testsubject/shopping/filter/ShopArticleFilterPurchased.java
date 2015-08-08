package dan.dit.whatsthat.testsubject.shopping.filter;

import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 31.07.15.
 */
public class ShopArticleFilterPurchased extends ShopArticleFilter {
    private final boolean mPurchased;

    public ShopArticleFilterPurchased(int iconResId, boolean purchased, boolean active) {
        super(iconResId);
        mPurchased = purchased;
        setActive(active);
    }

    @Override
    public boolean check(ShopArticle article) {
        int purchasedPercent = article.getPurchaseProgressPercent();
        if (mPurchased) {
            return purchasedPercent >= PercentProgressListener.PROGRESS_COMPLETE;
        } else {
            return purchasedPercent < PercentProgressListener.PROGRESS_COMPLETE;
        }
    }
}
