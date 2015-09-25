package dan.dit.whatsthat.testsubject.shopping;

import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class SubProduct {
    protected ShopArticle mParentArticle;

    public abstract View getView();

    void setParentArticle(ShopArticle parentArticle) {
        mParentArticle = parentArticle;
    }

    public abstract void inflateView(LayoutInflater inflater);
    public boolean hasNoView() {
        return getView() == null;
    }

    public abstract void onClick();
}
