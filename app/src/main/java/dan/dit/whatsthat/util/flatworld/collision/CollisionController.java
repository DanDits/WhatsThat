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
        // full O(n²) collision checks
        Iterator<Actor> it = toCheck.iterator();
        while (it.hasNext()) {
            Actor curr = it.next();
            it.remove();
            for (Actor subCheck : toCheck) {
                if (checkCollision(subCheck.getHitbox(), curr.getHitbox())) {
                    callback.onCollision(subCheck, curr);
                }
            }
        }
    }

    public abstract boolean checkCollision(Hitbox box1, Hitbox box2);
}
