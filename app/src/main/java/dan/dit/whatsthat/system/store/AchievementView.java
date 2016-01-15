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

package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectRiddleType;
import dan.dit.whatsthat.util.ui.LinearLayoutProgressBar;

/**
 * Created by daniel on 16.06.15.
 */
public class AchievementView extends ExpandableListView implements StoreContainer {

    private BaseExpandableListAdapter mAdapter;
    private List<HolderContainer> mContainers;
    private List<HolderContainer> mVisibleContainers;

    public AchievementView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initContainers(context);

        // For claiming rewards
        setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                return claimReward(mVisibleContainers.get(groupPosition).getAchievement(childPosition),
                        v.findViewById(R.id.achievement_reward));
            }
        });

    }

    private void initContainers(Context context) {
        mContainers = new ArrayList<>();
        PracticalRiddleType lastVisibleType = Riddle.getLastVisibleRiddleType(getContext());

        TestSubjectAchievementHolder holder = TestSubject.getInstance().getAchievementHolder();

        // DAILY
        addAchievementHolder(holder.getDailyAchievementHolder(), context.getResources().getString(R.string
                .achievement_daily_category_name), R.drawable.icon_daily);

        //TYPES
        for (TestSubjectRiddleType type : TestSubject.getInstance().getAvailableTypes()) {
            TypeAchievementHolder typeHolder = holder.getTypeAchievementHolder(type.getType());
            if (typeHolder != null) {
                HolderContainer container = addAchievementHolder(typeHolder, context.getResources()
                        .getString(type.getNameResId()), type.getIconResId());
                if (container != null && lastVisibleType != null && type.getType().equals
                        (lastVisibleType)) {
                    container.mInitiallyExpanded = true;
                }
            }
        }

        //MISC
        AchievementHolder misHolder = holder.getMiscAchievementHolder();
        addAchievementHolder(misHolder, context.getResources().getString(R.string.achievement_misc_category_name), R.drawable.icon_general);

        mVisibleContainers = new ArrayList<>(mContainers.size());
        refreshContainers();
    }

    private static class HolderContainer {
        final AchievementHolder mHolder;
        final String mCategoryName;
        final int mCategoryIconId;
        List<? extends Achievement> mDisplayedAchievements;
        boolean mInitiallyExpanded;

        HolderContainer(@NonNull AchievementHolder holder, String categoryName, int iconResId) {
            mHolder = holder;
            mCategoryName = categoryName;
            mCategoryIconId = iconResId;
        }

        void refresh() {
            mDisplayedAchievements = mHolder.getAchievements();
        }

        public boolean isVisible() {
            return mDisplayedAchievements != null && !mDisplayedAchievements.isEmpty();
        }

        public int getAchievementCount() {
            return mDisplayedAchievements == null ? 0 : mDisplayedAchievements.size();
        }

        public Achievement getAchievement(int index) {
            return mDisplayedAchievements.get(index);
        }
    }

    private HolderContainer addAchievementHolder(AchievementHolder holder, String name, int imageResId) {
        if (holder == null) {
            return null;
        }
        HolderContainer container = new HolderContainer(holder, name, imageResId);
        mContainers.add(container);
        return container;
    }

    private boolean claimReward(Achievement achievement, View toAnimate) {
        if (achievement.isRewardClaimable()) {
            achievement.claimReward();
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.reward_claimed);
            toAnimate.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void refresh(StoreActivity activity, FrameLayout titleBackContainer) {
        Log.d("Achievement", "Refreshing achievement view.");
        TestSubject.getInstance().getAchievementHolder().refresh();
        refreshContainers();
        if (mAdapter == null) {
            mAdapter = new AchievementsAdapter(activity);
            setGroupIndicator(getResources().getDrawable(R.drawable.achievement_list_group_indicator));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setIndicatorBoundsRelative(100, 150);
            } else {
                setIndicatorBounds(100, 150);
            }
            setAdapter(mAdapter);
            for (int i = 0; i < mVisibleContainers.size(); i++) {
                if (mVisibleContainers.get(i).mInitiallyExpanded) {
                    expandGroup(i);
                }
            }
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void refreshContainers() {
        mVisibleContainers.clear();
        for (HolderContainer container : mContainers) {
            container.refresh();
            if (container.isVisible()) {
                mVisibleContainers.add(container);
            }
        }
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {
    }

    @Override
    public View getView() {
        return this;
    }

    private class AchievementsAdapter extends BaseExpandableListAdapter {
        private final LayoutInflater mInflater;
        private Context mContext;

        public AchievementsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getGroupCount() {
            return mVisibleContainers.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return mVisibleContainers.get(i).getAchievementCount();
        }

        @Override
        public Object getGroup(int i) {
            return mVisibleContainers.get(i);
        }

        @Override
        public Object getChild(int i, int i1) {
            return mVisibleContainers.get(i).getAchievement(i1);
        }

        @Override
        public long getGroupId(int i) {
            return i + 1;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1 + 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View name = convertView == null ? null : convertView.findViewById(R.id.achievement_category_name);
            if (name == null) {
                convertView = mInflater.inflate(R.layout.achievement_category, null);
                name = convertView.findViewById(R.id.achievement_category_name);
            }
            ((TextView) name).setText(mVisibleContainers.get(groupPosition).mCategoryName);
            int imageResId = mVisibleContainers.get(groupPosition).mCategoryIconId;
            ((ImageView) convertView.findViewById(R.id.achievement_category_image))
                    .setImageResource(imageResId);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.achievement_element, null);
            }
            Achievement achievement = mVisibleContainers.get(groupPosition).getAchievement(childPosition);
            boolean dependenciesFulfilled = achievement.areDependenciesFulfilled();

            LinearLayoutProgressBar progressListener = (LinearLayoutProgressBar) convertView.findViewById(R.id.progress_bar);
            if (achievement.isAchieved()) {
                progressListener.onProgressUpdate(0);
                convertView.setBackgroundColor(progressListener.getStartColor());
            } else {
                convertView.setBackgroundColor(progressListener.getEndColor());
                progressListener.onProgressUpdate(achievement.getProgress());
            }

            ImageView image = (ImageView) convertView.findViewById(R.id.achievement_image);
            image.setImageResource(achievement.getIconResIdByState());

            TextView name = (TextView) convertView.findViewById(R.id.achievement_name);
            name.setText(achievement.getName(mContext.getResources()));

            TextView descr = (TextView) convertView.findViewById(R.id.achievement_descr);
            if (dependenciesFulfilled) {
                descr.setTextColor(Color.BLACK);
                if (achievement.isDiscovered()) {
                    descr.setText(achievement.getDescription(mContext.getResources()));
                } else {
                    descr.setText(R.string.achievement_descr_undiscovered);
                }
            } else {
                descr.setTextColor(Color.RED);
                descr.setText(achievement.buildDependenciesText(getResources()));
            }

            TextView reward = (TextView) convertView.findViewById(R.id.achievement_reward);
            reward.clearAnimation(); // as animation is not stopped when view gets pushed out of
            // screen and gets recycled
            reward.setText(achievement.getRewardDescription(mContext.getResources()));
            if (achievement.isRewardClaimable()) {
                reward.setBackgroundResource(R.drawable.star_background);
            } else {
                reward.setBackgroundResource(0);
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int childPosition) {
            Achievement achievement = mVisibleContainers.get(i).getAchievement(childPosition);
            return achievement.isRewardClaimable();
        }
    }
}
