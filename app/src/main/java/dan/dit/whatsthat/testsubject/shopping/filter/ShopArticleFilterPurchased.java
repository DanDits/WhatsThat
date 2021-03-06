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

package dan.dit.whatsthat.testsubject.shopping.filter;

import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.util.general.PercentProgressListener;

/**
 * Created by daniel on 31.07.15.
 */
public class ShopArticleFilterPurchased extends ShopArticleFilter {
    private final boolean mPurchased;

    public ShopArticleFilterPurchased(int iconResId, boolean purchased, boolean active) {
        super(iconResId);
        mPurchased = purchased;
        setActive(active);
    }

    @Override
    public boolean check(ShopArticle article) {
        int purchasedPercent = article.getPurchaseProgressPercent();
        if (mPurchased) {
            return purchasedPercent >= PercentProgressListener.PROGRESS_COMPLETE;
        } else {
            return purchasedPercent < PercentProgressListener.PROGRESS_COMPLETE;
        }
    }
}
