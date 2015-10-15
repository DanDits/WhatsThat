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

package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.content.Context;

import junit.framework.Test;

import java.util.ArrayList;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementJumper;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementLazor;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.LevelDependency;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.ShopArticle;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleHolder;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleMulti;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleSimple;
import dan.dit.whatsthat.testsubject.shopping.ShopArticleToggleable;
import dan.dit.whatsthat.util.dependencies.Dependency;
import dan.dit.whatsthat.util.dependencies.MinValueDependency;
import dan.dit.whatsthat.util.dependencies.OrDependency;

/**
 * Created by daniel on 20.08.15.
 */
public class SortimentHolder extends ShopArticleHolder {
    public static final String ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE = PracticalRiddleType.JUMPER_INSTANCE.getFullName() + "_start_further";
    public static final String ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.CIRCLE_INSTANCE.getFullName() + "_divide_by_move_feature";
    public static final String ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.TRIANGLE_INSTANCE.getFullName() + "_divided_by_move_feature";
    public static final String ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR = PracticalRiddleType.SNOW_INSTANCE.getFullName() + "_control_by_orientation_sensor";
    public static final String ARTICLE_KEY_LAZOR_PROTECTION_AT_DIFFICULTY = PracticalRiddleType.LAZOR_INSTANCE.getFullName() + "_protection_at_difficulty";
    public static final String ARTICLE_KEY_SHARE_AS_IMAGE_FEATURE = "share_experiment_as_image";
    private Dependency mAnyDownloadProductPurchasedDependency;
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
        addArticle(new ShopArticleSimple(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_triangle_divide_by_move_feature_name, R.string.article_triangle_divide_by_move_feature_descr, R.drawable.icon_triangle, 200));
        addArticle(new ShopArticleToggleable(ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR, mPurse, R.string.article_snow_feature_orientation_sensor_name, R.string.article_snow_feature_orientation_sensor_descr, R.drawable.icon_snow,
                R.string.article_snow_feature_orientation_sensor_on, R.string.article_snow_feature_orientation_sensor_off, 400));
        addHintArticles();
        addArticle(new ShopArticleSimple(ARTICLE_KEY_SHARE_AS_IMAGE_FEATURE, mPurse, R.string.article_share_as_image_feature_name, R.string.article_share_as_image_feature_descr, R.drawable.icon_general, 150));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_technical, R.drawable.ic_download, 25,
                "fabian", "crackle42_pics", 5, "https://www.dropbox.com/s/b5nydme8eg1lq8e/fabian_crackle42_pics.wtb?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_general, R.drawable.ic_download, 25,
                "fabian", "FaDi_pics", 8, "https://www.dropbox.com/s/drvqyfc9s9ar8j6/fabian_FaDi_pics.wtb?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_housing, R.drawable.ic_download, 40,
                "fabian", "housing", 8, "https://www.dropbox.com/s/4b28so70j5jugpo/fabian_housing.wtb?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_nature, R.drawable.ic_download, 60,
                "fabian", "nature", 9, "https://www.dropbox.com/s/6pgvwdyfvbp95n2/fabian_nature.wtb?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_animal, R.drawable.ic_download, 25,
                "fabian", "NoAnimal", 5, "https://www.dropbox.com/s/j6ofjcberhvt6tz/fabian_NoAnimal.wtb?dl=1"));

        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr_superlative, R.drawable.ic_download, 50,
                "fabian", "superlative", 8, "https://www.dropbox.com/s/vx5f7lkim0jja4j/fabian_superlative.wtb?dl=1"));

        addArticle(new ShopArticleSimple(ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE, mPurse, R.string.article_jumper_start_further_name, R.string.article_jumper_start_further_descr, R.drawable.icon_jumprun, 700));
        addArticle(new ShopArticleMulti(ARTICLE_KEY_LAZOR_PROTECTION_AT_DIFFICULTY, mPurse, R.string.riddle_type_lazor_article_protection_at_name, R.string.riddle_type_lazor_article_protection_at_descr, R.drawable.icon_lazor, R.array.riddle_type_lazor_article_protection_at_products,
                new int[] {25, 50, 100, 300}));
        sortArticles();
    }

    public Dependency getAnyDownloadProductPurchasedDependency() {
        if (mAnyDownloadProductPurchasedDependency != null) {
            return mAnyDownloadProductPurchasedDependency;
        }
        OrDependency dep = new OrDependency();
        for (ShopArticle article : mAllArticles) {
            if (article.getKey().startsWith(ShopArticleDownload.KEY_PREFIX)) {
                dep.add(TestSubject.getInstance().makeFeatureAvailableDependency(article.getKey()
                        , 2));
            }
        }
        mAnyDownloadProductPurchasedDependency = dep;
        return mAnyDownloadProductPurchasedDependency;
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
        getArticle(LevelUpArticle.KEY_LEVEL_UP_ARTICLE)
                .addDependency(getAnyDownloadProductPurchasedDependency(), 2); // for level 2 we need to purchase any download product to know it is there
        getArticle(RiddleArticle.KEY_CHOOSE_RIDDLE_ARTICLE)
                .addDependency(LevelDependency.getInstance(5), PracticalRiddleType.ALL_PLAYABLE_TYPES.indexOf(PracticalRiddleType.DICE_INSTANCE))
                .addDependency(LevelDependency.getInstance(4), PracticalRiddleType.ALL_PLAYABLE_TYPES.indexOf(PracticalRiddleType.JUMPER_INSTANCE))
                .addDependency(LevelDependency.getInstance(4), PracticalRiddleType.ALL_PLAYABLE_TYPES.indexOf(PracticalRiddleType.MEMORY_INSTANCE))
                .addDependency(LevelDependency.getInstance(1), PracticalRiddleType.ALL_PLAYABLE_TYPES.indexOf(PracticalRiddleType.TRIANGLE_INSTANCE));
        getArticle(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.CIRCLE_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.TRIANGLE_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(TestSubject.getInstance().makeProductPurchasedDependency(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, ShopArticle.GENERAL_PRODUCT_INDEX), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_SHARE_AS_IMAGE_FEATURE)
                .addDependency(LevelDependency.getInstance(3), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_SNOW_FEATURE_ORIENTATION_SENSOR)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.SNOW_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ShopArticleRiddleHints.makeKey(PracticalRiddleType.CIRCLE_INSTANCE))
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, AchievementCircle.Achievement1.NUMBER), 1);
        getArticle(ARTICLE_KEY_JUMPER_START_FURTHER_FEATURE)
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, AchievementJumper.ACHIEVEMENT_SUPER_MARIO), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.JUMPER_INSTANCE, AchievementJumper.ACHIEVEMENT_LACK_OF_TALENT), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(LevelDependency.getInstance(3), ShopArticle.GENERAL_PRODUCT_INDEX);
        getArticle(ARTICLE_KEY_LAZOR_PROTECTION_AT_DIFFICULTY)
                .addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.LAZOR_INSTANCE), ShopArticle.GENERAL_PRODUCT_INDEX)
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.LAZOR_INSTANCE, AchievementLazor.Achievement2.NUMBER), 0) // City protector
                .addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.LAZOR_INSTANCE, AchievementLazor.Achievement7.NUMBER), 1); // Shield protection
    }
}
