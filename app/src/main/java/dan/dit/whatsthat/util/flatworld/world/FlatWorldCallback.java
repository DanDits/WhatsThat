package dan.dit.whatsthat.util.flatworld.world;

/**
 * Created by daniel on 05.06.15.
 */
public interface FlatWorldCallback<W extends Actor> {
    void onReachedEndOfWorld(W columbus, float x, float y, int borderFlags);
    void onLeftWorld(W jesus, int borderFlags);

    void onCollision(W colliding1, W colliding2);

    void onMoverStateChange(W actor);
}
