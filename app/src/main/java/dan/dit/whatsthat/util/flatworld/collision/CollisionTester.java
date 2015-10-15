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

/**
 * Created by daniel on 26.06.15.
 */
public class CollisionTester {
    public static final int RESULT_INDEFINITE = -1;
    public static final int RESULT_NO_COLLISION = 0;
    public static final int RESULT_COLLISION = 1;
    public static final CollisionTester NO_COLLISION = new CollisionTester() {
        @Override
        public int collisionTest(Hitbox hitbox) {
            return RESULT_NO_COLLISION;
        }

        @Override
        public int collisionTest(HitboxCircle hitbox) {
            return RESULT_NO_COLLISION;
        }

        public int collisionTest(HitboxRect hitbox) {
            return RESULT_NO_COLLISION;
        }
    };

    public int collisionTest(Hitbox hitbox) {
        return RESULT_INDEFINITE;
    }

    public int collisionTest(HitboxCircle hitbox) {
        return RESULT_INDEFINITE;
    }

    public int collisionTest(HitboxRect hitbox) {
        return RESULT_INDEFINITE;
    }
}
