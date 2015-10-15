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

/**
 * Created by daniel on 31.07.15.
 */
public abstract class ShopArticleFilter {
    private final int mIcon;
    private boolean mActive;
    protected boolean mVisible = true;

    ShopArticleFilter(int iconResId) {
        mIcon = iconResId;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Check if the article should be shown in the list.
     * @param article The article to check.
     * @return True if the article is to be displayed. False if this filter
     * does not require the article be be shown. It might be shown by some other filter though.
     */
    public abstract boolean check(ShopArticle article);

    public int getIcon() {
        return mIcon;
    }

    public boolean isActive() {
        return mActive;
    }

    public boolean isVisible() {
        return mVisible;
    }
}
