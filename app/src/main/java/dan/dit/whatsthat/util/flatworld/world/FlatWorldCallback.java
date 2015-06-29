package dan.dit.whatsthat.util.flatworld.world;

import dan.dit.whatsthat.util.flatworld.collision.CollisionController;

/**
 * Created by daniel on 05.06.15.
 */
public interface FlatWorldCallback extends CollisionController.CollisionCallback {
    void onReachedEndOfWorld(Actor columbus, float x, float y, int borderFlags);
    void onLeftWorld(Actor jesus, int borderFlags);

    void onMoverStateChange(Actor actor);
}
