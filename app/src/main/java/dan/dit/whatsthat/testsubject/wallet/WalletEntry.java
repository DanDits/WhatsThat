package dan.dit.whatsthat.testsubject.wallet;

import android.content.res.Resources;

import dan.dit.whatsthat.testsubject.dependencies.Dependable;

/**
 * Created by daniel on 10.06.15.
 */
public class WalletEntry implements Dependable {
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    private final String mKey;
    private int mNameResId;
    private int mValue;

    public WalletEntry(String key, int nameResId, int defaultValue) {
        mKey = key;
        mNameResId = nameResId;
        mValue = defaultValue;
        if (key == null) {
            throw new IllegalArgumentException("No key given.");
        }
    }

    public void setNameResourceId(int nameResourceId) {
        mNameResId = nameResourceId;
    }

    @Override
    public int getValue() {
        return mValue;
    }

    public String getKey() {
        return mKey;
    }

    @Override
    public CharSequence getName(Resources res) {
        return mNameResId == 0 ? mKey : res.getString(mNameResId);
    }

    void setValue(int value) {
        mValue = value;
    }
}
