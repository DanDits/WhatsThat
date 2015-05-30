package dan.dit.whatsthat.achievement;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * A class that times data that an Achievement received. Data is kept in a TimeKeeper and can
 * be accessed by a unique (for an AchievementDataTimer instance) key. A TimeKeeper holds timestamps
 * that mark the system times when the TimeKeeper was updated and can hold a duration supplied by the client.
 * The amount of these timestamps and durations hold is fixed and the oldest one will be discarded if
 * new ones are added and the TimeKeeper is already full.<br>
 *     So this can be used to remember certain timestamps (like completion) for events that happened. Additionally you can
 *     remember the durations for the events and finally check if a certain amount of these events were within a defined
 *     time span. The meaning of "within" is here by defined differently, as the maximum spent time or only
 *     the durations for (some) events might be of interest.
 * Created by daniel on 14.05.15.
 */
public class AchievementDataTimer extends AchievementData {
    private Map<String, TimeKeeper> mTimeKeepers = new HashMap<>();
    protected AchievementDataEvent mEvent = new AchievementDataEvent();

    /**
     * Loads an AchievementDataTimer with the given data name.
     * @param dataName The AchievementData name.
     * @param compactedData The comapted data. Can be null to create a new instance.
     * @throws CompactedDataCorruptException If there was an error reading the data.
     */
    public AchievementDataTimer(String dataName, Compacter compactedData) throws CompactedDataCorruptException {
        super(dataName);
        unloadData(compactedData);
    }

    /**
     * Creates a new AchievementDataTimer with the given data name.
     * @param dataName The AchievementData name.
     */
    public AchievementDataTimer(String dataName) {
        super(dataName);
    }

    @Override
    protected synchronized void resetData() {
        mTimeKeepers.clear();
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter(mTimeKeepers.size());
        for (String key : mTimeKeepers.keySet()) {
            cmp.appendData(mTimeKeepers.get(key).compact());
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null) {
            return;
        }
        for (int i = 0; i < compactedData.getSize(); i++) {
            TimeKeeper timestamp = null;
            try {
                timestamp = new TimeKeeper(new Compacter(compactedData.getData(i)));
            } catch (CompactedDataCorruptException e) {
                Log.e("Achievement", "Compacted timestamp corrupt: " + e);
            }
            if (timestamp != null) {
                mTimeKeepers.put(timestamp.mKey, timestamp);
            }
        }
    }

    /**
     * Creates a new TimeKeeper for a key with a fixed amount of timestamps it will hold.
     * Does nothing if a parameter is illegal.
     * @param key The key for the new TimeKeeper.
     * @param amount The positive amount of timestamps to hold.
     */
    public synchronized void newTimeKeeper(String key, int amount) {
        if (key == null || amount <= 0) {
            return;
        }
        mTimeKeepers.put(key, new TimeKeeper(key, amount));
    }

    /**
     * Ensures that there is a TimeKeeper for the given key with the given amount. Will not create a
     * new TimeKeeper if there is one already with the same amount.
     * @param key The key for the TimeKeeper.
     * @param amount The positive amount of timestamps to hold.
     * @return True only if a new TimeKeeper was created.
     */
    public synchronized boolean ensureTimeKeeper(String key, int amount) {
        if (key == null || amount <= 0) {
            return false;
        }
        if (!mTimeKeepers.containsKey(key) || mTimeKeepers.get(key).mTimestamps.length != amount) {
            newTimeKeeper(key, amount);
            return true;
        }
        return false;
    }

    /**
     * Removes the TimeKeeper for a key. Does nothing if key illegal or no TimeKeeper with
     * this key available.
     * @param key The key to remove the TimeKeeper for.
     */
    public void removeTimerKeeper(String key) {
        if (key == null) {
            return;
        }
        mTimeKeepers.remove(key);
    }

