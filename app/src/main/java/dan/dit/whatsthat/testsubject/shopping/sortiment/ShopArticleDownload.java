/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.BundleManager;
import dan.dit.whatsthat.system.ImageDataDownload;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.shopping.ConfirmProduct;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;

/**
 * Created by daniel on 03.08.15.
 */
public class ShopArticleDownload extends ShopArticle implements ImageDataDownload.Feedback {
    public static final String KEY_PREFIX = "a_download_article_";
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

    private static String makeKey(String origin, String dataName) {
        return KEY_PREFIX + origin + dataName;
    }

    @Override
    public CharSequence getName(Resources res) {
        return res.getString(mNameResId, mDownload.getDataName());
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
        if (areDependenciesMissing(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }
        return mPurse.getCurrentScore() >= mCost ? HINT_PURCHASABLE : HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return !mPurse.hasShopValue(mKey) ? 0 : mCost;
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
            if (mDownloadProduct.getView() == null) {
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
            int purchasable = isPurchasable(subProductIndex);
            mConfirmProduct.setConfirmable(purchasable, getCostText(mContext.getResources(), subProductIndex), makeMissingDependenciesText(mContext.getResources(), subProductIndex),
                    mCost > 0 && (purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) ? R.drawable.think_currency_small : 0);
            return mConfirmProduct;
        }
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mConfirmProduct && isPurchasable(-1) == HINT_PURCHASABLE && mPurse.purchaseFeature(mKey, mCost)) {
            if (mListener != null) {
                mListener.onArticleChanged(this);
            }
            BundleManager.onBundleCreated(mContext, mDownload.getOrigin(), mDownload.getDataName(), mDownload.getEstimatedImages(), mDownload.getEstimatedSize(), false, getKey());
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
    public void setIsWorking(boolean isWorking) {
        if (mDownloadProduct != null && mDownloadProduct.mProgressIsWorking != null) {
            mDownloadProduct.mProgressIsWorking.setVisibility(isWorking ? View.VISIBLE : View.INVISIBLE);
        }
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    @Override
    public void onError(int messageResId, int errorCode) {
        if (errorCode == ImageDataDownload.ERROR_CODE_DOWNLOAD_IOEXCEPTION) {
            Toast.makeText(mContext, R.string.download_article_toast_error_no_internet, Toast
                    .LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(messageResId, errorCode), Toast.LENGTH_SHORT).show();
        }
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    @Override
    public void onDownloadComplete() {
        BundleManager.onBundleCreated(mContext, mDownload.getOrigin(), mDownload.getDataName(), mDownload.getEstimatedImages(), mDownload.getEstimatedSize(), false, null);
        Toast.makeText(mContext, R.string.download_article_toast_download_complete, Toast.LENGTH_SHORT).show();
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    @Override
    public void onComplete() {
        mPurse.purchase(mKey, 0, 1);
        Toast.makeText(mContext, R.string.download_article_toast_complete, Toast.LENGTH_SHORT).show();
        BundleManager.onBundleCreated(mContext, mDownload.getOrigin(), mDownload.getDataName(), mDownload.getEstimatedImages(), mDownload.getEstimatedSize(), true, null);

        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
        if (mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (mDownloadProduct != null && mDownloadProduct.mActualProgress != null) {
            mDownloadProduct.mActualProgress.setProgress(progress);
        }
        if (mDownloadProduct != null) {
            mDownloadProduct.updateDescription();
        }
    }

    private class DownloadProduct extends SubProduct {
        private View mView;
        private TextView mDescription;
        private View mProgressIsWorking;
        private ProgressBar mActualProgress;
        private View mProgress;

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
                    mDescription.setTextColor(mView.getResources().getColor(R.color.important_on_main_background));
                    mDescription.setText(mContext.getString(R.string.download_article_descr_synced, mDownload.getOrigin(), mDownload.getDataName()));
                } else if (mDownload.isDownloaded()) {
                    mDescription.setText(mContext.getString(R.string.download_article_descr_downloaded, mDownload.getOrigin(), mDownload.getDataName()));
                } else {
                    int estimatedSizeMB = mDownload.getEstimatedSize();
                    mDescription.setText(mContext.getString(R.string.download_article_descr_ready, mDownload.getURLHost(), estimatedSizeMB <= 0 ? 1 : estimatedSizeMB));
                }
            }
        }

        @Override
        public void inflateView(LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.download_product, null);
            mProgress = mView.findViewById(R.id.progress);
            mProgressIsWorking = mView.findViewById(R.id.progress_is_working);
            mActualProgress = (ProgressBar) mView.findViewById(R.id.progress_bar);
            mActualProgress.setMax(PROGRESS_COMPLETE);
            mActualProgress.setProgress(0);
            mDescription = (TextView) mView.findViewById(R.id.download_descr);
        }

        @Override
        public void onClick() {
            start();
        }
    }

    public void start() {
        if (!mDownload.isWorking() && mPurse.getShopValue(mKey) < SHOP_VALUE_DOWNLOADED_AND_SYNCED) {
            mDownload.start();
        } else if (mDownload.isWorking()) {
            mDownload.cancel();
        }
    }
}
