package dan.dit.whatsthat.riddle.achievement;

import android.util.Log;

import dan.dit.whatsthat.achievement.AchievementDataTimer;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 20.05.15.
 */
public class AchievementDataTimerRiddle extends AchievementDataTimer {
    public static final String DATA_NAME = "riddletimer";
    private static volatile AchievementDataTimerRiddle INSTANCE;

    private AchievementDataTimerRiddle(AchievementManager manager) throws CompactedDataCorruptException {
        super(DATA_NAME, manager.loadDataEvent(DATA_NAME));
        manager.manageAchievementData(this);
    }

    private AchievementDataTimerRiddle(AchievementManager manager, boolean notUsed) {
        super(DATA_NAME);
        manager.manageAchievementData(this);
    }

    public static AchievementDataTimerRiddle getInstance(AchievementManager manager) {
        if (INSTANCE == null) {
            synchronized (AchievementDataTimerRiddle.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new AchievementDataTimerRiddle(manager);
                    } catch (CompactedDataCorruptException e) {
                        Log.e("Achievement", "Failed recreating data timer: " + e);
                        INSTANCE = new AchievementDataTimerRiddle(manager, true);
                    }
                }
            }
        }
        return INSTANCE;
    }

}
