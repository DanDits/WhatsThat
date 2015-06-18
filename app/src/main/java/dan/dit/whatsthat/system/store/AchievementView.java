package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectRiddleType;
import dan.dit.whatsthat.util.ui.LinearLayoutProgressBar;

/**
 * Created by daniel on 16.06.15.
 */
public class AchievementView extends ExpandableListView implements StoreContainer {

    private BaseExpandableListAdapter mAdapter;
    private List<List<Achievement>> mAllAchievements;
    private List<String> mCategoryNames;
    private List<Integer> mCategoryImage;
    private Button mTitleBackButton;

    public AchievementView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAllAchievements = new ArrayList<>();
        mCategoryNames = new ArrayList<>();
        mCategoryImage = new ArrayList<>();
        TestSubjectAchievementHolder holder = TestSubject.getInstance().getAchievementHolder();
        for (TestSubjectRiddleType type : TestSubject.getInstance().getAvailableTypes()) {
            List<Achievement> achievements = holder.getTypeAchievements(type.getType());
            if (achievements != null) {
                mAllAchievements.add(achievements);
                mCategoryNames.add(context.getResources().getString(type.getNameResId()));
                mCategoryImage.add(type.getIconResId());
            }
        }

        setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                return claimReward(mAllAchievements.get(groupPosition).get(childPosition), v.findViewById(R.id.achievement_reward));
            }
        });
    }

    private void updateTitleBackButton() {
        mTitleBackButton.setText(getContext().getString(R.string.store_category_achievement, TestSubject.getInstance().getAchievementScore()));
    }

    private boolean claimReward(Achievement achievement, View toAnimate) {
        if (achievement.isRewardClaimable()) {
            int claimedScore = achievement.claimReward();
            TestSubject.getInstance().addAchievementScore(claimedScore);
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.reward_claimed);
            toAnimate.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateTitleBackButton();
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
    public void refresh(FragmentActivity activity, Button titleBackButton) {
        mTitleBackButton = titleBackButton;
        updateTitleBackButton();
        if (mAdapter == null) {
            mAdapter = new AchievementsAdapter(activity);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setIndicatorBoundsRelative(100, 150);
            } else {
                setIndicatorBounds(100, 150);
            }
            setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void stop(FragmentActivity activity) {
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
            return mCategoryNames.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return mAllAchievements.get(i).size();
        }

        @Override
        public Object getGroup(int i) {
            return mCategoryNames.get(i);
        }

        @Override
        public Object getChild(int i, int i1) {
            return mAllAchievements.get(i).get(i1);
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
            ((TextView) name).setText(mCategoryNames.get(groupPosition));
            int imageResId = mCategoryImage.get(groupPosition);
            if (imageResId != 0) {
                ((ImageView) convertView.findViewById(R.id.achievement_category_image)).setImageResource(imageResId);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.achievement_element, null);
            }
            Achievement achievement = mAllAchievements.get(groupPosition).get(childPosition);
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
            image.setImageResource(achievement.getIconResId());

            TextView name = (TextView) convertView.findViewById(R.id.achievement_name);
            if (achievement.isDiscovered()) {
                name.setText(achievement.getName(mContext.getResources()));
            } else {
                name.setText(R.string.achievement_name_undiscovered);
            }

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
                descr.setText("Depends on things."); //TODO some better text here
            }

            TextView reward = (TextView) convertView.findViewById(R.id.achievement_reward);
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
            Achievement achievement = mAllAchievements.get(i).get(childPosition);
            return achievement.isRewardClaimable();
        }
    }
}
