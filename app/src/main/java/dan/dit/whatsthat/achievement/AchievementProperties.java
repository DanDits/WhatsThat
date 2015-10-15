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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * A special AchievementData that maps keys to long values. Every
 * change will notify the associated listeners if no silent changes are currently
 * enabled and the update policy is met and an actual change happened.
 * Created by daniel on 12.05.15.
 */
public class AchievementProperties extends AchievementData {
    /**
     * The update policy that means that the data is always set, no matter the previous data.
     * This will still only notify listeners if the data changed.
     */
    public static final long UPDATE_POLICY_ALWAYS = 0L;

    private final Map<String, Long> mValues = new HashMap<>();
    private boolean mSilentChangeMode;
    private AchievementDataEvent mCurrEvent;

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
            mCurrEvent = obtainNewEvent();
            mCurrEvent.init(this, eventType, null);
        }
    }

    public void disableSilentChanges() {
        if (mSilentChangeMode) {
            mSilentChangeMode = false;
            AchievementDataEvent event = mCurrEvent;
            mCurrEvent = null;
            notifyListeners(event);
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
        notifyListeners(obtainNewEvent().initReset(this));
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
        if (mCurrEvent != null) {
            mCurrEvent.addChangedKey(key);
        }
        // if there previously was no value or the value changed, notify listeners
        if (!mSilentChangeMode && ((value != null && old == null) || (old != null && !old.equals(value)))) {
            AchievementDataEvent event = obtainNewEvent().init(this, AchievementDataEvent.EVENT_TYPE_DATA_UPDATE, key);
            notifyListeners(event);
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
        if (mCurrEvent != null) {
            mCurrEvent.addChangedKey(key);
        }
        if (!mSilentChangeMode) {
            AchievementDataEvent event = obtainNewEvent().init(this, AchievementDataEvent.EVENT_TYPE_DATA_UPDATE, key);
            notifyListeners(event);
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
