package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.Context;

import junit.framework.Test;

import java.util.ArrayList;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementJumper;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.LevelDependency;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleSimple;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleToggleable;
import dan.dit.whatsthat.util.dependencies.MinValueDependency;

/**
 * Created by daniel on 20.08.15.
 */
public class SortimentHolder extends ShopArticleHolder {
    public static final String ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE = PracticalRiddleType.JUMPER_INSTANCE.getFullName() + "_start_further";
    public static final String ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.CIRCLE_INSTANCE.getFullName() + "_divide_by_move_feature";
    public static final String ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.TRIANGLE_INSTANCE.getFullName() + "_divided_by_move_feature";
    public static final String ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR = PracticalRiddleType.SNOW_INSTANCE.getFullName() + "_control_by_orientation_sensor";

    public SortimentHolder(Context applicationContext, ForeignPurse purse) {
        super(applicationContext, purse);
    }

    @Override
    public void makeArticles() {
        mAllArticles = new ArrayList<>();
        mFilteredArticles = new ArrayList<>();
        addArticle(new RiddleArticle(mPurse));
        addArticle(new LevelUpArticle(mPurse));
        addArticle(new ShopArticleSimple(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_circle_divide_by_move_feature_name, R.string.article_circle_divide_by_move_feature_descr, R.drawable.icon_circle, 100));
        addArticle(new ShopArticleSimple(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_triangle_divide_by_move_feature_name, R.string.article_triangle_divide_by_move_feature_descr, R.drawable.icon_triangle, 100));
        addArticle(new ShopArticleToggleable(ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR, mPurse, R.string.article_snow_feature_orientation_sensor_name, R.string.article_snow_feature_orientation_sensor_descr, R.drawable.icon_snow,
                R.string.article_snow_feature_orientation_sensor_on, R.string.article_snow_feature_orientation_sensor_off, 100));
        addHintArticles();
        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr, R.drawable.ic_download, 0,
                "daniel", "test1", 2, "https://www.dropbox.com/s/pqcigkzt0q0yqhr/bla.zip?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr, R.drawable.ic_download, 0,
                "didi", "danielpix", 2, "https://www.dropbox.com/s/w3h0u9145etdb14/didi_danielpix.wtb?dl=1"));

        addArticle(new ShopArticleSimple(ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE, mPurse, R.string.article_jumper_start_further_name, R.string.article_jumper_start_further_descr, R.drawable.icon_jumprun, 10));

        sortArticles();
    }


    protected void addHintArticles() {
        for (PracticalRiddleType type : PracticalRiddleType.getAll()) {
            if (type.getTotalAvailableHintsCount() > 0) {
                addArticle(new ShopArticleRiddleHints(type, mPurse, R.string.article_hint_name, R.string.article_hint_descr));
            }
        }
    }

    @Override
    public void makeDependencies() {
        super.makeDependencies();

        getArticle(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.CIRCLE_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.TRIANGLE_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(TestSubject.getInstance().makeProductPurchasedDependency(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, ShopArticle.GENERAL_PRODUCT_INDEX), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.SNOW_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ShopArticleRiddleHints.makeKey(PracticalRiddleType.CIRCLE_INSTANCE))
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, AchievementCircle.Achievement1.NUMBER), 1);
        getArticle(ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE)
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, AchievementJumper.ACHIEVEMENT_SUPER_MARIO), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, AchievementJumper.ACHIEVEMENT_LACK_OF_TALENT), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(LevelDependency.getInstance(3), ShopArticle.GENERAL_PRODUCT_INDEX);
    }
}
