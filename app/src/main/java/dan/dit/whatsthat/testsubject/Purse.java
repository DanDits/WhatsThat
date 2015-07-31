package dan.dit.whatsthat.testsubject;

import android.content.Context;
import android.util.Log;

import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.wallet.Wallet;

/**
 * Created by daniel on 10.06.15.
 */
public class Purse {
    private static final String SCORE_WALLET = "score";
    private static final String SHOP_WALLET = "shop";
    protected static final String SHW_KEY_TESTSUBJECT_LEVEL = "testsubject_level";
    protected static final String SHW_KEY_SKIPABLE_GAMES = "skipable_games";
    protected static final String SW_KEY_SPENT_SCORE = "testsubject_spent_score";
    protected static final String SW_KEY_SOLVED_RIDDLE_SCORE = "solved_riddle_score";
    protected static final String SW_KEY_ACHIEVEMENT_SCORE = "achievement_score";
    private static final String SHW_KEY_CURRENT_RIDDLE_HINT = "current_riddle_hint_";
    private static final String SHW_KEY_AVAILABLE_RIDDLE_HINT_COUNT = "available_riddle_hint_";


    public final Wallet mScoreWallet;
    public final Wallet mShopWallet;

    public Purse(Context context) {
        mScoreWallet = new Wallet(context, SCORE_WALLET);
        mShopWallet = new Wallet(context, SHOP_WALLET);
    }

    public int getCurrentScore() {
        int score =
                mScoreWallet.getEntryValue(SW_KEY_SOLVED_RIDDLE_SCORE)
                + mScoreWallet.getEntryValue(SW_KEY_ACHIEVEMENT_SCORE)
                - mScoreWallet.getEntryValue(SW_KEY_SPENT_SCORE);
        Log.d("HomeStuff", "Getting current score: " + score);
        return score;
    }

    public void spentScore(final int score) {
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
}
