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

package dan.dit.whatsthat.util.flatworld.effects;

import android.graphics.Canvas;
import android.graphics.Paint;

import dan.dit.whatsthat.util.flatworld.look.Look;

/**
 * Created by daniel on 26.06.15.
 */
public abstract class WorldEffect {
    public static final int STATE_TIMEOUT = 0;
    private static final int STATE_RUNNING = 1;
    private static final long DURATION_INFINITE = Long.MAX_VALUE;

    Look mLook;
    private long mDuration;

    int mFadeFrom;
    int mFadeTo;
    long mFadeOffsetDuration;
    long mFadeTime;
    long mFadeTimeTotal;
    boolean mFadeAlphaOnly;

    WorldEffect(Look look) {
        mLook = look;
        if (mLook == null) {
            throw new IllegalArgumentException("Null frames given to WorldEffect.");
        }
        mDuration = DURATION_INFINITE;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public int update(long updatePeriod) {
        if (mDuration <= 0L) {
            return STATE_TIMEOUT;
        }
        mDuration -= updatePeriod;
        if (mFadeOffsetDuration > 0L) {
            mFadeOffsetDuration -= updatePeriod;
        } else {
            mFadeTime += updatePeriod;
        }
        mLook.update(updatePeriod);
        return getState();
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public void startFade(int fadeFrom, int fadeTo, long fadeTime, long startOffset, boolean
            fadeAlphaOnly) {
        mFadeFrom = fadeFrom;
        mFadeTo = fadeTo;
        mFadeTime = 0;
        mFadeTimeTotal = fadeTime;
        mFadeOffsetDuration = startOffset;
        mFadeAlphaOnly = fadeAlphaOnly;
    }

    public int getState() {
        return mDuration > 0 ? STATE_RUNNING : STATE_TIMEOUT;
    }
}
