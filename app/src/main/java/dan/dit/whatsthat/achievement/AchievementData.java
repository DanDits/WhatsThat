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

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dan.dit.whatsthat.util.compaction.Compactable;

/**
 * This class encapsulates data that is set and updated by the client and notifies
 * listeners like Achievements that are interested in the data changing to update their state.
 * The AchievementData can be compacted and restored. It is uniquely identified by a data name
 * and holds a list of listeners.
 * Created by daniel on 12.05.15.
 */
public abstract class AchievementData implements Compactable {
    private final List<AchievementDataEventListener> mListeners = new ArrayList<>();
    private List<AchievementDataEventListener> mAddedListeners = new ArrayList<>(4);
    private List<AchievementDataEventListener> mRemovedListeners = new ArrayList<>(4);
    protected final String mName;
    private int mIsProcessingEventDepth;
    private Queue<AchievementDataEvent> mEventFactory = new LinkedList<>();

    AchievementData(String dataName) {
        mName = dataName;
        if (TextUtils.isEmpty(mName)) {
            throw new IllegalArgumentException("Null name given for achievement data.");
        }
    }

    protected AchievementDataEvent obtainNewEvent() {
        if (mEventFactory == null || mEventFactory.isEmpty()) {
            return new AchievementDataEvent();
        } else {
            return mEventFactory.poll();
        }
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AchievementData) {
            return mName.equals(((AchievementData) other).mName);
        } else {
            return super.equals(other);
        }
    }

    /**
     * Resets the data. Does not touch the listeners.
     */
    protected abstract void resetData();

    /**
     * Adds a listener to be notified on future data events.
     * @param listener The listener to add if not yet added.
     */
    public void addListener(AchievementDataEventListener listener) {
        if (!mListeners.contains(listener) && !mAddedListeners.contains(listener)) {
            mAddedListeners.add(listener);
        }
    }

    /**
     * Removes a listener that will no longer be notified on future data events.
     * @param listener The listener to remove if not yet removed.
     * @return If the listener will be removed before next notification.
     */
    public boolean removeListener(AchievementDataEventListener listener) {
        if (mListeners.contains(listener) && !mRemovedListeners.contains(listener)) {
            mRemovedListeners.add(listener);
            return true;
        }
        return false;
    }

    /**
     * Notifies all associated listeners of the given event.
     * @param event The event to notify listeners of.
     */
    synchronized void notifyListeners(AchievementDataEvent event) {
        if (mIsProcessingEventDepth == 0) {
            for (int i = 0; i < mAddedListeners.size(); i++) {
                mListeners.add(mAddedListeners.get(i));
            }
            for (int i = 0; i < mRemovedListeners.size(); i++) {
                mListeners.remove(mRemovedListeners.get(i));
            }
            mAddedListeners.clear();
            mRemovedListeners.clear();
        }
        mIsProcessingEventDepth++;
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDataEvent(event);
        }
        mIsProcessingEventDepth--;
        mEventFactory.add(event);
    }
}
