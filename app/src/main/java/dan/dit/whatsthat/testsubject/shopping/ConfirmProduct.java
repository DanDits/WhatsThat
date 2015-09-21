package dan.dit.whatsthat.testsubject.shopping;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 29.07.15.
 */
public class ConfirmProduct extends SubProduct {


    private View mView;

    public ConfirmProduct(ShopArticle parent) {
        setParentArticle(parent);
    }

    public void inflateView(LayoutInflater inflater) {
        mView = inflater.inflate(R.layout.shop_confirm_product, null);
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public boolean hasNoView() {
        return mView == null;
    }

    @Override
    public void onClick() {
        if (mParentArticle != null) {
            mParentArticle.onChildClick(this);
        }
    }

    public void setConfirmable(int purchasableHint, CharSequence costText, CharSequence depText, int icon) {
        if (mView != null) {
            TextView view = ((TextView) mView.findViewById(R.id.shop_confirm_title));
            view.setTextColor(purchasableHint == ShopArticle.HINT_PURCHASABLE ? view.getResources().getColor(R.color.important_on_main_background) : Color.RED);
            if (purchasableHint == ShopArticle.HINT_PURCHASABLE) {
                view.setText(view.getResources().getString(R.string.article_product_confirm_purchase, costText));
            } else if (purchasableHint == ShopArticle.HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING) {
                view.setText(view.getResources().getString(R.string.article_product_confirm_purchase_dependency_missing, depText));
            } else if (purchasableHint == ShopArticle.HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) {
                view.setText(view.getResources().getString(R.string.article_product_confirm_purchase_too_expensive, costText));
            } else {
                view.setText(R.string.article_product_confirm_purchase_unavailable);
            }
            view.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0);
        }
    }
}
