package dan.dit.whatsthat.testsubject;

import android.content.res.Resources;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.util.dependencies.Dependency;

/**
 * Created by daniel on 07.01.16.
 */
public class DependencyHolder {
    private DependencyHolder() {}

    public static class FeatureAvailableDependency extends Dependency {
        private ShopArticle mArticle;
        private int mRequiredAmount;
        public FeatureAvailableDependency(ShopArticle featureArticle, int requiredAmount) {
            mArticle = featureArticle;
            mRequiredAmount = requiredAmount;
            if (mArticle == null) {
                throw new IllegalArgumentException("No article given.");
            }
        }

        @Override
        public boolean isFulfilled() {
            return TestSubject.isInitialized() &&
                    TestSubject.getInstance().getShopValue(mArticle.getKey()) >= mRequiredAmount;
        }

        @Override
        public CharSequence getName(Resources res) {
            return mArticle.getName(res) + ">" + (mRequiredAmount - 1);
        }
    }


    public static class ProductPurchasedDependency extends Dependency {
        private final int mProduct;
        private final ShopArticle mArticle;

        public ProductPurchasedDependency(ShopArticle article, int product) {
            mArticle = article;
            mProduct = product;
            if (article == null) {
                throw new IllegalArgumentException("No article for dependency given.");
            }
        }
        @Override
        public boolean isFulfilled() {
            return mArticle.isPurchasable(mProduct) == ShopArticle.HINT_NOT_PURCHASABLE_ALREADY_PURCHASED;
        }

        @Override
        public CharSequence getName(Resources res) {
            if (mProduct < 0) {
                return mArticle.getName(res);
            } else {
                return mArticle.getName(res) + " #" + (mProduct + 1);
            }
        }
    }

    public static class RiddleTypeDependency extends Dependency {
        private final TestSubject mTestSubject;
        private final PracticalRiddleType mType;
        public RiddleTypeDependency(PracticalRiddleType type, TestSubject testSubject) {
            mType = type;
            mTestSubject = testSubject;
        }
        @Override
        public boolean isFulfilled() {
            for (TestSubjectRiddleType testType : mTestSubject.getTypesController().getAll()) {
                if (testType.getType().equals(mType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CharSequence getName(Resources res) {
            return res.getString(mType.getNameResId());
        }
    }

    public static class ClaimedAchievementDependency extends Dependency {
        private Achievement mAchievement;

        public ClaimedAchievementDependency(Achievement achievement) {
            mAchievement = achievement;
            if (mAchievement == null) {
                throw new IllegalArgumentException("No achievement given.");
            }
        }

        @Override
        public boolean isFulfilled() {
            return mAchievement.isAchieved() && !mAchievement.isRewardClaimable();
        }

        @Override
        public CharSequence getName(Resources res) {
            return mAchievement.getName(res);
        }
    }
}
