package dan.dit.whatsthat.testsubject.shopping;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleFilter;

/**
 * Created by daniel on 29.07.15.
 */
public class ShopArticleHolder {
    private final Context mApplicationContext;
    private OnArticleChangedListener mListener;
    private ForeignPurse mPurse;
    private List<ShopArticle> mFilteredArticles;
    private List<ShopArticle> mAllArticles;
    public static final String ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.CIRCLE_INSTANCE.getFullName() + "_divide_by_move_feature";
    public static final String ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.TRIANGLE_INSTANCE.getFullName() + "_divided_by_move_feature";
    private List<ShopArticleFilter> mFilters;

    public List<ShopArticleFilter> getFilters() {
        return mFilters;
    }

    public void setFilters(List<ShopArticleFilter> filters) {
        mFilters = filters;
        applyFilters();
    }

    public void applyFilters() {
        mFilteredArticles.clear();
        mFilteredArticles.addAll(mAllArticles);
        if (mFilters != null && !mFilters.isEmpty()) {
            Iterator<ShopArticle> artIt = mFilteredArticles.iterator();
            while (artIt.hasNext()) {
                ShopArticle art = artIt.next();
                boolean positiveFilterFound = false;
                for (ShopArticleFilter filter : mFilters) {
                    if (filter.isActive() && filter.check(art)) {
                        positiveFilterFound = true;
                        break;
                    }
                }
                if (!positiveFilterFound) {
                    artIt.remove();
                }
            }
        }
    }

    public List<ShopArticle> getAllArticles() {
        return mAllArticles;
    }

    public void closeArticles() {
        for (ShopArticle article : mAllArticles) {
            article.onClose();
        }
    }

    public ForeignPurse getPurse() {
        return mPurse;
    }

    public interface OnArticleChangedListener {
        void onArticleChanged(ShopArticle article);
    }

    public ShopArticleHolder(Context applicationContext, ForeignPurse purse) {
        mPurse = purse;
        mApplicationContext = applicationContext;
        makeArticles();
    }

    public void setOnArticleChangedListener(OnArticleChangedListener listener) {
        mListener = listener;
        for (ShopArticle article : mAllArticles) {
            article.setOnArticleChangedListener(mListener);
        }
    }

    private void makeArticles() {
        mAllArticles = new ArrayList<>();
        mFilteredArticles = new ArrayList<>();
        addArticle(new ShopArticleSimple(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_circle_divide_by_move_feature_name, R.string.article_circle_divide_by_move_feature_descr, R.drawable.icon_circle, 6));
        addArticle(new ShopArticleSimple(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_triangle_divide_by_move_feature_name, R.string.article_triangle_divide_by_move_feature_descr, R.drawable.icon_triangle, 6));
        addHintArticles();
        addArticle(new ShopArticleDownload(mApplicationContext, mPurse, R.string.download_article_name, R.string.download_article_descr, R.drawable.icon_general, 0,
                "daniel", "test1", 2, "https://www.dropbox.com/s/pqcigkzt0q0yqhr/bla.zip?dl=1"));
        //TODO add articles
        sortArticles();
    }

    private void sortArticles() {
        final List<ShopArticle> originalArticles = new ArrayList<>(mAllArticles);
        Collections.sort(mAllArticles, new Comparator<ShopArticle>() {
            private List<Integer> mFoundIcons = new ArrayList<>(mAllArticles.size());
            @Override
            public int compare(ShopArticle t1, ShopArticle t2) {
                int icon1 = t1.getIconResId();
                int icon2 = t2.getIconResId();
                int index1 = mFoundIcons.indexOf(icon1);
                int index2 = mFoundIcons.indexOf(icon2);
                int originalOrder = originalArticles.indexOf(t1) - originalArticles.indexOf(t2);
                if (index1 == -1 && index2 == -1) {
                    // both icons new, keep order inherited from original list and append to found icons
                    if (icon1 == icon2) {
                        mFoundIcons.add(icon1);
                        return originalOrder;
                    } else {
                        if (originalOrder > 0) {
                            // t1 further in back of list
                            mFoundIcons.add(icon2);
                            mFoundIcons.add(icon1);
                            return 1;
                        } else {
                            mFoundIcons.add(icon1);
                            mFoundIcons.add(icon2);
                            return -1;
                        }
                    }
                } else {
                    if (index1 == -1) {
                        mFoundIcons.add(icon1);
                        return originalOrder;
                    } else if (index2 == -1) {
                        mFoundIcons.add(icon2);
                        return originalOrder;
                    } else {
                        //both icons already in list, inherit order of original list
                        if (index1 == index2) {
                            return originalOrder;
                        } else {
                            return index1 - index2;
                        }
                    }
                }
            }
        });
    }

    public ShopArticle getArticle(String key) {
        for (ShopArticle article : mAllArticles) {
            if (article.getKey().equals(key)) {
                return article;
            }
        }
        return null;
    }

    public void makeDependencies() {
        for (ShopArticle art : mAllArticles) {
            art.makeDependencies();
        }
        getArticle(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE).addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.CIRCLE_INSTANCE), ShopArticle.GENERAL_DEPENDENCY_INDEX);
        getArticle(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE).addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.TRIANGLE_INSTANCE), ShopArticle.GENERAL_DEPENDENCY_INDEX);
        getArticle(ShopArticleRiddleHints.makeKey(PracticalRiddleType.CIRCLE_INSTANCE)).addDependency(TestSubject.getInstance().makeAchievementDependency(PracticalRiddleType.CIRCLE_INSTANCE, AchievementCircle.Achievement1.NUMBER), 1);
    }

    private void addArticle(ShopArticle article) {
        mAllArticles.add(article);
        article.setOnArticleChangedListener(mListener);
    }

    private void addHintArticles() {
        for (PracticalRiddleType type : PracticalRiddleType.getAll()) {
            if (type.getTotalAvailableHintsCount() > 0) {
                addArticle(new ShopArticleRiddleHints(type, mPurse, R.string.article_hint_name, R.string.article_hint_descr));
            }
        }
    }

    public int getArticlesCount() {
        return mFilteredArticles.size();
    }

    public ShopArticle getArticle(int index) {
        return mFilteredArticles.get(index);
    }

    public int getCurrentScore() {
        return mPurse.getCurrentScore();
    }
}
