package dan.dit.whatsthat.achievement;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

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
    private boolean mIsProcessingEvent;

    AchievementData(String dataName) {
        mName = dataName;
        if (TextUtils.isEmpty(mName)) {
            throw new IllegalArgumentException("Null name given for achievement data.");
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
        if (!mIsProcessingEvent) {
            for (int i = 0; i < mAddedListeners.size(); i++) {
                mListeners.add(mAddedListeners.get(i));
            }
            for (int i = 0; i < mRemovedListeners.size(); i++) {
                mListeners.remove(mRemovedListeners.get(i));
            }
            mAddedListeners.clear();
            mRemovedListeners.clear();
        }
        mIsProcessingEvent = true;
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDataEvent(event);
        }
        mIsProcessingEvent = false;
    }
}
