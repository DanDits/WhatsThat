package dan.dit.whatsthat.testsubject.shopping;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.ForeignPurse;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 29.07.15.
 */
public class ShopArticleHolder {
    private OnArticleChangedListener mListener;
    private ForeignPurse mPurse;
    private List<ShopArticle> mArticles;
    public static final String ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.CIRCLE_INSTANCE.getFullName() + "_divide_by_move_feature";
    public static final String ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE = PracticalRiddleType.TRIANGLE_INSTANCE.getFullName() + "_divided_by_move_feature";

    public interface OnArticleChangedListener {
        void onArticleChanged(ShopArticle article);
    }

    public ShopArticleHolder(ForeignPurse purse) {
        mPurse = purse;
        makeArticles();
    }

    public void setOnArticleChangedListener(OnArticleChangedListener listener) {
        mListener = listener;
        for (ShopArticle article : mArticles) {
            article.setOnArticleChangedListener(mListener);
        }
    }

    private void makeArticles() {
        mArticles = new ArrayList<>();
        addArticle(new ShopArticleSimple(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_circle_divide_by_move_feature_name, R.string.article_circle_divide_by_move_feature_descr, R.drawable.icon_circle, 6));
        addArticle(new ShopArticleSimple(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE, mPurse, R.string.article_triangle_divide_by_move_feature_name, R.string.article_triangle_divide_by_move_feature_descr, R.drawable.icon_triangle, 6));
        addHintArticles();
        //TODO add articles
    }

    public ShopArticle getArticle(String key) {
        for (ShopArticle article : mArticles) {
            if (article.getKey().equals(key)) {
                return article;
            }
        }
        return null;
    }

    public void makeDependencies() {
        for (ShopArticle art : mArticles) {
            art.makeDependencies();
        }
        getArticle(ARTICLE_KEY_CIRCLE_DIVIDE_BY_MOVE_FEATURE).addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.CIRCLE_INSTANCE), ShopArticle.GENERAL_DEPENDENCY_INDEX);
        getArticle(ARTICLE_KEY_TRIANGLE_DIVIDE_BY_MOVE_FEATURE).addDependency(TestSubject.getInstance().getRiddleTypeDependency(PracticalRiddleType.TRIANGLE_INSTANCE), ShopArticle.GENERAL_DEPENDENCY_INDEX);

    }

    private void addArticle(ShopArticle article) {
        mArticles.add(article);
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
        return mArticles.size();
    }

    public ShopArticle getArticle(int index) {
        return mArticles.get(index);
    }

    public int getCurrentScore() {
        return mPurse.getCurrentScore();
    }
}
