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

package dan.dit.whatsthat.util.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.util.general.ObserverController;

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
    private ObserverController<OnEntryChangedListener, WalletEntry>
            mOnEntryChangedListenerController = new ObserverController<>();

    public interface OnEntryChangedListener extends ObserverController.Observer<WalletEntry>{
        void onEntryRemoved(WalletEntry entry);
    }

    public Wallet(Context context, String name) {
        mName = name;
        mPrefs = context.getSharedPreferences(WALLET_FILE_NAME, Context.MODE_PRIVATE);
        mEntries = new HashMap<>();
        mEditor = new Editor();
    }

    public void removeEntry(String key) {
        WalletEntry entry = mEntries.remove(key);
        if (entry != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.remove(mName + key).apply();
            notifyRemovedListeners(entry);
        }
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

        public boolean add(int delta) {
            if (delta > 0) {
                mValue = mEntry.getValue() + delta;
                apply();
                return true;
            }
            return false;
        }

        public boolean set(int value) {
            if (value > mValue) {
                mValue = value;
                apply();
                return true;
            }
            return false;
        }

        public boolean setTrue() {
            if (mValue != WalletEntry.TRUE && mValue < WalletEntry.TRUE) {
                mValue = WalletEntry.TRUE;
                apply();
                return true;
            }
            return false;
        }

        private void apply() {
            SharedPreferences.Editor editor = mPrefs.edit();
            int oldValue = mEntry.getValue();
            mEntry.setValue(mValue);
            editor.putInt(mName + mEntry.getKey(), mValue).apply();
            if (oldValue != mValue) {
                notifyChangedListeners(mEntry);
            }
        }
    }

    public void addChangedListener(OnEntryChangedListener listener) {
        mOnEntryChangedListenerController.addObserver(listener);
    }

    public void removeChangedListener(OnEntryChangedListener listener) {
        mOnEntryChangedListenerController.removeObserver(listener);
    }

    private void notifyChangedListeners(WalletEntry entry) {
        mOnEntryChangedListenerController.notifyObservers(entry);
    }

    private void notifyRemovedListeners(WalletEntry entry) {
        for (OnEntryChangedListener listener : mOnEntryChangedListenerController) {
            listener.onEntryRemoved(entry);
        }
    }
}
