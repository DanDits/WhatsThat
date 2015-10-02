package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.TreeMap;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 02.10.15.
 */
public class AchievementLazor extends TypeAchievementHolder {
    public AchievementLazor(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
    }
}
