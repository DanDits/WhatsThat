package dan.dit.whatsthat.testsubject.shopping;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.dependencies.Dependency;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class ShopArticle {
    protected static final int GENERAL_DEPENDENCY_INDEX = -1;
    public static final int HINT_PURCHASABLE = 1;
    public static final int HINT_NOT_PURCHASABLE_TOO_EXPENSIVE = 0;
    public static final int HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING = -1;
    public static final int HINT_NOT_PURCHASABLE_ALREADY_PURCHASED = -2;
    public static final int HINT_NOT_PURCHASABLE_OTHER = -3;

    private static final int DEFAULT_ICON = R.drawable.icon_plain;
    protected final String mKey;
    protected final int mDescrResId;
    protected final int mNameResId;
    protected ForeignPurse mPurse;
    protected int mIconResId;
    protected ShopArticleHolder.OnArticleChangedListener mListener;
    private Map<Integer, List<Dependency>> mDependencies;

    public ShopArticle(String key, ForeignPurse purse, int nameResId, int descrResId, int iconResId) {
        mPurse = purse;
        mKey = key;
        mNameResId = nameResId;
        mDescrResId = descrResId;
        mIconResId = iconResId;
        mDependencies = new HashMap<>();
        if (mPurse== null) {
            throw new IllegalArgumentException("No purse given.");
        }
        if (TextUtils.isEmpty(mKey)) {
            throw new IllegalArgumentException("Illegal key given.");
        }
    }

    public abstract int isPurchasable(int subProductIndex);

    public abstract CharSequence getSpentScore(Resources resources);
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
            mDependencies.put(subProductIndex < 0 ? GENERAL_DEPENDENCY_INDEX : subProductIndex, deps);
        }
        deps.add(dep);
        return this;
    }

    private boolean checkDependencies(List<Dependency> deps) {
        if (deps == null) {
            return true;
        }
        for (Dependency dep : deps) {
            if (!dep.isFulfilled()) {
                return false;
            }
        }
        return true;
    }

    public boolean areDependenciesFulfilled(int subProductIndex) {
        if (subProductIndex < 0) {
            return checkDependencies(mDependencies.get(GENERAL_DEPENDENCY_INDEX));
        } else {
            if (!checkDependencies(mDependencies.get(GENERAL_DEPENDENCY_INDEX)) || !checkDependencies(mDependencies.get(subProductIndex))) {
                return false;
            }
        }
        return true;
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

    public CharSequence makeMissingDependenciesText(Resources res, int subProductIndex) {
        StringBuilder builder = new StringBuilder();
        if (subProductIndex < 0) {
            appendMissingDependencies(builder, res, mDependencies.get(GENERAL_DEPENDENCY_INDEX), false);
        } else {
            boolean addSeparator = false;
            addSeparator = appendMissingDependencies(builder, res, mDependencies.get(GENERAL_DEPENDENCY_INDEX), addSeparator);
            appendMissingDependencies(builder, res, mDependencies.get(subProductIndex), addSeparator);
        }
        return builder.toString();
    }

    public void makeDependencies() {

    }

    public String getKey() {
        return mKey;
    }
}
