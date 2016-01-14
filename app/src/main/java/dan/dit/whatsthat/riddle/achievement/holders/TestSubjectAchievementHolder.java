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

package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.general.ObserverController;

/**
 * Created by daniel on 21.05.15.
 */
public class TestSubjectAchievementHolder implements AchievementHolder {

    private final Map<PracticalRiddleType, TypeAchievementHolder> mHolders = new HashMap<>();
    private MiscAchievementHolder mMiscHolder;
    private DailyAchievementsHolder mDailyHolder;
    private final List<AchievementHolder> mAllHolders = new ArrayList<>();
    private ObserverController<UnclaimedAchievementsCountListener, Void>
            mUnclaimedObserverController = new ObserverController<>();

    public boolean refresh() {
        return mDailyHolder.refresh();
    }

    /**
     * The interface for listening to the amount of unclaimed achievements. The event parameter
     * passed is null.
     */
    public interface UnclaimedAchievementsCountListener extends ObserverController
            .Observer<Void> {
        // for naming and simplification only, no more methods required
    }

    public TestSubjectAchievementHolder(AchievementManager manager) {
        makeAchievements(manager);
        manager.addAchievementChangedListener(new AchievementManager.OnAchievementChangedListener() {
            @Override
            public void onDataEvent(Integer changedHint) {
                switch (changedHint) {
                    case AchievementManager.CHANGED_GOT_CLAIMED: // fall through
                    case AchievementManager.CHANGED_TO_RESET: // fall through
                    case AchievementManager.CHANGED_TO_ACHIEVED_AND_UNCLAIMED:
                        mUnclaimedObserverController.notifyObservers(null);
                }
            }
        });
    }

    private boolean manageHolder(AchievementManager manager, AchievementHolder holder) {
        if (holder == null) {
            return false;
        }
        mAllHolders.add(holder);
        holder.makeAchievements(manager);
        return true;
    }

    public TypeAchievementHolder getTypeAchievementHolder(PracticalRiddleType type) {
        return mHolders.get(type);
    }

    public AchievementHolder getMiscAchievementHolder() {
        return mMiscHolder;
    }

    public AchievementHolder getDailyAchievementHolder() {
        return mDailyHolder;
    }

    public AchievementPropertiesMapped<String> getMiscData() {
        return mMiscHolder == null ? null : mMiscHolder.getData();
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mMiscHolder = new MiscAchievementHolder();
        manageHolder(manager, mMiscHolder);

        for (PracticalRiddleType type : PracticalRiddleType.getAll()) {
            TypeAchievementHolder holder = type.getAchievementHolder();
            if (manageHolder(manager, holder)) {
                mHolders.put(type, holder);
            }
        }

        mDailyHolder = new DailyAchievementsHolder();
        manageHolder(manager, mDailyHolder);
    }

    @Override
    public void addDependencies() {
        for (AchievementHolder holder : mAllHolders) {
            holder.addDependencies();
        }
    }

    @Override
    public void initAchievements() {
        for (AchievementHolder holder : mAllHolders) {
            holder.initAchievements();
        }
    }

    @Override
    public List<Achievement> getAchievements() {
        List<Achievement> achievements = new ArrayList<>();
        for (AchievementHolder holder : mAllHolders) {
            achievements.addAll(holder.getAchievements());
        }
        return achievements;
    }

    @Override
    public int getExpectableTestSubjectScore(int testSubjectLevel) {
        int expectedResult = 0;
        for (AchievementHolder holder : mAllHolders) {
            expectedResult += holder.getExpectableTestSubjectScore(testSubjectLevel);
        }
        return expectedResult;
    }

    public List<AchievementHolder> getHolders() {
        return mAllHolders;
    }

    public void addUnclaimedAchievementsCountListener(UnclaimedAchievementsCountListener
                                                              listener) {
        mUnclaimedObserverController.addObserver(listener);
    }

    public int getUnclaimedAchievementsCount() {
        int unclaimed = 0;
        for (AchievementHolder holder : mAllHolders) {
            for (Achievement achievement : holder.getAchievements()) {
                if (achievement.isRewardClaimable()) {
                    unclaimed++;
                }
            }
        }
        return unclaimed;
    }

    public void removeUnclaimedAchievementsCountListener(UnclaimedAchievementsCountListener listener) {
        mUnclaimedObserverController.removeObserver(listener);
    }
}
