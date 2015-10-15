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
import android.util.Log;

import java.util.Random;

/**
 * Created by daniel on 05.06.15.
 */
public class GeneralHitboxCollider extends CollisionController {
    private static final int DEFAULT_CHECKS_COUNT = 1;

    private final int mChecksCount = DEFAULT_CHECKS_COUNT;
    private RectF mBoundIntersection = new RectF();
    private Random mRand = new Random();

    private boolean getIntersection(Hitbox obj1, Hitbox obj2, RectF intersection) {
        RectF bound1 = obj1.getBoundingRect();
        RectF bound2 = obj2.getBoundingRect();
        intersection.left = Math.max(bound1.left, bound2.left);
        intersection.right = Math.min(bound1.right, bound2.right);
        intersection.top = Math.max(bound1.top, bound2.top);
        intersection.bottom = Math.min(bound1.bottom, bound2. bottom);
        return intersection.left <= intersection.right && intersection.top <= intersection.bottom;
    }

    @Override
    public boolean checkCollision(Hitbox box1, Hitbox box2) {
        // first try direct collision
        int result = box2.accept(box1.getCollisionTester());
        if (result != CollisionTester.RESULT_INDEFINITE) {
            return result == CollisionTester.RESULT_COLLISION;
        }
        boolean hasIntersection = getIntersection(box1, box2, mBoundIntersection);
        if (hasIntersection) {
            for (int i = 0; i < mChecksCount; i++) {
                if (box1.checkRandomPointCollision(box2, mRand, mBoundIntersection)
                        || box2.checkRandomPointCollision(box1, mRand, mBoundIntersection)) {
                    return true;
                }
            }
        }
        return false;
    }
}
