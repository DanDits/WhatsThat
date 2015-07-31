package dan.dit.whatsthat.testsubject.shopping;

import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class SubProduct {
    protected ShopArticle mParentArticle;

    public abstract View getView();

    public void setParentArticle(ShopArticle parentArticle) {
        mParentArticle = parentArticle;
    }

    public abstract void inflateView(LayoutInflater inflater);
    public abstract boolean hasNoView();

    public abstract void onClick();
}
