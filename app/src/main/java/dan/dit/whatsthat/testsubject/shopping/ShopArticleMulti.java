package dan.dit.whatsthat.testsubject.shopping;

import android.content.res.Resources;
import android.support.annotation.ArrayRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 03.10.15.
 */
public class ShopArticleMulti extends ShopArticle {
    private final int mProductDescrResId;
    private int[] mCosts;
    private MultiProduct[] mProducts;

    /**
     * Creates a new shop article with multiple sub products. There can be at most 31 subproducts, the amount is being defined
     * by the size of the given costs array. The products are supposed to be improvements of each other and are required that the previous one
     * is already purchased as a dependency.
     * @param key The article's key identifying it..
     * @param purse The purse to load and save purchase state from/to.
     * @param nameResId The name resource id of this article's name.
     * @param descrResId The description resource id of this article.
     * @param iconResId The icon for the article.
     * @param productDescrResId A string array resource for each product. Should be of size equal to given costs.
     * @param costs The costs for each sub product.
     */
    public ShopArticleMulti(@NonNull String key, @NonNull ForeignPurse purse, @StringRes int nameResId, @StringRes int descrResId, @DrawableRes int iconResId,
                            @ArrayRes int productDescrResId, int[] costs) {
        super(key, purse, nameResId, descrResId, iconResId);
        mCosts = costs;
        mProductDescrResId = productDescrResId;
        if (mCosts == null || mCosts.length == 0) {
            throw new IllegalArgumentException("No costs given.");
        }
        mProducts = new MultiProduct[mCosts.length];
    }

    @Override
    public void makeDependencies() {
        super.makeDependencies();
        for (int i = 1; i < mProducts.length; i++) {
            addDependency(TestSubject.getInstance().makeProductPurchasedDependency(mKey, i - 1), i);
        }
    }

    private int getPurchasedCount() {
        int shopValue = mPurse.getShopValue(mKey);
        return getSetBitsCount(shopValue);
    }

    public static boolean hasPurchased(int value, int index) {
        return ((value >>> index) & 1) == 1;
    }
    private boolean isPurchased(int subProductIndex) {
        int shopValue = mPurse.getShopValue(mKey);
        return hasPurchased(shopValue, subProductIndex);
    }

    private int getSetBitsCount(int value) {
        // http://stackoverflow.com/questions/109023/how-to-count-the-number-of-set-bits-in-a-32-bit-integer
        value = value - ((value >>> 1) & 0x55555555);
        value = (value & 0x33333333) + ((value >>> 2) & 0x33333333);
        return (((value + (value >>> 4)) & 0x0F0F0F0F) * 0x01010101) >>> 24;
    }

    private int getMaxPurchaseCount() {
        return mCosts.length;
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (isPurchased(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        if (areDependenciesMissing(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }
        return mPurse.getCurrentScore() >= mCosts[subProductIndex] ? HINT_PURCHASABLE : HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        int spent = 0;
        for (int i = 0; i < mCosts.length; i++) {
            if (isPurchased(i) && (subProductIndex == GENERAL_PRODUCT_INDEX || subProductIndex == i)) {
                spent += mCosts[i];
            }
        }
        return spent;
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        int cost = mCosts[subProductIndex];
        if (cost > 0) {
            return String.valueOf(cost);
        } else {
            return resources.getString(R.string.shop_article_free);
        }
    }

    @Override
    public int getSubProductCount() {
        return getMaxPurchaseCount();
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        // ensure multi product exists
        MultiProduct product = mProducts[subProductIndex];
        if (product == null) {
            product = new MultiProduct(inflater, subProductIndex);
            mProducts[subProductIndex] = product;
        }
        if (!isPurchased(subProductIndex)) {
            // not yet purchased, add confirm product view
            ConfirmProduct confirm = product.mConfirm;
            if (confirm == null) {
                confirm = new ConfirmProduct(this);
                confirm.inflateView(inflater);
            }
            CharSequence costText;
            CharSequence depText;
            int cost = mCosts[subProductIndex];
            if (confirm.hasNoView()) {
                costText = String.valueOf(cost);
                depText = "---";
            } else {
                Resources res = confirm.getView().getResources();
                costText = getCostText(res, subProductIndex);
                depText = makeMissingDependenciesText(res, subProductIndex);
            }
            int purchasable = isPurchasable(subProductIndex);
            confirm.setConfirmable(purchasable, costText, depText, cost > 0 && (purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) ? R.drawable.think_currency_small : 0);
            product.addConfirmView(confirm);
        } else {
            product.removeConfirmView();
        }
        return product;
    }

    @Override
    public void onChildClick(SubProduct product) {
        int index = -1;
        for (int i = 0; i < mProducts.length; i++) {
            if (mProducts[i] == product) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        int newShopValue = mPurse.getShopValue(mKey);
        newShopValue |= (1 << index);
        if (isPurchasable(index) == HINT_PURCHASABLE && mPurse.purchaseHigherValue(mKey, mCosts[index], newShopValue) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return (int) (100 * getPurchasedCount() / (double) getMaxPurchaseCount());
    }

    private class MultiProduct extends SubProduct {
        private String mDescr;
        private ViewGroup mView;
        private ConfirmProduct mConfirm;
        public MultiProduct(LayoutInflater inflater, int subProductIndex) {
            setParentArticle(ShopArticleMulti.this);
            String[] descrs = inflater.getContext().getResources().getStringArray(mProductDescrResId);
            if (descrs.length > 0 && descrs.length > subProductIndex) {
                mDescr = descrs[subProductIndex];
            } else {
                mDescr = "";
            }
            inflateView(inflater);
        }

        @Override
        public View getView() {
            return mView;
        }

        @Override
        public void inflateView(LayoutInflater inflater) {
            mView = (ViewGroup) inflater.inflate(R.layout.shop_article_multi_product, null);
            ((TextView) mView.findViewById(R.id.product_descr)).setText(mDescr);
        }

        public void addConfirmView(ConfirmProduct confirm) {
            mConfirm = confirm;
            if (mView.getChildCount() == 1) {
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                mView.addView(confirm.getView());
            }
        }

        public void removeConfirmView() {
            mConfirm = null;
            if (mView.getChildCount() > 1) {
                mView.removeViewAt(mView.getChildCount() - 1);
            }
        }
    }
}
