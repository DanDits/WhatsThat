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

package dan.dit.whatsthat.util.flatworld.look;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;

/**
 * Created by daniel on 06.09.15.
 */
public class BitmapLook extends Look {

    private final Bitmap mBitmap;

    public BitmapLook(Bitmap image) {
        mBitmap = image;
        if (mBitmap == null) {
            throw new IllegalArgumentException("No bitmap given to bitmap look.");
        }
    }

    @Override
    public int getWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public boolean update(long updatePeriod) {
        return false;
    }

    @Override
    public void draw(Canvas canvas, float x, float y, @Nullable Paint paint) {
        if (mVisible) {
            canvas.drawBitmap(mBitmap, x + mOffsetX, y + mOffsetY, paint);
        }
    }

    @Override
    public void reset() {

    }
}
