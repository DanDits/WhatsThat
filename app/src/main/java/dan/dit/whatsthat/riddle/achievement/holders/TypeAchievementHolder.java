package dan.dit.whatsthat.riddle.achievement.holders;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.riddle.achievement.GameAchievement;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

import static java.util.Collections.EMPTY_LIST;

/**
 * Created by daniel on 15.06.15.
 */
public abstract class TypeAchievementHolder implements AchievementHolder {
    final PracticalRiddleType mType;
    Map<Integer, GameAchievement> mAchievements;

    TypeAchievementHolder(PracticalRiddleType type) {
        mType = type;
    }

    @Override
    public void addDependencies() {
        for (GameAchievement achievement : mAchievements.values()) {
            achievement.setDependencies();
        }
    }

    @Override
    public void initAchievements() {
        if (mAchievements == null) {
            Log.e("Achievement", "Trying to init achievements before creating them.");
            return;
        }
        for (GameAchievement achievement : mAchievements.values()) {
            achievement.init();
        }
    }

    public List<? extends Achievement> getAchievements() {
        if (mAchievements != null && !mAchievements.isEmpty()) {
            return new ArrayList<>(mAchievements.values());
        }
        return EMPTY_LIST;
    }

    public GameAchievement getByNumber(int number) {
        return mAchievements.get(number);
    }
}
