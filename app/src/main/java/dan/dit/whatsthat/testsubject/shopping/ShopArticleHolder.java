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
import dan.dit.whatsthat.testsubject.shopping.filter.ShopArticleGroupFilter;

/**
 * Created by daniel on 29.07.15.
 */
public abstract class ShopArticleHolder {
    protected final Context mApplicationContext;
    private OnArticleChangedListener mListener;
    protected ForeignPurse mPurse;
    protected List<ShopArticle> mFilteredArticles;
    protected List<ShopArticle> mAllArticles;
    private ShopArticleGroupFilter mRootFilter;


    public void setFilter(ShopArticleGroupFilter rootFilter) {
        mRootFilter = rootFilter;
        applyFilters();
    }

    public void applyFilters() {
        mFilteredArticles.clear();
        mFilteredArticles.addAll(mAllArticles);
        if (mRootFilter != null) {
            Iterator<ShopArticle> artIt = mFilteredArticles.iterator();
            while (artIt.hasNext()) {
                if (!mRootFilter.check(artIt.next())) {
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

    protected abstract void makeArticles();

    protected void sortArticles() {
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
    }

    protected final void addArticle(ShopArticle article) {
        mAllArticles.add(article);
        article.setOnArticleChangedListener(mListener);
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
