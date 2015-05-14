package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener {
    private static final String KEY_DISCOVERED = "_discovered";
    private static final String KEY_ACHIEVED = "_achieved";
    private static final String KEY_VALUE = "_value";
    private static final String KEY_MAX_VALUE = "_max_value";

    protected final String mId;
    protected boolean mDiscovered;
    protected boolean mAchieved;
    private AchievementManager mManager;
    protected int mValue;
    protected int mMaxValue;

    public Achievement(String id, AchievementManager manager) {
        mId = id;
        mManager = manager;
        if (TextUtils.isEmpty(mId)) {
            throw new IllegalArgumentException("Null id given.");
        }
        if (manager == null) {
            throw new IllegalArgumentException("Null managet given.");
        }
    }

    public abstract void initEvents();

    protected final void discover() {
        if (mDiscovered) {
            return; // already discovered
        }
        mDiscovered = true;
        onDiscovered();
        mManager.onChanged(this);
    }

    protected abstract void onDiscovered();

    protected final void achieve() {
        if (mAchieved) {
            return; // already achieved
        }
        mAchieved = true;
        onAchieved();
        mManager.onChanged(this);
    }

    protected abstract void onAchieved();

    public void save(SharedPreferences prefs) {
        prefs.edit()
                .putBoolean(mId + KEY_DISCOVERED, mDiscovered)
                .putBoolean(mId + KEY_ACHIEVED, mAchieved)
                .putInt(mId + KEY_VALUE, mValue)
                .putInt(mId + KEY_MAX_VALUE, mMaxValue).apply();

    }
}
