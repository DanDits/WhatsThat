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

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;

/**
 * Created by daniel on 30.09.15.
 */
public class ShopArticleToggleable extends ShopArticleSimple {
    private ToggleableProduct mToggler;
    private int mToggleTextOnResId;
    private int mToggleTextOffResId;

    public ShopArticleToggleable(String key, ForeignPurse purse, int nameResId, int descrResId, int iconResId,
                                 int toggleTextOn, int toggleTextOff, int cost) {
        super(key, purse, nameResId, descrResId, iconResId, cost);
        mToggleTextOnResId = toggleTextOn;
        mToggleTextOffResId = toggleTextOff;
    }


    @Override
    public int getSubProductCount() {
        return 1;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int index) {
        if (!mPurse.hasShopValue(mKey)) {
            return super.getSubProduct(inflater, index);
        }
        if (mToggler == null) {
            mToggler = new ToggleableProduct();
            mToggler.setParentArticle(this);
        }
        if (mToggler.hasNoView()) {
            mToggler.inflateView(inflater);
        } else {
            mToggler.updateState();
        }
        return mToggler;
    }

    @Override
    public boolean isClickable(int subProductIndex) {
        return mToggler != null || super.isClickable(subProductIndex);
    }

    @Override
    public void onChildClick(SubProduct product) {
        super.onChildClick(product);
        if (product == mToggler && mPurse.toggleFeature(mKey) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    private class ToggleableProduct extends SubProduct {
        private View mView;

        @Override
        public View getView() {
            return mView;
        }

        @Override
        public void inflateView(LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.shop_toggler_product, null);
            updateState();
        }

        private void updateState() {
            Switch toggler = ((Switch) mView.findViewById(R.id.toggler));
            toggler.setChecked(mPurse.hasToggleableFeature(mKey));
            toggler.setTextOn(toggler.getResources().getString(mToggleTextOnResId));
            toggler.setTextOff(toggler.getResources().getString(mToggleTextOffResId));
        }
    }
}
