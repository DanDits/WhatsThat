package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 05.06.15.
 */
public abstract class Collider<W> {

    public abstract <W1 extends W, W2 extends W> boolean checkCollision(W1 box1, W2 box2);
}
