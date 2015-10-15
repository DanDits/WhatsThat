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

import android.content.res.Resources;

import dan.dit.whatsthat.util.dependencies.Dependable;

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
