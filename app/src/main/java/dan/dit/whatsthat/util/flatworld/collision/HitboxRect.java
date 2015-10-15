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

package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

import dan.dit.whatsthat.util.flatworld.look.Look;

/**
 * Created by daniel on 30.05.15.
 */
public class HitboxRect extends Hitbox {
    private float mLeft;
    private float mTop;
    private float mBottom;
    private float mRight;
    private Tester mTester = new Tester();

    private HitboxRect(final float left, final float top, final float width, final float height) {
        mLeft = left;
        mTop = top;
        mRight = left + width;
        mBottom = top + height;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    private boolean checkCollision(HitboxRect other) {
        return mRight >= other.mLeft && mLeft <= other.mRight && mBottom >= other.mTop && mTop <= other.mBottom;
    }

    @Override
    public RectF getBoundingRect() {
        return mBoundingRect;
    }

    @Override
    public boolean isInside(float x, float y) {
        return x >= mLeft && x <= mRight && y >= mTop && y <= mBottom;
    }

    @Override
    public boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect) {
        float rx = getRandomFloatInRange(Math.max(pointInRect.left, mLeft), Math.min(pointInRect.right, mRight), rand);
        float ry = getRandomFloatInRange(Math.max(pointInRect.top, mTop), Math.min(pointInRect.bottom, mBottom), rand);
        return other.isInside(rx, ry);
    }

    @Override
    public void move(float x, float y) {
        mLeft += x;
        mRight += x;
        mTop += y;
        mBottom += y;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    @Override
    public void setTop(float newTop) {
        mBottom = newTop + mBottom - mTop;
        mTop = newTop;
        mBoundingRect.top = mTop;
        mBoundingRect.bottom = mBottom;
    }

    @Override
    public void setLeft(float newLeft) {
        mRight = newLeft + mRight - mLeft;
        mLeft = newLeft;
        mBoundingRect.right = mRight;
        mBoundingRect.left = mLeft;
    }

    @Override
    public void setRight(float right) {
        mLeft = right - (mRight - mLeft);
        mRight = right;
        mBoundingRect.right = mRight;
        mBoundingRect.left = mLeft;
    }

    @Override
    public void setBottom(float bottom) {
        mTop = bottom - (mBottom - mTop);
        mBottom = bottom;
        mBoundingRect.top = mTop;
        mBoundingRect.bottom = mBottom;
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        float width = mRight - mLeft;
        float height = mBottom - mTop;
        mTop = centerY - height / 2.f;
        mLeft = centerX - width / 2.f;
        mRight = mLeft + width;
        mBottom = mTop + height;
        mBoundingRect.set(mLeft, mTop, mRight, mBottom);
    }

    @Override
    public float getCenterX() {
        return mLeft + (mTop - mLeft) / 2.f;
    }

    @Override
    public float getCenterY() {
        return mTop + (mBottom - mTop) / 2.f;
    }

    @Override
    public CollisionTester getCollisionTester() {
        return mTester;
    }

    @Override
    public int accept(CollisionTester collisionTester) {
        return collisionTester.collisionTest(this);
    }

    private class Tester extends CollisionTester {
        @Override
        public int collisionTest(HitboxRect toCheck) {
            return checkCollision(toCheck) ? RESULT_COLLISION : RESULT_NO_COLLISION;
        }
    }

    public static HitboxRect makeHitbox(Look look, float hitboxWidthFraction, float hitboxHeightFraction, float frameLeft, float frameTop) {
        float offsetX = -look.getWidth() * (1 - hitboxWidthFraction) / 2.f;
        float offsetY = -look.getHeight() * (1 - hitboxHeightFraction) / 2.f;
        look.setOffset(offsetX, offsetY);
        return new HitboxRect(frameLeft -  offsetX, frameTop - offsetY, look.getWidth() * hitboxWidthFraction, look.getHeight() * hitboxHeightFraction);
    }
}
