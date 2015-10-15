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

/**
 * A hitbox without any collision and no inside that exists only at exactly one coordinate
 * which is its center.
 * Created by daniel on 26.06.15.
 */
public class HitboxGhostPoint extends Hitbox {

    private float mX;
    private float mY;
    private RectF mBounds;

    public HitboxGhostPoint(float x, float y) {
        mX = x;
        mY = y;
        mBounds = new RectF(mX, mY, mX, mY);
    }

    @Override
    public RectF getBoundingRect() {
        return mBounds;
    }

    @Override
    public boolean isInside(float x, float y) {
        return false;
    }

    @Override
    public boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect) {
        return false;
    }

    private void updateBounds() {
        mBounds.left = mX;
        mBounds.top = mY;
        mBounds.right = mX;
        mBounds.bottom = mY;
    }

    @Override
    public void move(float deltaX, float deltaY) {
        mX += deltaX;
        mY += deltaY;
        updateBounds();
    }

    @Override
    public void setTop(float top) {
        mY = top;
        updateBounds();
    }

    @Override
    public void setLeft(float left) {
        mX = left;
        updateBounds();
    }

    @Override
    public void setRight(float right) {
        mX = right;
        updateBounds();
    }

    @Override
    public void setBottom(float bottom) {
        mY = bottom;
        updateBounds();
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        mX = centerX;
        mY = centerY;
        updateBounds();
    }

    @Override
    public float getCenterX() {
        return mX;
    }

    @Override
    public float getCenterY() {
        return mY;
    }

    @Override
    public CollisionTester getCollisionTester() {
        return CollisionTester.NO_COLLISION;
    }

    @Override
    public int accept(CollisionTester tester) {
        return tester.collisionTest(this);
    }

}