    /**
     * The TimeKeeper will be notified to update, using the current execution time
     * as a timestamp and the given duration as the a duration.
     * @param key The key to identify the TimeKeeper. Does nothing if key illegal or no TimeKeeper found.
     * @param duration The duration for this timed data. Will use 0 if negative.
     */
    public synchronized void onTimeKeeperUpdate(String key, long duration) {
        if (key == null) {
            return;
        }
        TimeKeeper timeKeeper = mTimeKeepers.get(key);
        if (timeKeeper != null) {
            timeKeeper.update(duration);
            mEvent.init(this, AchievementDataEvent.EVENT_TYPE_DATA_UPDATE, key);
            notifyListeners(mEvent);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{!");
        for (String key : mTimeKeepers.keySet()) {
            builder.append(mTimeKeepers.get(key).toString()).append("!");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Returns the TimeKeeper identified by the given key.
     * @param key The key for the TimeKeeper.
     * @return The TimeKeeper.
     */
    public TimeKeeper getTimeKeeper(String key) {
        if (key == null) {
            return null;
        }
        return mTimeKeepers.get(key);
    }

    /**
     * The TimeKeeper class managed by the AchievementDataTimer.
     */
    public static class TimeKeeper implements Compactable {
        private long[] mTimestamps;
        private long[] mDuration;
        private String mKey;
        private int mCurrIndex;

        private TimeKeeper(String key, int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Illegal count: " + count);
            }
            if (key == null) {
                throw new IllegalArgumentException("Illegal key: " + key);
            }
            mKey = key;
            mTimestamps = new long[count];
            mDuration = new long[count];
            mCurrIndex = 0;
        }

        private TimeKeeper(Compacter data) throws CompactedDataCorruptException {
            unloadData(data);
            Log.d("Achievement", "Reconstructed timekeeper: " + this);
        }

        @Override
        public String compact() {
            Compacter cmp = new Compacter(1 + 2 * mTimestamps.length);
            cmp.appendData(mKey);
            cmp.appendData(mTimestamps.length);
            for (int i = 0; i < mTimestamps.length; i++) {
                cmp.appendData(mTimestamps[i]);
                cmp.appendData(mDuration[i]);
            }
            return cmp.compact();
        }

        @Override
        public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
            if (compactedData == null || compactedData.getSize() < 3) {
                throw new CompactedDataCorruptException("Timestamp data missing").setCorruptData(compactedData);
            }
            mKey = compactedData.getData(0);
            int length = compactedData.getInt(1);
            if (length <= 0) {
                throw new CompactedDataCorruptException("Null or negative length.").setCorruptData(compactedData);
            }
            mTimestamps = new long[length];
            mDuration = new long[length];
            for (int i = 0; i < length && 2 + 2 * i + 1 < compactedData.getSize(); i++) {
                mTimestamps[i] = compactedData.getLong(2 + 2 * i);
                mDuration[i] = compactedData.getLong(2 + 2 * i + 1);
                mCurrIndex = i;
            }
            mCurrIndex++;
        }

        @Override
        public String toString() {
            return mKey + ": " + Arrays.toString(mTimestamps) + "|" + Arrays.toString(mDuration);
        }

        private void update(long duration) {
            if (mCurrIndex == mTimestamps.length) {
                // end reached, rotate data left
                for (int i = 1; i < mTimestamps.length; i++) {
                    mTimestamps[i - 1] = mTimestamps[i];
                    mDuration[i - 1] = mDuration[i];
                }
                mCurrIndex--;
            }
            if (mCurrIndex < mTimestamps.length) {
                // more space to add data
                mTimestamps[mCurrIndex] = System.currentTimeMillis();
                mDuration[mCurrIndex] = duration >= 0L ? duration : 0L;
            }
            mCurrIndex++;
        }

        /**
         * Returns the sum of all provided durations, ignoring the timestamps.
         * @return The sum of all durations.
         */
        public long sumDurations() {
            long duration = 0L;
            for (int i = 0; i < mDuration.length; i++) {
                duration += mDuration[i];
            }
            return duration;
        }

        /**
         * Returns the total time consumed by this TimeKeeper. This is defined
         * as the sum of the greater value of the times between two timestamps and the duration for
         * the event. So this is useful if the events are not strictly sequential but the durations
         * can overlap the timestamps (for example when timestamps mark completion points and multiple
         * data events can be started at the same time).
         * @return The total time consumed for the events.
         */
        public long getTotalTimeConsumed() {
            long time = 0L;
            for (int i = 0; i < mDuration.length; i++) {
                if (i == 0) {
                    time += mDuration[i];
                } else {
                    time += Math.max(mDuration[i], mTimestamps[i] - mTimestamps[i - 1]);
                }
            }
            return time;
        }

        /**
         * Returns the amount of event times saved by the TimeKeeper. Is smaller
         * than the initial capacity and greater than or equal to zero.
         * @return The amount of update events received.
         */
        public int getTimesCount() {
            return mCurrIndex;
        }
    }
}
