package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.anjlab.android.iab.v3.TransactionDetails;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.system.store.BillingCallback;
import dan.dit.whatsthat.system.store.StoreActivity;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;

/**
 * Created by daniel on 09.01.16.
 */
public class PurchaseCurrencyArticle extends ShopArticle {
    private final SortimentHolder mHolder;
    private final int mGainedCurrency;
    private final String mGoogleProductId;
    private PurchaseCurrencyProduct mProduct;
    private int mCostInCent;

    /**
     * Creates a new article to purchase in app currency for real money using GooglePlay billing.
     * Requires inapp billing (and internet) and can fail on various reasons during the process.
     * This article can be purchased infinite times, the gained currency is displayed where
     * normally the spent currency is visualized.
     * @param articleKey The key of this article. Must be unique for all ShopArticles.
     * @param purse The purse used to add gained currency to.
     * @param holder The SortimentHolder used to get a valid BillingCallback of when click on
     *               purchase.
     * @param nameResId A string resource for the article name.
     * @param descrResId A string resource for the article description.
     * @param iconResId A drawable resource for the icon.
     * @param gainedCurrency The gained currency on successful purchase. Must be positive!
     * @param googleProductId The product id as specified in the google developer console. Must
     *                        be valid!
     * @param costInCent A visual hint as to how much real money this is going to cost in cents.
     *                   This is normalized for prices in germany including taxes, but the actual
     *                   price for billing will differ from country and currency used.
     */
    public PurchaseCurrencyArticle(String articleKey, ForeignPurse purse, SortimentHolder holder,
                                   int nameResId, int descrResId, int iconResId,
                                   int gainedCurrency,
                                   String googleProductId,
                                   int costInCent) {
        super(articleKey, purse, nameResId, descrResId, iconResId);
        mHolder = holder;
        mGainedCurrency = gainedCurrency;
        mGoogleProductId = googleProductId;
        mCostInCent = costInCent;
        if (mGainedCurrency <= 0) {
            throw new IllegalArgumentException("No currency gain to buy.");
        }
        if (TextUtils.isEmpty(mGoogleProductId)) {
            throw new IllegalArgumentException("No google product id given.");
        }
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (subProductIndex != 0) {
            return HINT_NOT_PURCHASABLE_OTHER;
        }
        if (areDependenciesMissing(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }

        return HINT_PURCHASABLE;
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return mPurse.getShopValue(mKey) * -mGainedCurrency;
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        return resources.getString(R.string.article_purchase_with_real_money);//will most likely
        // not be displayed
    }

    @Override
    public int getSubProductCount() {
        return 1;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        if (mProduct == null) {
            mProduct = new PurchaseCurrencyProduct();
        }
        if (mProduct.hasNoView()) {
            mProduct.inflateView(inflater);
        }
        if (!mProduct.hasNoView()) {
            TextView start = ((TextView) mProduct.getView().findViewById(R.id.shop_start_purchase));
            if (mCostInCent <= 0) {
                start.setText(R.string.article_purchase_start_plain);
            } else {
                start.setText(mProduct.getView().getResources().getString(
                        R.string.article_purchase_start, mCostInCent / 100.f));
            }
        }
        return mProduct;
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mProduct && mHolder != null && isPurchasable(0) == HINT_PURCHASABLE) {
            Log.d("Billing", "Clicked on " + mProduct + " child, holder not null, callback null="
                    + (mHolder.getBillingCallback() == null));
            BillingCallback callback = mHolder.getBillingCallback();
            if (callback != null) {
                callback.purchase(mGoogleProductId, mProduct);
            }
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return mPurse.getShopValue(mKey) > 0 ? 100 : 0;
    }

    private class PurchaseCurrencyProduct extends SubProduct implements StoreActivity.ProductPurchasedCallback {

        public PurchaseCurrencyProduct() {
            super(R.layout.shop_purchase_currency_product);
            setParentArticle(PurchaseCurrencyArticle.this);
        }

        @Override
        public int onProductPurchased(String productId, TransactionDetails details) {
            if (!TextUtils.isEmpty(productId) && productId.equals(mGoogleProductId)) {
                mPurse.purchaseCurrency(mKey, mGainedCurrency);
                if (mListener != null) {
                    mListener.onArticleChanged(PurchaseCurrencyArticle.this);
                }
                return StoreActivity.ProductPurchasedCallback.CONSUME_PRODUCT;
            }
            return StoreActivity.ProductPurchasedCallback.DO_NOTHING;
        }
    }
}
