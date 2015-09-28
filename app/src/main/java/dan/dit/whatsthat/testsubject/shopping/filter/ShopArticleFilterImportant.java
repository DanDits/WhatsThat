package dan.dit.whatsthat.testsubject.shopping.filter;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;

/**
 * Created by daniel on 24.09.15.
 */
public class ShopArticleFilterImportant extends ShopArticleFilter {

    public ShopArticleFilterImportant(int iconResId) {
        super(iconResId);
        setActive(true);
        mVisible = true;
    }

    @Override
    public boolean check(ShopArticle article) {
        return article.isImportant();
    }
}
