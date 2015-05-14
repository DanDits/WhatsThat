package dan.dit.whatsthat.achievement;

import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.util.compaction.Compactable;

/**
 * Created by daniel on 12.05.15.
 */
public abstract class AchievementData implements Compactable {
    protected final List<AchievementDataEventListener> mListeners = new LinkedList<>();
    private final String mName;

    public AchievementData(String dataName) {
        mName = dataName;
        if (TextUtils.isEmpty(mName)) {
            throw new IllegalArgumentException("Null name given for achievement data.");
        }
    }

    protected abstract void resetData();

    public void addListener(AchievementDataEventListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public boolean removeListener(AchievementDataEventListener listener) {
        return mListeners.remove(listener);
    }

    protected void notifyListeners() {
        for (AchievementDataEventListener listener : mListeners) {
            listener.onDataEvent(this);
        }
    }
}
