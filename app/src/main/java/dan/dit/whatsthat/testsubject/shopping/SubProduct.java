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

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class SubProduct {
    protected View mView;
    protected ShopArticle mParentArticle;
    protected int mLayoutResId;
    public SubProduct(int layoutResId) {
        mLayoutResId = layoutResId;
    }

    public View getView() {
        return mView;
    }

    protected void setParentArticle(ShopArticle parentArticle) {
        mParentArticle = parentArticle;
    }

    public void inflateView(LayoutInflater inflater) {
        mView = inflater.inflate(mLayoutResId, null);
    }

    public boolean hasNoView() {
        return getView() == null;
    }

    public void onClick() {
        if (mParentArticle != null) {
            mParentArticle.onChildClick(this);
        }
    }
}
