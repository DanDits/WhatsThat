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

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 29.07.15.
 */
public class ConfirmProduct extends SubProduct {


    public ConfirmProduct(ShopArticle parent) {
        super(R.layout.shop_confirm_product);
        setParentArticle(parent);
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
