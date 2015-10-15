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

package dan.dit.whatsthat.achievement;

import java.util.ArrayList;
import java.util.List;

/**
 * An event that is handed to AchievementDataEventListeners when something
 * happens with the AchievementData. The event type, the changed data and potentially changed
 * keys can be retrieved off the event. Data actual data is hold by the AchievementData.
 * Created by daniel on 21.05.15.
 */
public class AchievementDataEvent {
    /**
     * Event type that is triggered when the AchievementData is reset and its values therefore no longer
     * valid.
     */
    public static final int EVENT_TYPE_DATA_RESET = 0;

    /**
     * Event type that is triggered when the data was created.
     */
    public static final int EVENT_TYPE_DATA_CREATE = 1;

    /**
     * Event type that is triggered when data is updated.
     */
    public static final int EVENT_TYPE_DATA_UPDATE = 2;

    /**
     * Event type that is triggered when data is closed but still valid. No future updates are to be expected before it
     * is re created.
     */
    public static final int EVENT_TYPE_DATA_CLOSE = 3;

    private AchievementData mChangedData;
    private List<String> mChangedKeys = new ArrayList<>(5);
    private int mEventType;

    @Override
    public String toString() {
        return mEventType + ", " + mChangedData + ", " + mChangedKeys;
    }
    /**
     * Initializes this event with the given AchievementData, type and a single valid changed key.
     * @param changedData The changing data.
     * @param eventType The type of the event.
     * @param changedKey An optional key of data that changed in the AchievementData.
     * @return This event.
     */
    public AchievementDataEvent init(AchievementData changedData, int eventType, String changedKey) {
        mChangedData = changedData;
        mChangedKeys.clear();
        addChangedKey(changedKey);
        mEventType = eventType;
        if (changedData == null) {
            throw new IllegalArgumentException("Null changed data for event.");
        }
        return this;
    }

    /**
     * Initializes this event with the given AchievementData that just reset its data.
     * @param changedData The data that got reset.
     * @return This event.
     */
    public AchievementDataEvent initReset(AchievementData changedData) {
        return init(changedData, EVENT_TYPE_DATA_RESET, null);
    }

    /**
     * Adds the given key to the list of changed keys.
     * @param key The key to add. Does nothing if null.
     */
    void addChangedKey(String key) {
        if (key != null) {
            mChangedKeys.add(key);
        }
    }

    /**
     * The changed data.
     * @return The changed data.
     */
    public AchievementData getChangedData() {
        return mChangedData;
    }

    /**
     * The event type.
     * @return The event type.
     */
    public int getEventType() {
        return mEventType;
    }

    /**
     * Checks the list of changed keys if the given key is not null and contained.
     * @param keySolved The key to check.
     * @return If the given data associated with the given key changed in the AchievementData.
     */
    public boolean hasChangedKey(String keySolved) {
        return keySolved != null && mChangedKeys.contains(keySolved);
    }
}
