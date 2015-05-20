package dan.dit.whatsthat.achievement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 14.05.15.
 */
public class AchievementDataTimer extends AchievementData {
    private Map<String, long[]> mTimedData = new HashMap<>();

    public AchievementDataTimer(String dataName, Compacter compactedData) throws CompactedDataCorruptException {
        super(dataName);
        unloadData(compactedData);
    }

    public AchievementDataTimer(String dataName) {
        super(dataName);
    }

    @Override
    protected synchronized void resetData() {
        mTimedData.clear();;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter(2 * mTimedData.size());
        for (String key : mTimedData.keySet()) {
            long[] data = mTimedData.get(key);
            if (data != null) {
                cmp.appendData(key)
                .appendData(data.length);
                for (long timestamp : data) {
                    cmp.appendData(timestamp);
                }
            }
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null) {
            return;
        }
        int i = 0;
        String key;
        int dataSize;
        long[] data;
        while (i + 1 < compactedData.getSize()) {
            key = compactedData.getData(i);
            dataSize = compactedData.getInt(i + 1);
            if (dataSize > 0) {
                data = new long[dataSize];
                for (int j = 0; j < dataSize && i + 2 + j < compactedData.getSize(); j++) {
                    data[j] = compactedData.getLong(i + 2 + j);
                }
                i += 2 + dataSize;
                mTimedData.put(key, data);
            } else {
                i += 2;
            }
        }
    }

    public synchronized void newTimedData(String key, int amount) {
        if (key == null || amount <= 0) {
            return;
        }
        mTimedData.put(key, new long[amount]);
    }

    public synchronized void onTimedData(String key, long timestamp) {
        if (key == null || timestamp <= 0) {
            return;
        }
        long[] data = mTimedData.get(key);
        if (data != null) {
            int addedIndex;
            for (addedIndex = 0; addedIndex < data.length; addedIndex++) {
                if (data[addedIndex] <= 0) {
                    data[addedIndex] = timestamp;
                    break;
                }
            }
            if (addedIndex >= data.length) {
                // already full, remove first entry and move all up one position, append newest at end
                for (int i = 0; i + 1 < data.length; i++) {
                    data[i] = data[i + 1];
                }
                data[data.length - 1] = timestamp;
            }
            notifyListeners();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{!");
        for (String key : mTimedData.keySet()) {
            builder.append(key).append(": ").append(Arrays.toString(mTimedData.get(key))).append("!");
        }
        builder.append("}");
        return builder.toString();
    }
}
