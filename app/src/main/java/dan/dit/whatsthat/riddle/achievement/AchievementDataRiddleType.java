package dan.dit.whatsthat.riddle.achievement;

import android.util.Log;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 18.05.15.
 */
public class AchievementDataRiddleType extends AchievementProperties {
    private static final String DATA_NAME_PREFIX = "riddletype";
    private static final String KEY_NEW_GAMES_STARTED = "new_games_started";
    public static final String KEY_GAMES_SOLVED = "games_solved";
    public static final String KEY_BEST_SOLVED_TIME = "best_solved_time";

    public AchievementDataRiddleType(PracticalRiddleType type, AchievementManager manager) throws CompactedDataCorruptException {
        super(DATA_NAME_PREFIX + type.getFullName(), manager.loadDataEvent(DATA_NAME_PREFIX + type.getFullName()));
        manager.manageAchievementData(this);
    }

    public AchievementDataRiddleType(PracticalRiddleType type, AchievementManager manager, boolean notUsed) {
        super(DATA_NAME_PREFIX + type.getFullName());
        manager.manageAchievementData(this);
    }

    public void onNewGame() {
        Long newValue = increment(KEY_NEW_GAMES_STARTED, 1, 0);
        Log.d("Achievement", "OnNewGame: " + mName + ", count=" + newValue);
    }

    public void onSolvedGame() {
        Long newValue = increment(KEY_GAMES_SOLVED, 1, 0);
        Log.d("Achievement", "onSolvedGame: " + mName + ", count=" + newValue);
    }

}
