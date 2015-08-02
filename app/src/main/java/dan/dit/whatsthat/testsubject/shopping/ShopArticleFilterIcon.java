package dan.dit.whatsthat.testsubject.shopping;

/**
 * Created by daniel on 31.07.15.
 */
public class ShopArticleFilterIcon extends ShopArticleFilter {
    private final int mFilterIconId;

    public ShopArticleFilterIcon(int iconResId, int filterIconId, boolean active) {
        super(iconResId);
        mFilterIconId = filterIconId;
        setActive(active);
    }

    @Override
    public boolean check(ShopArticle article) {
        return article.getIconResId() == mFilterIconId;
    }
}
