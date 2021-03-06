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

import android.content.Context;

import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.wallet.Wallet;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Created by daniel on 10.06.15.
 */
class Purse {
    private static final String SCORE_WALLET = "score";
    private static final String SHOP_WALLET = "shop";
    public static final String SHW_KEY_TESTSUBJECT_LEVEL = "testsubject_level";
    static final String SHW_KEY_SKIPABLE_GAMES = "skipable_games";
    private static final String SW_KEY_SPENT_SCORE = "testsubject_spent_score";
    static final String SW_KEY_SOLVED_RIDDLE_SCORE = "solved_riddle_score";
    static final String SW_KEY_PURCHASED_CURRENCY_SCORE = "purchased_currency_score";
    static final String SW_KEY_ACHIEVEMENT_SCORE = "achievement_score";
    private static final String SHW_KEY_CURRENT_RIDDLE_HINT = "current_riddle_hint_";
    private static final String SHW_KEY_AVAILABLE_RIDDLE_HINT_COUNT = "available_riddle_hint_";
    public static final String SHW_KEY_SPENT_SCORE_ON_LEVEL_UP = "shw_level_up_spent_score";


    public final Wallet mScoreWallet;
    public final Wallet mShopWallet;

    public Purse(Context context) {
        mScoreWallet = new Wallet(context, SCORE_WALLET);
        mShopWallet = new Wallet(context, SHOP_WALLET);
    }

    public int getCurrentScore() {
        return  mScoreWallet.getEntryValue(SW_KEY_SOLVED_RIDDLE_SCORE)
                + mScoreWallet.getEntryValue(SW_KEY_ACHIEVEMENT_SCORE)
                + mScoreWallet.getEntryValue(SW_KEY_PURCHASED_CURRENCY_SCORE)
                - mScoreWallet.getEntryValue(SW_KEY_SPENT_SCORE);
    }

    public void spendScore(final int score) {
        if (score < 0) {
            throw new IllegalArgumentException("No score to spent: " + score);
        }
        mScoreWallet.editEntry(SW_KEY_SPENT_SCORE).add(score);
    }

    public int getAchievementScore() {
        return mScoreWallet.getEntryValue(SW_KEY_ACHIEVEMENT_SCORE);
    }

    public int getCurrentRiddleHintNumber(PracticalRiddleType type) {
        return mShopWallet.getEntryValue(SHW_KEY_CURRENT_RIDDLE_HINT + type.getFullName());
    }

    public int increaseCurrentRiddleHintNumber(PracticalRiddleType type) {
        mShopWallet.editEntry(SHW_KEY_CURRENT_RIDDLE_HINT + type.getFullName()).add(1);
        return getCurrentRiddleHintNumber(type);
    }

    public int getAvailableRiddleHintsCount(PracticalRiddleType type) {
        return mShopWallet.getEntryValue(SHW_KEY_AVAILABLE_RIDDLE_HINT_COUNT + type.getFullName());
    }

    public void increaseAvailableRiddleHintNumber(PracticalRiddleType type) {
        mShopWallet.editEntry(SHW_KEY_AVAILABLE_RIDDLE_HINT_COUNT + type.getFullName()).add(1);
    }

    public void setAvailableRiddleHintsAtStartCount(PracticalRiddleType type) {
        mShopWallet.editEntry(SHW_KEY_AVAILABLE_RIDDLE_HINT_COUNT + type.getFullName()).set(Math.min(type.getAvailableHintsAtStartCount(), type.getTotalAvailableHintsCount()));
    }

    public boolean hasToggleableFeature(String featureKey) {
        int entryValue = mShopWallet.getEntryValue(featureKey);
        return entryValue > WalletEntry.FALSE && entryValue % 2 == 1;
    }

    public boolean purchaseLevelUp(final int nextLevelUpCost) {
        if (nextLevelUpCost < 0) {
            return false; // no cost initialized or no level available
        }
        if (getCurrentScore() < nextLevelUpCost) {
            return false; // too little score
        }
        spendScore(nextLevelUpCost);
        mShopWallet.editEntry(SHW_KEY_SPENT_SCORE_ON_LEVEL_UP).add(nextLevelUpCost);
        return true;
    }
}
