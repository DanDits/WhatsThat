package dan.dit.whatsthat.achievement;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.05.15.
 */
public class AchievementManager implements AchievementDataEventListener {

    private static final String ACHIEVEMENT_PREFERENCES_FILE = "dan.dit.whatsthat.achievement_data";
    public static final int CHANGED_PROGRESS = 0;
    public static final int CHANGED_TO_COVERED = 1;
    public static final int CHANGED_TO_DISCOVERED = 2;
    public static final int CHANGED_TO_ACHIEVED = 3;
    public static final int CHANGED_GOT_CLAIMED = 4;
    private static AchievementManager INSTANCE = null;
    private final SharedPreferences mPrefs;
    private Set<AchievementData> mManagedChangedData;
    private Set<Achievement> mChangedAchievements;

    private AchievementManager(Context applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("No context given.");
        }
        applicationContext = applicationContext.getApplicationContext();
        mPrefs = applicationContext.getSharedPreferences(ACHIEVEMENT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mManagedChangedData = new HashSet<>();
        mChangedAchievements = new HashSet<>();
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

    //close manager at the end to commit changes to data and achievements
    public static synchronized void commit() {
        if (INSTANCE != null && (!INSTANCE.mManagedChangedData.isEmpty() || !INSTANCE.mChangedAchievements.isEmpty())) {
            SharedPreferences.Editor editor = INSTANCE.mPrefs.edit();
            for (AchievementData data : INSTANCE.mManagedChangedData) {
                editor.putString(data.mName, data.compact());
            }
            for (Achievement achievement : INSTANCE.mChangedAchievements) {
                achievement.addData(editor);
            }
            editor.apply();
            Log.d("Achievement", "Commiting achievement manager, saving " + INSTANCE.mChangedAchievements.size() + " changed achievements.");
            INSTANCE.mManagedChangedData.clear();
            INSTANCE.mChangedAchievements.clear();
        }
    }

    protected void onChanged(Achievement achievement, int changedHint) {
        if (achievement != null) {
            mChangedAchievements.add(achievement);
            Log.d("Achievement", "Achievemenet on changed: " + changedHint + ": " + achievement);
            if (changedHint == CHANGED_TO_ACHIEVED) {
                TestSubject.getInstance().postAchievementAchieved(achievement);
            }
        }
    }

    public void manageAchievementData(AchievementData data) {
        // for achievement data that is not managed by the using instance but rather left alone in its existence somewhere
        // this offers a general interface for saving and loading
        data.addListener(this);
    }

    @Override
    public void onDataEvent(AchievementDataEvent event) {
        mManagedChangedData.add(event.getChangedData());
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

    protected SharedPreferences getSharedPreferences() {
        return mPrefs;
    }

    public static boolean hasInstance() {
        return INSTANCE != null;
    }
}
