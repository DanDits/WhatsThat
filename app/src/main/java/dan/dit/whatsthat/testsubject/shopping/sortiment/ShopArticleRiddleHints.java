package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ConfirmProduct;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.testsubject.shopping.sortiment.HintProduct;

/**
 * Created by daniel on 29.07.15.
 */
public class ShopArticleRiddleHints extends ShopArticle {
    private static final String KEY_SUFFIX = "_riddle_hints";
    private PracticalRiddleType mType;
    private List<Integer> mCosts;
    private ConfirmProduct mConfirmProduct;
    private Map<Integer, HintProduct> mHints;

    public static String makeKey(PracticalRiddleType type) {
        return type.getFullName() + KEY_SUFFIX;
    }

    public ShopArticleRiddleHints(PracticalRiddleType type, ForeignPurse purse, int nameResId, int descrResId) {
        super(makeKey(type), purse, nameResId, descrResId, type.getIconResId());
        mType = type;
        mCosts = mType.getHintCosts();
        mHints = new HashMap<>();
        if (mCosts == null || mCosts.isEmpty()) {
            throw new IllegalArgumentException("No costs given.");
        }
    }

    @Override
    public CharSequence getName(Resources res) {
        return res.getString(mNameResId, mPurse.getCurrentRiddleHintNumber(mType), mPurse.getAvailableRiddleHintsCount(mType));
    }

    @Override
    public int isPurchasable(int subProductIndex) {
        if (subProductIndex < 0 || subProductIndex >= mType.getTotalAvailableHintsCount()) {
            return HINT_NOT_PURCHASABLE_OTHER; // no valid hint index
        }
        int availableHints = mPurse.getAvailableRiddleHintsCount(mType);
        if (subProductIndex < availableHints) {
            return HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }
        if (subProductIndex > availableHints) {
            return HINT_NOT_PURCHASABLE_OTHER; // only the next one is purchasable in the list
        }
        if (areDependenciesMissing(subProductIndex)) {
            return HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING;
        }

        return getSpentScore(subProductIndex) <= mPurse.getCurrentScore() ? HINT_PURCHASABLE : HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }

    @Override
    public boolean isClickable(int subProductIndex) {
        int purchasableHint = isPurchasable(subProductIndex);
        return purchasableHint == HINT_PURCHASABLE || (purchasableHint == HINT_NOT_PURCHASABLE_ALREADY_PURCHASED && mPurse.getCurrentRiddleHintNumber(mType) > subProductIndex);
    }

    @Override
    public CharSequence getSpentScore(Resources resources) {
        int spent = 0;
        final int purchasedCount = mPurse.getAvailableRiddleHintsCount(mType);
        for (int i = 0; i <  purchasedCount; i++) {
            spent += mCosts.get(Math.min(i, mCosts.size() - 1));
        }
        if (spent == 0) {
            return "";
        }
        return resources.getString(R.string.shop_article_spent, spent);
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        int cost;
        if (subProductIndex >= 0 && subProductIndex < mCosts.size()) {
            cost = mCosts.get(subProductIndex);
        } else {
            cost = mCosts.get(mCosts.size() - 1);
        }
        return Math.max(cost, 0);
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        int cost = getSpentScore(subProductIndex);
        if (cost > 0) {
            return String.valueOf(cost);
        } else {
            return resources.getString(R.string.shop_article_free);
        }
    }

    @Override
    public int getSubProductCount() {
        return Math.min(mPurse.getAvailableRiddleHintsCount(mType) + 1, mType.getTotalAvailableHintsCount());
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        int purchasableHint = isPurchasable(subProductIndex);
        if (purchasableHint == HINT_PURCHASABLE || purchasableHint == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE || purchasableHint == HINT_NOT_PURCHASABLE_DEPENDENCIES_MISSING) {
            if (mConfirmProduct == null) {
                mConfirmProduct = new ConfirmProduct(this);
            }
            if (mConfirmProduct.hasNoView()) {
                mConfirmProduct.inflateView(inflater);
            }
            CharSequence costText;
            CharSequence depText;
            if (mConfirmProduct.hasNoView()) {
                costText = String.valueOf(getSpentScore(subProductIndex));
                depText = "---";
            } else {
                Resources res = mConfirmProduct.getView().getResources();
                costText = getCostText(res, subProductIndex);
                depText = makeMissingDependenciesText(res, subProductIndex);
            }
            mConfirmProduct.setConfirmable(purchasableHint, costText, depText,
                    getSpentScore(subProductIndex) > 0 && (purchasableHint == HINT_PURCHASABLE || purchasableHint == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE) ? R.drawable.think_currency_small : 0);
            return mConfirmProduct;
        } else if (purchasableHint == HINT_NOT_PURCHASABLE_ALREADY_PURCHASED) {
            HintProduct hint = mHints.get(subProductIndex);
            boolean alreadyRead = mPurse.getCurrentRiddleHintNumber(mType) > subProductIndex;
            if (hint == null) {
                hint = new HintProduct(mType, subProductIndex, alreadyRead);
                hint.inflateView(inflater);
                mHints.put(subProductIndex, hint);
            }
            hint.setAlreadyRead(alreadyRead);
            return hint;
        }
        Log.e("HomeStuff", "Obtaining subproduct that is not purchasable or purchased: " + purchasableHint + " hints: " + mHints + " costs " + mCosts + " total available " + mType.getTotalAvailableHintsCount() + " current available " + mPurse.getAvailableRiddleHintsCount(mType));
        return null;
    }

    @Override
    public void onChildClick(SubProduct product) {
        int nextIndex = mPurse.getAvailableRiddleHintsCount(mType);
        if (product == mConfirmProduct && isPurchasable(nextIndex) == HINT_PURCHASABLE && mPurse.purchaseHint(mType, getSpentScore(nextIndex)) && mListener != null) {
            mListener.onArticleChanged(this);
        }
    }

    @Override
    public int getPurchaseProgressPercent() {
        return (int) (100. * mPurse.getAvailableRiddleHintsCount(mType) / (double) mType.getTotalAvailableHintsCount());
    }

    @Override
    public void makeDependencies() {
        addDependency(TestSubject.getInstance().getRiddleTypeDependency(mType), GENERAL_DEPENDENCY_INDEX);
    }
}
