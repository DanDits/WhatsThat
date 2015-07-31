package dan.dit.whatsthat.testsubject.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * A wallet is a single object hold by the TestSubject which contains
 * various kinds of currencies, expenses, valuables and unlocked features that can be used.
 * The wallet is updated only when the corresponding event happened and changes to the wallet are saved by the TestSubject
 * and not validated again in the future.
 * Created by daniel on 09.06.15.
 */
public class Wallet {
    private static final String WALLET_FILE_NAME = "dan.dit.whatsthat.wallets";
    private Map<String, WalletEntry> mEntries;
    private Editor mEditor;
    private SharedPreferences mPrefs;
    private final String mName;

    public Wallet(Context context, String name) {
        mName = name;
        mPrefs = context.getSharedPreferences(WALLET_FILE_NAME, Context.MODE_PRIVATE);
        mEntries = new HashMap<>();
        mEditor = new Editor();
    }

    public WalletEntry assureEntry(String key) {
        return assureEntry(key, WalletEntry.FALSE);
    }

    public WalletEntry assureEntry(String key, int defaultValue) {
        WalletEntry entry = mEntries.get(key);
        if (entry == null) {
            synchronized (this) {
                int loaded = mPrefs.getInt(mName + key, defaultValue);
                entry = new WalletEntry(key, 0, loaded);
                mEntries.put(key, entry);
                mEditor.init(entry).apply();
            }
        }
        return entry;
    }

    public int getEntryValue(String key) {
        return assureEntry(key).getValue();
    }

    public int getEntryValue(String key, int defaultValue) {
        return assureEntry(key, defaultValue).getValue();
    }

    public Editor editEntry(String key) {
        return mEditor.init(assureEntry(key));
    }

    public Editor editEntry(String key, int defaultValue) {
        return mEditor.init(assureEntry(key, defaultValue));
    }

    /**
     * Editor of wallet entries. Each edit only accepts inputs that will
     * increase the WalletEntry value, so setting to a smaller value, setting a true
     * entry to false or negative deltas will do nothing.
     * Depts can be implemented by a separate entry.
     */
    public class Editor {
        private WalletEntry mEntry;
        private int mValue;

        private Editor init(WalletEntry entry) {
            mEntry = entry;
            mValue = entry.getValue();
            return this;
        }

        public void add(int delta) {
            if (delta > 0) {
                mValue = mEntry.getValue() + delta;
                apply();
            }
        }

        public void set(int value) {
            if (value > mValue) {
                mValue = value;
                apply();
            }
        }

        public void setTrue() {
            if (mValue != WalletEntry.TRUE && mValue < WalletEntry.TRUE) {
                mValue = WalletEntry.TRUE;
                apply();
            }
        }

        private void apply() {
            SharedPreferences.Editor editor = mPrefs.edit();
            mEntry.setValue(mValue);
            editor.putInt(mName + mEntry.getKey(), mValue).apply();
        }
    }
}
