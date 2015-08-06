package dan.dit.whatsthat.util.flatworld.collision;

import java.util.Iterator;
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
