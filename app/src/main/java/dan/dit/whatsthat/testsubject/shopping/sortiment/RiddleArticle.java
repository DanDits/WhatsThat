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
import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 24.09.15.
 */
public class RiddleArticle extends ShopArticle {
    public static final String KEY_CHOOSE_RIDDLE_ARTICLE = "key_choose_riddle_article";
    private SparseArray<RiddleProduct> mProducts;
    protected RiddleArticle(ForeignPurse purse) {
        super(KEY_CHOOSE_RIDDLE_ARTICLE, purse, R.string.article_choose_riddle_name, R.string.article_choose_riddle_descr, R.drawable.icon_general);
        mProducts = new SparseArray<>(PracticalRiddleType.ALL_PLAYABLE_TYPES.size());
        addDependency(new Dependency() {
            @Override
            public boolean isNotFulfilled() {
                return !TestSubject.getInstance().canChooseNewRiddle();
            }

            @Override
            public CharSequence getName(Resources res) {
                return res.getString(R.string.article_level_up_dep_new_level);
            }
        }, GENERAL_PRODUCT_INDEX);
    }

    @Override
    public boolean isImportant() {
        int purchasable = isPurchasable(GENERAL_PRODUCT_INDEX);
        return purchasable == HINT_NOT_PURCHASABLE_OTHER;
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (hasAllRiddles()) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        if (areDependenciesMissing(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }
        if (subProductIndex == GENERAL_PRODUCT_INDEX) {
            return HINT_NOT_PURCHASABLE_OTHER;
        }
        PracticalRiddleType type = getTypeForProductIndex(subProductIndex);
        if (TestSubject.getInstance().isTypeAvailable(type)) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        return HINT_PURCHASABLE;

    }

    private PracticalRiddleType getTypeForProductIndex(int index) {
        if (index >= 0 && index < PracticalRiddleType.ALL_PLAYABLE_TYPES.size()) {
            return PracticalRiddleType.ALL_PLAYABLE_TYPES.get(index);
        }
        return null;
    }

    private boolean hasAllRiddles() {
        return TestSubject.getInstance().getAvailableTypes().size() >= PracticalRiddleType.ALL_PLAYABLE_TYPES.size();
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return 0;
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        return "";
    }

    @Override
    public int getSubProductCount() {
        return PracticalRiddleType.ALL_PLAYABLE_TYPES.size();
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        RiddleProduct product = mProducts.get(subProductIndex);
        if (product == null) {
            product = new RiddleProduct(PracticalRiddleType.ALL_PLAYABLE_TYPES.get(subProductIndex));
            mProducts.put(subProductIndex, product);
            product.inflateView(inflater);
        } else {
            product.updateState();
        }
        return product;
    }

    private int getChildIndex(SubProduct product) {
        int index = -1;
        for (int i = 0; i < mProducts.size(); i++) {
            if (mProducts.valueAt(i).equals(product)) {
                index = mProducts.keyAt(i);
                break;
            }
        }
        return index;
    }

    @Override
    public void onChildClick(SubProduct product) {
        int index = getChildIndex(product);
        Log.d("Riddle", "On child click of riddle article  index: " + index + " of types " + mProducts);
        if (index < 0) {
            return;
        }
        Log.d("Riddle", "Purchasable: " + isPurchasable(index) + " listener: " + mListener);
        if (isPurchasable(index) == HINT_PURCHASABLE
                && TestSubject.getInstance().chooseNewRiddle(((RiddleProduct) product).mType)
                && mListener != null) {
            Log.d("Riddle", "Chose new riddle " + ((RiddleProduct) product).mType);
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return (int) (100 * TestSubject.getInstance().getAvailableTypes().size() / (double) PracticalRiddleType.ALL_PLAYABLE_TYPES.size());
    }


    private class RiddleProduct extends SubProduct {
        PracticalRiddleType mType;
        View mView;

        public RiddleProduct(PracticalRiddleType type) {
            mType = type;
            mParentArticle = RiddleArticle.this;
        }

        @Override
        public View getView() {
            return mView;
        }

        public void updateState() {
            if (TestSubject.getInstance().isTypeAvailable(mType)) {
                mView.findViewById(R.id.riddle_confirm).setVisibility(View.GONE);
                mView.setBackgroundColor(mView.getResources().getColor(R.color.important));
            } else {

                TextView confirm = (TextView) mView.findViewById(R.id.riddle_confirm);
                mView.setBackgroundResource(R.drawable.button_important_background);
                confirm.setVisibility(View.VISIBLE);
                int index = getChildIndex(this);
                int purchasable = isPurchasable(index);
                if (purchasable == HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING) {
                    confirm.setText(makeMissingDependenciesText(confirm.getResources(), index));
                    confirm.setTextColor(Color.RED);
                    confirm.setEnabled(false);
                } else {
                    confirm.setText(R.string.article_choose_riddle_confirm);
                    confirm.setTextColor(0xFFcf9700); // orangeish
                    confirm.setEnabled(true);
                }

            }
        }

        @Override
        public void inflateView(LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.choose_riddle_product, null);
            ((ImageView) mView.findViewById(R.id.riddle_icon)).setImageResource(mType.getIconResId());
            ((TextView) mView.findViewById(R.id.riddle_name)).setText(mType.getNameResId());
            ((TextView) mView.findViewById(R.id.riddle_advertising)).setText(mType.getAdvertisingResId());
            updateState();
        }
    }
}
