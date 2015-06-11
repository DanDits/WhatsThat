package dan.dit.whatsthat.testsubject;

import android.content.Context;

import dan.dit.whatsthat.testsubject.wallet.Wallet;

/**
 * Created by daniel on 10.06.15.
 */
public class Purse {
    private static final String SCORE_WALLET = "score";
    private static final String REWARD_WALLET = "reward";
    private static final String SHOP_WALLET = "shop";
    public final Wallet mScoreWallet;
    public final Wallet mRewardWallet;
    public final Wallet mShopWallet;

    public Purse(Context context) {
        mScoreWallet = new Wallet(context, SCORE_WALLET);
        mRewardWallet = new Wallet(context, REWARD_WALLET);
        mShopWallet = new Wallet(context, SHOP_WALLET);
    }

}
