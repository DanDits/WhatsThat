package dan.dit.whatsthat.testsubject.shopping;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.system.ImageDataDownload;
import dan.dit.whatsthat.testsubject.ForeignPurse;

/**
 * Created by daniel on 03.08.15.
 */
public class ShopArticleDownload extends ShopArticle implements ImageDataDownload.Feedback {
    private static final String KEY_PREFIX = "download_article_";
    private static final int SHOP_VALUE_DOWNLOADED_AND_SYNCED = 2;
    private ImageDataDownload mDownload;
    private int mCost;
    private DownloadProduct mDownloadProduct;
    private ConfirmProduct mConfirmProduct;
    private Context mContext;

    public ShopArticleDownload(Context context, ForeignPurse purse, int nameResId, int descrResId, int iconResId, int cost,
                               String origin, String dataName, int estimatedSizeMB, String url) {
        super(makeKey(origin, dataName), purse, nameResId, descrResId, iconResId);
        mCost = cost;
        mContext = context;
        mDownload = new ImageDataDownload(context, origin, dataName, estimatedSizeMB, url, this);
    }

    public static String makeKey(String origin, String dataName) {
        return KEY_PREFIX + origin + dataName;
    }

    @Override
    public boolean isClickable(int subProductIndex) {
        return isPurchasable(subProductIndex) == HINT_PURCHASABLE || (mPurse.getShopValue(mKey) < SHOP_VALUE_DOWNLOADED_AND_SYNCED);
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

    @Override
    public CharSequence getSpentScore(Resources resources) {
        if (mCost == 0 || !mPurse.hasShopValue(mKey)) {
            return "";
        }
        return resources.getString(R.string.shop_article_spent, mCost);
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
        return 1;
    }

    @Override
    public void onClose() {
        mDownloadProduct = null;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        if (mPurse.hasShopValue(mKey)) {
            if (mDownloadProduct == null) {
                mDownloadProduct = new DownloadProduct();
            }
            if (mDownloadProduct.hasNoView()) {
                mDownloadProduct.inflateView(inflater);
            }
            mDownloadProduct.updateDescription();
            return mDownloadProduct;
        } else {
            if (mConfirmProduct == null) {
                mConfirmProduct = new ConfirmProduct(this);
            }
            if (mConfirmProduct.hasNoView()) {
                mConfirmProduct.inflateView(inflater);
            }
            mConfirmProduct.setConfirmable(isPurchasable(subProductIndex), getCostText(mContext.getResources(), subProductIndex), makeMissingDependenciesText(mContext.getResources(), subProductIndex));
            return mConfirmProduct;
        }
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mConfirmProduct && isPurchasable(-1) == HINT_PURCHASABLE && mPurse.purchase(mKey, mCost) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        int shopValue = mPurse.getShopValue(mKey);
        switch (shopValue) {
            case 1:
                return 50;
            case SHOP_VALUE_DOWNLOADED_AND_SYNCED:
                return PROGRESS_COMPLETE;
            default:
                return 0;
        }
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        if (mDownloadProduct != null && mDownloadProduct.mProgress != null) {
            if (indeterminate && !mDownloadProduct.mProgress.isIndeterminate()) {
                mDownloadProduct.mProgress.setIndeterminate(indeterminate);
            } else if (!indeterminate && mDownloadProduct.mProgress.isIndeterminate()) {
                mDownloadProduct.mProgress.setIndeterminate(indeterminate);
            }
        }
    }

    @Override
    public void onError(int messageResId, int errorCode) {
        Toast.makeText(mContext, mContext.getResources().getString(messageResId, errorCode), Toast.LENGTH_SHORT).show();
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    @Override
    public void onDownloadComplete() {
        Toast.makeText(mContext, R.string.download_article_toast_download_complete, Toast.LENGTH_SHORT).show();
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    @Override
    public void onComplete() {
        mPurse.purchase(mKey, 0, 1);
        Toast.makeText(mContext, R.string.download_article_toast_complete, Toast.LENGTH_SHORT).show();
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
        if (mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (mDownloadProduct != null && mDownloadProduct.mProgress != null) {
            mDownloadProduct.mProgress.setProgress(progress);
        }
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    private class DownloadProduct extends SubProduct {
        private View mView;
        private TextView mDescription;
        private ProgressBar mProgress;
        @Override
        public View getView() {
            return mView;
        }

        public void updateDescription() {
            if (mDescription == null) {
                return;
            }
            if (mDownload.isWorking()) {
                if (mProgress != null) {
                    mProgress.setVisibility(View.VISIBLE);
                }
                if (mDownload.isDownloaded()) {
                    mDescription.setText(mContext.getString(R.string.download_article_descr_syncing, mDownload.getOrigin(), mDownload.getDataName()));
                } else {
                    mDescription.setText(mContext.getString(R.string.download_article_descr_downloading, mDownload.getOrigin(), mDownload.getDataName()));
                }
            } else {
                if (mProgress != null) {
                    mProgress.setVisibility(View.GONE);
                }
                if (mPurse.getShopValue(mKey) >= SHOP_VALUE_DOWNLOADED_AND_SYNCED) {
                    mDescription.setTextColor(Color.GREEN);
                    mDescription.setText(mContext.getString(R.string.download_article_descr_synced, mDownload.getOrigin(), mDownload.getDataName()));
                } else if (mDownload.isDownloaded()) {
                    mDescription.setText(mContext.getString(R.string.download_article_descr_downloaded, mDownload.getOrigin(), mDownload.getDataName()));
                } else {
                    int estimatedSizeMB = mDownload.getEstimatedSize();
                    mDescription.setText(mContext.getString(R.string.download_article_descr_ready, mDownload.getUrl(), estimatedSizeMB <= 0 ? 1 : estimatedSizeMB));
                }
            }
        }

        @Override
        public void inflateView(LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.download_product, null);
            mProgress = (ProgressBar) mView.findViewById(R.id.progress_bar);
            mProgress.setMax(PROGRESS_COMPLETE);
            mDescription = (TextView) mView.findViewById(R.id.download_descr);
        }

        @Override
        public boolean hasNoView() {
            return mView == null;
        }

        @Override
        public void onClick() {
            if (!mDownload.isWorking() && mPurse.getShopValue(mKey) < SHOP_VALUE_DOWNLOADED_AND_SYNCED) {
                mDownload.start();
            } else if (mDownload.isWorking()) {
                mDownload.cancel();
            }
        }
    }
}
