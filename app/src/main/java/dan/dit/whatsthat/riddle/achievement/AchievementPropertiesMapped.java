/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
