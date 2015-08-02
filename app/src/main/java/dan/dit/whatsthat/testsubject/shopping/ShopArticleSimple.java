package dan.dit.whatsthat.testsubject.shopping;

import android.content.res.Resources;
import android.view.LayoutInflater;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 29.07.15.
 */
public class ShopArticleSimple extends ShopArticle {
    private int mCost;
    private ConfirmProduct mConfirmProduct;


    public ShopArticleSimple(String key, ForeignPurse purse, int nameResId, int descrResId, int iconResId, int cost) {
        super(key, purse, nameResId, descrResId, iconResId);
        mCost = cost;
        if (getSubProductCount() > 0) {
            mConfirmProduct = new ConfirmProduct(this);
        }
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return mCost;
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        if (mCost > 0) {
            return String.valueOf(mCost);
        } else {
            return resources.getString(R.string.shop_article_free);
        }
    }

    @Override
    public int getSubProductCount() {
        int count = mPurse.hasShopValue(mKey) ? 0 : 1;
        return count;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int index) {
        if (mConfirmProduct == null) {
            return null;
        }
        if (mConfirmProduct.hasNoView()) {
            mConfirmProduct.inflateView(inflater);
        }
        CharSequence costText;
        CharSequence depText;
        if (mConfirmProduct.hasNoView()) {
            costText = String.valueOf(mCost);
            depText = "---";
        } else {
            Resources res = mConfirmProduct.getView().getResources();
            costText = getCostText(res, index);
            depText = makeMissingDependenciesText(res, index);
        }
        mConfirmProduct.setConfirmable(isPurchasable(index), costText, depText);
        return mConfirmProduct;
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mConfirmProduct && isPurchasable(-1) == HINT_PURCHASABLE && mPurse.purchase(mKey, mCost) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return mPurse.hasShopValue(mKey) ? PercentProgressListener.PROGRESS_COMPLETE : 0;
    }

    @Override
    public CharSequence getSpentScore(Resources resources) {
        if (mCost == 0 || !mPurse.hasShopValue(mKey)) {
            return "";
        }
        return resources.getString(R.string.shop_article_spent, mCost);
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (mPurse.hasShopValue(mKey)) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        if (!areDependenciesFulfilled(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }
        return mPurse.getCurrentScore() >= mCost ? HINT_PURCHASABLE : HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }
}