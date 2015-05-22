package dan.dit.whatsthat.achievement;

import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class Achievement implements AchievementDataEventListener {
    public static final String SEPARATOR = "_"; // not allowed in achievement id or keys
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_ACHIEVED = "achieved";
    private static final String KEY_VALUE = "value";
    private static final String KEY_MAX_VALUE = "maxvalue";
    public static final boolean DEFAULT_IS_DISCOVERED = true;
    private static final int DEFAULT_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 0;

    protected final String mId;
    protected boolean mDiscovered;
    protected boolean mAchieved;
    protected final AchievementManager mManager;
    protected int mValue;
    protected int mMaxValue;

    public Achievement(String id, AchievementManager manager) {
        mId = id;
        mManager = manager;
        if (TextUtils.isEmpty(mId)) {
            throw new IllegalArgumentException("Null id given.");
        }
        if (mId.contains(SEPARATOR)) {
            throw new IllegalArgumentException("Separator contained in id " + id);
        }
        if (manager == null) {
            throw new IllegalArgumentException("Null manager given.");
        }
        loadData(manager.getSharedPreferences());
        onCreated();
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Achievement) {
            return mId.equals(((Achievement) other).mId);
        } else {
            return super.equals(other);
        }
    }

    protected abstract void onCreated();

    protected abstract void onInit();

    protected synchronized final void discover() {
        if (mDiscovered) {
            return; // already discovered
        }
        mDiscovered = true;
        onDiscovered();
        mManager.onChanged(this);
    }

    protected abstract void onDiscovered();

    protected synchronized final void achieve() {
        if (mAchieved) {
            return; // already achieved
        }
        if (!mDiscovered) {
            discover();
        }
        mAchieved = true;
        onAchieved();
        mManager.onChanged(this);
    }

    protected abstract void onAchieved();

    protected final void addData(SharedPreferences.Editor editor) {
        editor
                .putBoolean(mId + SEPARATOR + KEY_DISCOVERED, mDiscovered)
                .putBoolean(mId + SEPARATOR + KEY_ACHIEVED, mAchieved)
                .putInt(mId + SEPARATOR + KEY_VALUE, mValue)
                .putInt(mId + SEPARATOR + KEY_MAX_VALUE, mMaxValue);

    }

    private void loadData(SharedPreferences prefs) {
        mDiscovered = prefs.getBoolean(mId + SEPARATOR + KEY_DISCOVERED, DEFAULT_IS_DISCOVERED);
        mAchieved = prefs.getBoolean(mId + SEPARATOR + KEY_ACHIEVED, false); // any other default value would be kinda.. stupid
        mValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_VALUE);
        mMaxValue = prefs.getInt(mId + SEPARATOR + KEY_VALUE, DEFAULT_MAX_VALUE);
    }
}
