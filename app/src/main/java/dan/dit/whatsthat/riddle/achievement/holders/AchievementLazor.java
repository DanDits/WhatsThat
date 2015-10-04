package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.TreeMap;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 02.10.15.
 */
public class AchievementLazor extends TypeAchievementHolder {
    public static final String KEY_GAME_IS_PROTECTED = "city_is_protected";
    public static final String KEY_GAME_LAST_LAZOR_METEOR_DESTROYED_COUNT = "last_lazor_meteor_destroyed_count";
    public static final String KEY_GAME_LAST_METEOR_DESTROYED_COLOR_TYPE = "last_meteor_color_type";
    public static final String KEY_GAME_METEOR_DESTROYED_COUNT = "meteor_destroyed_count";
    public static final String KEY_GAME_METEOR_CRASHED_IN_CITY_COUNT = "meteor_crashed_in_city_count";
    public static final String KEY_GAME_METEOR_CRASHED_NOT_CITY_COUNT = "meteor_crashed_not_city_count";
    public static final String KEY_GAME_DIFFICULTY = "curr_difficulty";

    public AchievementLazor(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
    }
}
