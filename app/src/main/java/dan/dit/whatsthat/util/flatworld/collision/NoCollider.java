package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 05.06.15.
 */
public class NoCollider<W extends Collidable> extends Collider<W> {

    @Override
    public boolean checkCollision(W box1, W box2) {
        return false;
    }
}
