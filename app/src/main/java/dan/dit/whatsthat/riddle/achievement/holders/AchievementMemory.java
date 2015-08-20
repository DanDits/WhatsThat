package dan.dit.whatsthat.riddle.achievement.holders;

import java.util.TreeMap;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Created by daniel on 20.08.15.
 */
public class AchievementMemory extends TypeAchievementHolder {
    public static final String KEY_GAME_PATH_LENGTH = "path_length";
    public static final String KEY_GAME_STATE_RED_COUNT = "state_reds";
    public static final String KEY_GAME_STATE_YELLOW_COUNT = "state_yellow";
    public static final String KEY_GAME_STATE_GREEN_COUNT = "state_green";
    public static final String KEY_GAME_STATE_BLACK_COUNT = "state_black";
    public static final String KEY_GAME_UNCOVERED_PAIRS_COUNT = "uncovered_pairs";
    public static final String KEY_GAME_CARD_UNCOVERED_BY_CLICK_COUNT = "uncovered_by_click";

    public AchievementMemory(PracticalRiddleType type) {
        super(type);
    }

    @Override
    public void makeAchievements(AchievementManager manager) {
        mAchievements = new TreeMap<>();
    }

    //TODO implement (currently not possible and probably will not add: sensing one single card to go from black to green)
}
