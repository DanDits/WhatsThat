package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.List;

import dan.dit.whatsthat.achievement.Achievement;
import dan.dit.whatsthat.achievement.AchievementManager;

/**
 * Created by daniel on 15.06.15.
 */
public interface AchievementHolder {
    void makeAchievements(AchievementManager manager);
    void addDependencies();
    void initAchievements();
    List<? extends Achievement> getAchievements();
}
