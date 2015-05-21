package dan.dit.whatsthat.achievement;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by daniel on 21.05.15.
 */
public class AchievementDataEvent {
    public static final int EVENT_TYPE_DATA_RESET = 0;
    public static final int EVENT_TYPE_DATA_CREATE = 1;
    public static final int EVENT_TYPE_DATA_UPDATE = 2;
    public static final int EVENT_TYPE_DATA_CLOSE = 3;

    private AchievementData mChangedData;
    private List<String> mChangedKeys = new LinkedList<>();
    private int mEventType;

    public void init(AchievementData changedData, int eventType, String changedKey) {
        mChangedData = changedData;
        mChangedKeys.clear();
        addChangedKey(changedKey);
        mEventType = eventType;
        if (changedData == null) {
            throw new IllegalArgumentException("Null changed data for event.");
        }
    }

    protected void addChangedKey(String key) {
        if (key != null) {
            mChangedKeys.add(key);
        }
    }

    public AchievementData getChangedData() {
        return mChangedData;
    }

    public int getEventType() {
        return mEventType;
    }

    public boolean hasChangedKey(String keySolved) {
        return keySolved != null && mChangedKeys.contains(keySolved);
    }
}
