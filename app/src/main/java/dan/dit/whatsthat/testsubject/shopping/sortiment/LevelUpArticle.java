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

import android.content.res.Resources;
import android.view.LayoutInflater;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ConfirmProduct;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 24.09.15.
 */
public class LevelUpArticle extends ShopArticle {
    public static final String KEY_LEVEL_UP_ARTICLE = "key_level_up_purchase";
    private ConfirmProduct mConfirmProduct;

    protected LevelUpArticle(ForeignPurse purse) {
        super(KEY_LEVEL_UP_ARTICLE, purse, R.string.article_level_up_name, R.string.article_level_up_descr, R.drawable.icon_general);
        addDependency(new Dependency() {
            @Override
            public boolean isNotFulfilled() {
                return TestSubject.getInstance().canChooseNewRiddle();
            }

            @Override
            public CharSequence getName(Resources res) {
                return res.getString(R.string.article_level_up_dep_new_riddle);
            }
        }, GENERAL_PRODUCT_INDEX);
    }

    private boolean hasReachedMaximumLevel() {
        return TestSubject.getInstance().getCurrentLevel() >= TestSubject.getInstance().getMaximumLevel();
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (hasReachedMaximumLevel()) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        if (areDependenciesMissing(TestSubject.getInstance().getCurrentLevel() + 1)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }
        int cost = TestSubject.getInstance().getNextLevelUpCost();
        return mPurse.getCurrentScore() >= cost ? HINT_PURCHASABLE : HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }

    @Override
    public boolean isImportant() {
        int purchasable = isPurchasable(GENERAL_PRODUCT_INDEX);
        return purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE
                || (purchasable == HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING && !areDependenciesMissing(GENERAL_PRODUCT_INDEX));
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return mPurse.getShopValue(TestSubject.SHW_KEY_SPENT_SCORE_ON_LEVEL_UP);
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        int cost = TestSubject.getInstance().getNextLevelUpCost();
        if (cost > 0) {
            return String.valueOf(cost);
        } else {
            return resources.getString(R.string.shop_article_free);
        }
    }

    @Override
    public int getSubProductCount() {
        return hasReachedMaximumLevel() ? 0 : 1;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        if (hasReachedMaximumLevel()) {
            return null;
        }
        if (mConfirmProduct == null || mConfirmProduct.hasNoView()) {
            mConfirmProduct = new ConfirmProduct(this);
            mConfirmProduct.inflateView(inflater);
        }
        CharSequence costText;
        CharSequence depText;
        int cost = TestSubject.getInstance().getNextLevelUpCost();
        if (mConfirmProduct.hasNoView()) {
            costText = String.valueOf(cost);
            depText = "---";
        } else {
            Resources res = mConfirmProduct.getView().getResources();
            costText = getCostText(res, subProductIndex);
            depText = makeMissingDependenciesText(res, TestSubject.getInstance().getCurrentLevel() + 1);
        }
        int purchasable = isPurchasable(subProductIndex);
        mConfirmProduct.setConfirmable(purchasable, costText, depText, cost > 0 && (purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) ? R.drawable.think_currency_small : 0);
        return mConfirmProduct;
    }

    @Override
    public void onChildClick(SubProduct product) {
        if (product == mConfirmProduct && isPurchasable(GENERAL_PRODUCT_INDEX) == HINT_PURCHASABLE
                && TestSubject.getInstance().purchaseLevelUp() && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return (int) ((TestSubject.getInstance().getCurrentLevel() + 1)/ ((double) TestSubject.getInstance().getMaximumLevel() + 1.) * 100);
    }
}
