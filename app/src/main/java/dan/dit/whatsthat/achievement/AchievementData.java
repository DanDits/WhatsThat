package dan.dit.whatsthat.achievement;

import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.compaction.Compactable;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class AchievementData implements Compactable {
    private final List<AchievementDataEventListener> mListeners = new LinkedList<>();
    private List<AchievementDataEventListener> mRemovedListeners = new LinkedList<>();
    protected final String mName;

    public AchievementData(String dataName) {
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

    protected abstract void resetData();

    public void addListener(AchievementDataEventListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public boolean removeListener(AchievementDataEventListener listener) {
        if (mListeners.contains(listener) && !mRemovedListeners.contains(listener)) {
            mRemovedListeners.add(listener);
            return true;
        }
        return false;
    }

    protected void notifyListeners(AchievementDataEvent event) {
        for (AchievementDataEventListener removed : mRemovedListeners) {
            mListeners.remove(removed);
        }
        mRemovedListeners.clear();
        for (AchievementDataEventListener listener : mListeners) {
            listener.onDataEvent(event);
        }
    }
}
