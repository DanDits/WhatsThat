package dan.dit.whatsthat.achievement;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 12.05.15.
 */
public class AchievementProperties extends AchievementData {
    protected final Map<String, Long> mValues = new HashMap<>();
    private boolean mSilentChangeMode;

    public AchievementProperties(String name, Compacter data) throws CompactedDataCorruptException {
        super(name);
        unloadData(data);
    }

    /**
     * Enables or disables the silent change mode. This should be used when many changes that belong together
     * are done and if listeners should not be notified about every small change.
     * @param silentChanges True to enable silent changes. False to return to default mode that notifies
     *                      listeners about all future changes and potential changes during silent mode (so this will notify listeners).
     */
    protected void setSilentChangeMode(boolean silentChanges) {
        mSilentChangeMode = silentChanges;
        if (!silentChanges) {
            notifyListeners();
        }
    }

    public AchievementProperties(String name) {
        super(name);
    }

    @Override
    protected synchronized void resetData() {
        mValues.clear();
    }

    public synchronized void putValue(String key, Long value) {
        if (key == null) {
            return;
        }
        Long old;
        if (value == null) {
            old = mValues.remove(value);
        } else {
            old = mValues.put(key, value);
        }
        // if there previously was no value or the value changed, notify listeners
        if (!mSilentChangeMode && ((value != null && old == null) || (old != null && !old.equals(value)))) {
            notifyListeners();
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
        if (!mSilentChangeMode) {
            notifyListeners();
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
