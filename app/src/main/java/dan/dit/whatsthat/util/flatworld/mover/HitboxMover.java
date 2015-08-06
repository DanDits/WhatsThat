package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 30.05.15.
 */
public abstract class HitboxMover {
    static final float ONE_SECOND = 1000.f; //ms

    public abstract float getSpeed();
    public abstract float getAcceleration();
    public abstract int getState();

    /**
     * Periodially updates the mover, moving the given hitbox. The kind of moving
     * is defined by the mover, the impact by the fraction of the given update period.
     * @param toMove The hitbox to move.
     * @param updatePeriod The time in miliseconds that passed.
     * @return A hint if updating resulted in an internal state change of the mover. Else false.
     */
    public abstract boolean update(Hitbox toMove, long updatePeriod);
    public abstract boolean isMoving();

}
