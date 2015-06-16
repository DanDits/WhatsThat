package dan.dit.whatsthat.testsubject;

import android.content.Context;
import android.util.Log;

import dan.dit.whatsthat.testsubject.wallet.Wallet;

/**
 * Created by daniel on 10.06.15.
 */
public class Purse {
    private static final String SCORE_WALLET = "score";
    private static final String REWARD_WALLET = "reward";
    private static final String SHOP_WALLET = "shop";
    protected static final String RW_KEY_TESTSUBJECT_LEVEL = "testsubject_level";
    protected static final String RW_KEY_SKIPABLE_GAMES = "skipable_games";
    protected static final String SW_KEY_SPENT_SCORE = "testsubject_spent_score";
    protected static final String SW_KEY_SOLVED_RIDDLE_SCORE = "solved_riddle_score";
    protected static final String SW_KEY_ACHIEVEMENT_SCORE = "achievement_score";


    public final Wallet mScoreWallet;
    public final Wallet mRewardWallet;
    public final Wallet mShopWallet;

    public Purse(Context context) {
        mScoreWallet = new Wallet(context, SCORE_WALLET);
        mRewardWallet = new Wallet(context, REWARD_WALLET);
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

}
