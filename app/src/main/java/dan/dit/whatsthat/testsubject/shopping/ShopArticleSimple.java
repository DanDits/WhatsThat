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

package dan.dit.whatsthat.testsubject.shopping;

import android.content.res.Resources;
import android.view.LayoutInflater;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.util.general.PercentProgressListener;

/**
 * Created by daniel on 29.07.15.
 */
public class ShopArticleSimple extends ShopArticle {
    protected int mCost;
    protected ConfirmProduct mConfirmProduct;


    public ShopArticleSimple(String key, ForeignPurse purse, int nameResId, int descrResId, int iconResId, int cost) {
        super(key, purse, nameResId, descrResId, iconResId);
        mCost = cost;
        if (!mPurse.hasShopValue(mKey)) {
            mConfirmProduct = new ConfirmProduct(this);
        }
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
        return mPurse.hasShopValue(mKey) ? 0 : 1;
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
        int purchasable = isPurchasable(index);
        mConfirmProduct.setConfirmable(purchasable, costText, depText, mCost > 0 && (purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) ? R.drawable.think_currency_small : 0);
        return mConfirmProduct;
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mConfirmProduct && isPurchasable(GENERAL_PRODUCT_INDEX) == HINT_PURCHASABLE && mPurse.purchaseFeature(mKey, mCost) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return mPurse.hasShopValue(mKey) ? PercentProgressListener.PROGRESS_COMPLETE : 0;
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
}
