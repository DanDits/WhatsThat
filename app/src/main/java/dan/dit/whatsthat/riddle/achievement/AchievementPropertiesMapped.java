package dan.dit.whatsthat.riddle.achievement;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * A small extension to AchievementProperties that allows temporarily storing
 * values of a certain type associated to a key. This is useful for passing bigger information
 * than a long but the data is not stored permanently. Else this behaves exactly like an AchievementProperties.
 * Created by daniel on 29.06.15.
 */
public class AchievementPropertiesMapped<Type> extends AchievementProperties {
    private Map<String, Type> mMapped = new HashMap<>();

    public AchievementPropertiesMapped(String name, Compacter data) throws CompactedDataCorruptException {
        super(name, data);
    }

    public AchievementPropertiesMapped(String name) {
        super(name);
    }

    /**
     * Updates the mapped value for the given key and increments the
     * associated value for the key by one, notifying listeners if not in silent mode.
     * @param key The key for the data. Does nothing if null.
     * @param value The value for the mapping which can be retrieved when receiving the event.
     */
    public void updateMappedValue(String key, Type value) {
        if (key != null) {
            mMapped.put(key, value);
            increment(key, 1L, 0L);
        }
    }

    /**
     * Returns the mapped value for the given key if any.
     * @param key The valid key to retrieve data from.
     * @return The mapped data or null if there is no mapped data or null was mapped.
     */
    public Type getMappedValue(String key) {
        return mMapped.get(key);
    }

}
