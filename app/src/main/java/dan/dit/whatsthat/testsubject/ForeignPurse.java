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

package dan.dit.whatsthat.testsubject;

import android.text.TextUtils;
import android.util.Log;

import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Wraps a Purse object and offers some more high level access functions to it. This prevents accidental
 * changes and inconsistencies. The methods provided are thread safe and will make sure that something
 * can only be purchased if there is enough current score. Methods will throw an exception if the given cost is negative
 * or if an empty key is given to them.
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

    public synchronized boolean purchaseHigherValue(final String key, final int cost, final int newValue) {
        if (TextUtils.isEmpty(key) || cost < 0) {
            throw new IllegalArgumentException("No key or illegal cost to purchaseFeature: " + key + ": " + cost);
        }
        if (mPurse.getCurrentScore() < cost) {
            return false;
        }
        if (mPurse.mShopWallet.editEntry(key).set(newValue)) {
            mPurse.spentScore(cost);
            return true;
        }
        return false;
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

    public synchronized boolean toggleFeature(final String key) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("No key to toggleFeature.");
        }
        int currValue = mPurse.mShopWallet.getEntryValue(key);
        if (currValue == WalletEntry.FALSE) {
            return false; // not yet owned
        }
        boolean wasEven = currValue % 2 == 0;
        currValue++;
        if (currValue < 0) {
            // overflow case
            currValue = wasEven ? 1 : 2;
            mPurse.mShopWallet.removeEntry(key);
        }
        mPurse.mShopWallet.editEntry(key, 1).set(currValue);
        return true;
    }

    public boolean hasToggleableFeature(final String key) {
        return mPurse.hasToggleableFeature(key);
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
