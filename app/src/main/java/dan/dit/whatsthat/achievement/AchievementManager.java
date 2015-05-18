package dan.dit.whatsthat.achievement;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.05.15.
 */
public class AchievementManager implements AchievementDataEventListener {

    private static final String ACHIEVEMENT_PREFERENCES_FILE = "dan.dit.whatsthat.achievement_data";
    private static AchievementManager INSTANCE = null;
    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mDataEditor;

    private AchievementManager(Context applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("No context given.");
        }
        applicationContext = applicationContext.getApplicationContext();
        mPrefs = applicationContext.getSharedPreferences(ACHIEVEMENT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mDataEditor = mPrefs.edit();
    }

    public static void initialize(Context applicationContext) {
        INSTANCE = new AchievementManager(applicationContext);
    }

    public static AchievementManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AchievementManager not yet initialized!");
        }
        return INSTANCE;
    }

    //close manager at the end to commit changes to data
    public static void close() {
        if (INSTANCE != null) {
            INSTANCE.mDataEditor.apply();
        }
    }

    protected void onChanged(Achievement achievement) {
        if (achievement != null) {
            achievement.save(mPrefs);
            // TODO display if achieved or updated progress
        }
    }

    public void manageAchievementData(AchievementData data) {
        // for achievement data that is not managed by the using instance but rather left alone in its existence somewhere
        // this offers a general interface for saving and loading
        data.addListener(this);
    }

    @Override
    public void onDataEvent(AchievementData changedData) {
        if (changedData != null) {
            Log.d("Achievement", "OnDataEvent in manager. Saving data of " + changedData.mName + " to xml (expensive compacting).");
            mDataEditor.putString(changedData.mName, changedData.compact());
        }
    }

    public Compacter loadDataEvent(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String data = mPrefs.getString(name, null);
        if (data == null) {
            return null;
        }
        return new Compacter(data);
    }
}
