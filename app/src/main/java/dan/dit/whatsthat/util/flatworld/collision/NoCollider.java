package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 05.06.15.
 */
public class NoCollider<W> extends Collider<W> {

    @Override
    public <W1 extends W, W2 extends W> boolean checkCollision(W1 box1, W2 box2) {
        return false;
    }
}
