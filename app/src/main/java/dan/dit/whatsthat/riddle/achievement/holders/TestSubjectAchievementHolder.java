package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.achievement.AchievementPropertiesMapped;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 21.05.15.
 */
public class TestSubjectAchievementHolder implements AchievementHolder {

    private Map<PracticalRiddleType, TypeAchievementHolder> mHolders = new HashMap<>();
    private MiscAchievementHolder mMiscHolder;

    public TestSubjectAchievementHolder(AchievementManager manager) {
        makeAchievements(manager);
    }

    public TypeAchievementHolder getTypeAchievementHolder(PracticalRiddleType type) {
        return mHolders.get(type);
    }

    public AchievementHolder getMiscAchievementHolder() {
        return mMiscHolder;
    }

    public AchievementPropertiesMapped<String> getMiscData() {
        return mMiscHolder == null ? null : mMiscHolder.getData();
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mMiscHolder = new MiscAchievementHolder();
        mMiscHolder.makeAchievements(manager);
        for (PracticalRiddleType type : PracticalRiddleType.getAll()) {
            TypeAchievementHolder holder = type.getAchievementHolder();
            if (holder != null) {
                holder.makeAchievements(manager);
                mHolders.put(type, holder);
            }
        }
    }

    @Override
    public void addDependencies() {
        mMiscHolder.addDependencies();
        for (TypeAchievementHolder holder : mHolders.values()) {
            holder.addDependencies();
        }
    }

    @Override
    public void initAchievements() {
        mMiscHolder.initAchievements();
        for (TypeAchievementHolder holder : mHolders.values()) {
            holder.initAchievements();
        }
    }

    @Override
    public List<Achievement> getAchievements() {
        List<Achievement> achievements = new ArrayList<>();
        achievements.addAll(mMiscHolder.getAchievements());
        for (TypeAchievementHolder holder : mHolders.values()) {
            achievements.addAll(holder.getAchievements());
        }
        return achievements;
    }

}
