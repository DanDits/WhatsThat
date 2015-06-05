package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 05.06.15.
 */
public abstract class Collider<W extends Collidable> {

    public abstract boolean checkCollision(W box1, W box2);
}
