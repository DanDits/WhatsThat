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

import android.view.MotionEvent;

/**
 * Created by daniel on 10.06.15.
 */
public class LockDistanceRefresher {
    private ListLockMaxIndex mLock;
    private float mMinDistanceToRefreshLock;
    private float mRefreshedAtX;
    private float mRefreshedAtY;

    public LockDistanceRefresher(ListLockMaxIndex lock, float minDistanceToRefreshLock) {
        mLock = lock;
        mMinDistanceToRefreshLock = minDistanceToRefreshLock;
        if (mLock == null) {
            throw new IllegalArgumentException("No lock to refresh.");
        }
    }

    public void update(MotionEvent event) {
        boolean refresh = false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            refresh = true;
        }
        float x= event.getX();
        float y = event.getY();
        if ((x - mRefreshedAtX) * (x - mRefreshedAtX) + (y - mRefreshedAtY) * (y - mRefreshedAtY) > mMinDistanceToRefreshLock * mMinDistanceToRefreshLock) {
            refresh = true;
        }
        if (refresh) {
            mRefreshedAtX = x;
            mRefreshedAtY = y;
            mLock.refresh();
        }
    }
}
