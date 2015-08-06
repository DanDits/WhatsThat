package dan.dit.whatsthat.achievement;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.05.15.
 */
public class AchievementProperties extends AchievementData {
    public static final long UPDATE_POLICY_ALWAYS = 0L;

    private final Map<String, Long> mValues = new HashMap<>();
    private boolean mSilentChangeMode;
    private AchievementDataEvent mEvent = new AchievementDataEvent();

    protected AchievementProperties(String name, Compacter data) throws CompactedDataCorruptException {
        super(name);
        unloadData(data);
    }

    protected boolean isSilentChangeMode() {
        return mSilentChangeMode;
    }

    public void enableSilentChanges(int eventType) {
        if (!mSilentChangeMode) {
            mSilentChangeMode = true;
            mEvent.init(this, eventType, null);
        }
    }

    public void disableSilentChanges() {
        if (mSilentChangeMode) {
            mSilentChangeMode = false;
            notifyListeners(mEvent);
        }
    }

    protected AchievementProperties(String name) {
        super(name);
    }

    public boolean removeKey(String key) {
        return key != null && mValues.remove(key) != null;
    }

    @Override
    protected synchronized void resetData() {
        mValues.clear();
    }

    public synchronized void putValue(String key, Long value, long requiredValueToOldDelta) {
        if (key == null) {
            return;
        }
        Long old;
        if (value == null) {
            old = mValues.remove(key);
        } else {
            old = mValues.put(key, value);
            if (old != null) {
                if (requiredValueToOldDelta > 0L) {
                    if (value - old < requiredValueToOldDelta) {
                        value = old;
                        mValues.put(key, value);
                    }
                } else if (requiredValueToOldDelta < 0L) {
                    if (value - old > requiredValueToOldDelta) {
                        value = old;
                        mValues.put(key, value);
                    }
                }
            }
        }
        mEvent.addChangedKey(key);
        // if there previously was no value or the value changed, notify listeners
        if (!mSilentChangeMode && ((value != null && old == null) || (old != null && !old.equals(value)))) {
            mEvent.init(this, AchievementDataEvent.EVENT_TYPE_DATA_UPDATE, key);
            notifyListeners(mEvent);
        }
    }

    public synchronized Long increment(String key, long delta, long baseValue) {
        if (key == null) {
            return null;
        }
        Long value = mValues.get(key);
        if (value == null) {
            value = baseValue + delta;
        } else {
            value += delta;
        }
        mValues.put(key, value);
        mEvent.addChangedKey(key);
        if (!mSilentChangeMode) {
            mEvent.init(this, AchievementDataEvent.EVENT_TYPE_DATA_UPDATE, key);
            notifyListeners(mEvent);
        }
        return value;
    }

    public synchronized Long getValue(String key, long defaultValue) {
        Long value = mValues.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    @Override
    public String compact() {
        Compacter cmp = new Compacter(mValues.size() * 2);
        for (String key : mValues.keySet()) {
            cmp.appendData(key);
            cmp.appendData(mValues.get(key));
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null) {
            return;
        }
        for (int i = 0; i + 1 < compactedData.getSize(); i+=2) {
            mValues.put(compactedData.getData(i), compactedData.getLong(i + 1));
        }
    }

    @Override
    public String toString() {
        return mValues.toString();
    }
}
