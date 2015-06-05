package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 03.06.15.
 */
public interface Collidable<W> {
    boolean checkCollision(W with);
}
