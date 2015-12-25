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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;

import dan.dit.whatsthat.util.flatworld.collision.HitboxGhostPoint;
import dan.dit.whatsthat.util.flatworld.look.Look;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;
import dan.dit.whatsthat.util.general.MathFunction;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;

/**
 * Created by daniel on 26.06.15.
 */
public class WorldEffectMoved extends  WorldEffect {
    private static final float FADE_COLOR_IMPACT = 0.8f;
    private HitboxGhostPoint mPoint;
    private HitboxMover mMover;
    private Paint mPaint;

    public WorldEffectMoved(Look look, float x, float y, HitboxMover mover) {
        super(look);
        mPoint = new HitboxGhostPoint(x, y);
        mMover = mover;
    }

    public void setCenter(float x, float y) {
        mPoint.setCenter(x - mLook.getWidth() / 2, y - mLook.getHeight() / 2);
    }

    @Override
    public int update(long updatePeriod) {
        int state = super.update(updatePeriod);
        if (state == STATE_TIMEOUT) {
            return state;
        }
        if (mMover.update(mPoint, updatePeriod)) {
            onUpdateMoverStateChange();
        }
        return state;
    }

    private void onUpdateMoverStateChange() {

    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (getState() == STATE_TIMEOUT) {
            return;
        }
        ColorFilter oldFilter = paint != null ? paint.getColorFilter() : null;
        int oldAlpha = paint != null ? paint.getAlpha() : 255;
        if (mFadeOffsetDuration <= 0 && mFadeTime < mFadeTimeTotal) {
            float fadeFraction = mFadeTime / (float) mFadeTimeTotal;
            if (paint == null) {
                if (mPaint == null) {
                    mPaint = new Paint();
                }
                paint = mPaint;
            }
            if (mFadeAlphaOnly) {
                paint.setAlpha((int) MathFunction.LinearInterpolation.evaluate(0, mFadeFrom, 1,
                        mFadeTo, fadeFraction));
            } else {
                int currColor = ColorAnalysisUtil.interpolateColorLinear(mFadeFrom, mFadeTo, fadeFraction);
                ColorFilter colorFilter = new LightingColorFilter(
                        ColorAnalysisUtil.colorMultiples(currColor, FADE_COLOR_IMPACT)
                        , 0);

                paint.setColorFilter(colorFilter);
                paint.setAlpha(Color.alpha(currColor));
            }
        }
        mLook.draw(canvas, mPoint.getCenterX(), mPoint.getCenterY(), paint);
        if (oldFilter != null && paint != null) {
            paint.setAlpha(oldAlpha);
            paint.setColorFilter(oldFilter);
        }
    }
}
