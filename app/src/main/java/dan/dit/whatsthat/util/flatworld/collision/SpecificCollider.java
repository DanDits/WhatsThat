package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 05.06.15.
 */
public class SpecificCollider<W extends Collidable<W>> extends Collider<W> {
    @Override
    public boolean checkCollision(W box1, W box2) {
        return box1.checkCollision(box2);
    }
}
