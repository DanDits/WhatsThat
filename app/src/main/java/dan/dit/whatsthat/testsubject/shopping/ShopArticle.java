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
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class ShopArticle {
    public static final int GENERAL_PRODUCT_INDEX = -1;
    public static final int HINT_PURCHASABLE = 1;
    public static final int HINT_NOT_PURCHASABLE_TOO_EXPENSIVE = 0;
    public static final int HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING = -1;
    public static final int HINT_NOT_PURCHASABLE_ALREADY_PURCHASED = -2;
    public static final int HINT_NOT_PURCHASABLE_OTHER = -3;

    private static final int DEFAULT_ICON = R.drawable.icon_plain;
    protected final String mKey;
    protected final int mDescrResId;
    protected final int mNameResId;
    protected final ForeignPurse mPurse;
    protected final int mIconResId;
    protected ShopArticleHolder.OnArticleChangedListener mListener;
    private SparseArray<List<Dependency>> mDependencies;

    protected ShopArticle(String key, ForeignPurse purse, int nameResId, int descrResId, int iconResId) {
        mPurse = purse;
        mKey = key;
        mNameResId = nameResId;
        mDescrResId = descrResId;
        mIconResId = iconResId;
        mDependencies = new SparseArray<>();
        if (mPurse== null) {
            throw new IllegalArgumentException("No purse given.");
        }
        if (TextUtils.isEmpty(mKey)) {
            throw new IllegalArgumentException("Illegal key given.");
        }
    }

    public abstract int isPurchasable(int subProductIndex);

    public CharSequence getSpentScore(Resources resources) {
        int spentScore = getSpentScore(GENERAL_PRODUCT_INDEX);
        return handleSpentScoreText(resources, spentScore);
    }

    protected static CharSequence handleSpentScoreText(Resources resources, int spent) {
        if (spent == 0) {
            return "";
        }
        if (spent > 0) {
            return resources.getString(R.string.shop_article_spent, spent);
        } else {
            return resources.getString(R.string.shop_article_gained, -spent);
        }
    }

    public abstract int getSpentScore(int subProductIndex);
    public abstract CharSequence getCostText(Resources resources, int subProductIndex);

    public abstract int getSubProductCount();

    public abstract SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex);

    public CharSequence getName(Resources res) {
        return res.getString(mNameResId);
    }

    public CharSequence getDescription(Resources res) {
        return res.getString(mDescrResId);
    }

    public int getIconResId() {
        return mIconResId == 0 ? DEFAULT_ICON : mIconResId;
    }

    public abstract void onChildClick(SubProduct product);

    public void setOnArticleChangedListener(ShopArticleHolder.OnArticleChangedListener listener) {
        mListener = listener;
    }

    public abstract int getPurchaseProgressPercent();

    public void onClose() {

    }

    @Override
    public String toString() {
        return mKey;
    }

    public boolean isClickable(int subProductIndex) {
        return isPurchasable(subProductIndex) == HINT_PURCHASABLE;
    }

    public final ShopArticle addDependency(Dependency dep, int subProductIndex) {
        if (dep == null) {
            throw new IllegalArgumentException("Null dependency cannot be added.");
        }
        List<Dependency> deps = mDependencies.get(subProductIndex);
        if (deps == null) {
            deps = new ArrayList<>(1);
            mDependencies.put(subProductIndex < 0 ? GENERAL_PRODUCT_INDEX : subProductIndex, deps);
        }
        deps.add(dep);
        return this;
    }

    private boolean isDependencyNotFulfilled(List<Dependency> deps) {
        if (deps == null) {
            return false;
        }
        for (Dependency dep : deps) {
            if (!dep.isFulfilled()) {
                return true;
            }
        }
        return false;
    }

    protected boolean areDependenciesMissing(int subProductIndex) {
        if (subProductIndex < 0) {
            return isDependencyNotFulfilled(mDependencies.get(GENERAL_PRODUCT_INDEX));
        } else {
            if (isDependencyNotFulfilled(mDependencies.get(GENERAL_PRODUCT_INDEX)) || isDependencyNotFulfilled(mDependencies.get(subProductIndex))) {
                return true;
            }
        }
        return false;
    }

    private boolean appendMissingDependencies(StringBuilder builder, Resources res, List<Dependency> deps, boolean separator) {
        if (deps == null) {
            return separator;
        }
        boolean addSeparator = separator;
        for (Dependency dep : deps) {
            if (!dep.isFulfilled()) {
                if (addSeparator) {
                    builder.append(", ");
                }
                builder.append(dep.getName(res));
                addSeparator = true;
            }
        }
        return addSeparator;
    }

    protected CharSequence makeMissingDependenciesText(Resources res, int subProductIndex) {
        StringBuilder builder = new StringBuilder();
        if (subProductIndex < 0) {
            appendMissingDependencies(builder, res, mDependencies.get(GENERAL_PRODUCT_INDEX), false);
        } else {
            boolean addSeparator;
            addSeparator = appendMissingDependencies(builder, res, mDependencies.get(GENERAL_PRODUCT_INDEX), false);
            appendMissingDependencies(builder, res, mDependencies.get(subProductIndex), addSeparator);
        }
        return builder.toString();
    }

    public void makeDependencies() {

    }

    public final String getKey() {
        return mKey;
    }

    public boolean isImportant() {
        return false;
    }
}
