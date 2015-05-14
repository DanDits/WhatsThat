package dan.dit.whatsthat.achievement;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by daniel on 12.05.15.
 */
public class AchievementManager {

    private static final String ACHIEVEMENT_PREFERENCES_FILE = "dan.dit.whatsthat.achievement_data";
    private final Context mApplicationContext;
    private final SharedPreferences mPrefs;

    public AchievementManager(Context applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("No context given.");
        }
        mApplicationContext = applicationContext.getApplicationContext();
        mPrefs = mApplicationContext.getSharedPreferences(ACHIEVEMENT_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }


    protected void onChanged(Achievement achievement) {
        if (achievement != null) {
            achievement.save(mPrefs);
            // TODO display if achieved or updated progress
        }
    }

    public void manageAchievementData(AchievementData data) {
        // for achievementdata that is not managed by the using instance but rather left alone in its existance somewhere
        // this offers a general interface for saving and loading
        //TODO save the given data, make the manager a listener for given data, save data onEvent to xml, offer a way to load data for recreation
    }
}
