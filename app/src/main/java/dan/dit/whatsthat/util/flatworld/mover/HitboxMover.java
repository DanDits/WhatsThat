package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 30.05.15.
 */
public abstract class HitboxMover {
    protected static final float ONE_SECOND = 1000.f; //ms

    public abstract float getSpeed();
    public abstract float getAcceleration();
    public abstract int getState();
    public abstract boolean update(Hitbox toMove, long updatePeriod);
    public abstract boolean isMoving();

}
