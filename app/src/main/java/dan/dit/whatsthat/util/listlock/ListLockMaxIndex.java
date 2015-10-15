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

package dan.dit.whatsthat.util.listlock;

import java.util.List;

/**
 * Created by daniel on 09.06.15.
 */
public class ListLockMaxIndex {
    public static final int UNLIMITED_ELEMENTS = -1;
    private boolean mLocked;
    private int mMaxIndex;
    private int mMaxElements;
    private List mList;

    public ListLockMaxIndex(List list, int maxElements) {
        mList = list;
        mMaxElements = maxElements;
        refresh();
    }

    void refresh() {
        if (mMaxElements > 0) {
            mMaxIndex = Math.min(mList.size(), mMaxElements) - 1;
        } else {
            mMaxIndex = mList.size() - 1;
        }
        mLocked = false;
    }

    public boolean isUnlocked(int index) {
        return index <= mMaxIndex && !mLocked;
    }

    public void lock(int count) {
        mMaxIndex -= count;
        if (mMaxIndex < 0) {
            mLocked = true;
        }
    }

}
