package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.res.Resources;
import android.view.LayoutInflater;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Created by daniel on 24.09.15.
 */
public class RiddleArticle extends ShopArticle {
    public static final String KEY_CHOOSE_RIDDLE_ARTICLE = "key_choose_riddle_article";
    protected RiddleArticle(ForeignPurse purse) {
        super(KEY_CHOOSE_RIDDLE_ARTICLE, purse, R.string.article_choose_riddle_name, R.string.article_choose_riddle_descr, R.drawable.icon_general);
        addDependency(new Dependency() {
            @Override
            public boolean isNotFulfilled() {
                return !mPurse.hasShopValue(TestSubject.SHW_KEY_CAN_CHOOSE_NEW_RIDDLE);
            }

            @Override
            public CharSequence getName(Resources res) {
                return res.getString(R.string.article_level_up_dep_new_level);
            }
        }, GENERAL_PRODUCT_INDEX);
    }

    @Override
    public boolean isImportant() {
        int purchasable = isPurchasable(GENERAL_PRODUCT_INDEX);
        return purchasable == HINT_PURCHASABLE || purchasable == HINT_NOT_PURCHASABLE_TOO_EXPENSIVE;
    }

    //TODO implement a subproduct class, an instance per potential riddle type, purchasable if not yet contained in TestSubjectRiddleTypes
    // TODO
    @Override
    public int isPurchasable(int subProductIndex) {

        return 0;
    }

    @Override
    public CharSequence getSpentScore(Resources resources) {
        return null;
    }

    @Override
    public int getSpentScore(int subProductIndex) {
        return 0;
    }

    @Override
    public CharSequence getCostText(Resources resources, int subProductIndex) {
        return null;
    }

    @Override
    public int getSubProductCount() {
        return 0;
    }

    @Override
    public SubProduct getSubProduct(LayoutInflater inflater, int subProductIndex) {
        return null;
    }

    @Override
    public void onChildClick(SubProduct product) {

    }

    @Override
    public int getPurchaseProgressPercent() {
        return 0;
    }
}
