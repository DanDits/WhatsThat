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

import java.util.List;

import dan.dit.whatsthat.util.flatworld.world.Actor;

/**
 * Created by daniel on 05.06.15.
 */
public abstract class CollisionController {
    public interface CollisionCallback {
        void onCollision(Actor colliding1, Actor colliding2);
    }

    public void checkCollision(List<Actor> toCheck, CollisionCallback callback) {
        for (int i = 0; i < toCheck.size(); i++) {
            Actor curr = toCheck.get(i);
            for (int j = i + 1; j < toCheck.size(); j++) {
                Actor subCheck = toCheck.get(j);
                if (checkCollision(subCheck.getHitbox(), curr.getHitbox())) {
                    callback.onCollision(subCheck, curr);
                }
            }
        }
    }

    public abstract boolean checkCollision(Hitbox box1, Hitbox box2);
}
