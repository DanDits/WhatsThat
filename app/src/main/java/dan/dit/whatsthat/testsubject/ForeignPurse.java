package dan.dit.whatsthat.testsubject;

import android.text.TextUtils;
import android.util.Log;

import dan.dit.whatsthat.riddle.achievement.MiscAchievement;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Created by daniel on 29.07.15.
 */
public class ForeignPurse {

    private final Purse mPurse;

    public ForeignPurse(Purse purse) {
        mPurse = purse;
        if (mPurse == null) {
            throw new IllegalArgumentException("No purse given.");
        }
    }


    public int getShopValue(String key) {
        return mPurse.mShopWallet.getEntryValue(key);
    }

    public boolean hasShopValue(String key) {
        return mPurse.mShopWallet.getEntryValue(key) != WalletEntry.FALSE;
    }

    public int getCurrentScore() {
        return mPurse.getCurrentScore();
    }

    public synchronized boolean purchaseFeature(final String key, final int cost) {
        if (TextUtils.isEmpty(key) || cost < 0) {
            throw new IllegalArgumentException("No key or illegal cost to purchaseFeature: " + key + ": " + cost);
        }
        if (mPurse.getCurrentScore() < cost) {
            return false;
        }
        int oldScore = mPurse.getCurrentScore();
        mPurse.mShopWallet.editEntry(key).setTrue();
        mPurse.spentScore(cost);
        TestSubject.getInstance().getAchievementHolder().getMiscData().updateMappedValue(MiscAchievementHolder.KEY_FEATURES_PURCHASED, key);
        Log.d("HomeStuff", "Purchased " + key + " for " + cost + " (score change: " + oldScore + "->" + mPurse.getCurrentScore() + ")");
        return true;
    }

    public synchronized boolean purchase(final String key, final int cost, final int amount) {
        if (TextUtils.isEmpty(key) || cost < 0) {
            throw new IllegalArgumentException("No key or illegal cost to purchaseFeature: " + key + ": " + cost);
        }
        if (mPurse.getCurrentScore() < cost) {
            return false;
        }
        mPurse.mShopWallet.editEntry(key).add(amount);
        mPurse.spentScore(cost);
        return true;
    }


    public int getAvailableRiddleHintsCount(PracticalRiddleType type) {
        return mPurse.getAvailableRiddleHintsCount(type);
    }

    public int getCurrentRiddleHintNumber(PracticalRiddleType type) {
        return mPurse.getCurrentRiddleHintNumber(type);
    }

    public synchronized boolean purchaseHint(final PracticalRiddleType type, final int cost) {
        if (type == null || cost < 0) {
            throw new IllegalArgumentException("No type or illegal cost to purchaseFeature hint: " + cost);
        }
        if (mPurse.getCurrentScore() < cost) {
            return false;
        }
        if (getAvailableRiddleHintsCount(type) >= type.getTotalAvailableHintsCount()) {
            return false;
        }
        int oldScore = mPurse.getCurrentScore();
        mPurse.increaseAvailableRiddleHintNumber(type);
        mPurse.spentScore(cost);
        Log.d("HomeStuff", "Purchased hint for " + type + " for " + cost + " (score change: " + oldScore + "->" + mPurse.getCurrentScore() + ")");
        return true;
    }
}
