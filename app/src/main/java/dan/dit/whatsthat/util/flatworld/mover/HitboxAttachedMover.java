package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 26.06.15.
 */
public class HitboxAttachedMover extends HitboxMover {

    private final Hitbox mAttachedTo;

    public HitboxAttachedMover(Hitbox attachTo) {
        mAttachedTo = attachTo;
        if (mAttachedTo == null) {
            throw new IllegalArgumentException("Cannot attack to null hitbox.");
        }
    }

    @Override
    public float getSpeed() {
        return 0.f;
    }

    @Override
    public float getAcceleration() {
        return 0.f;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        toMove.setCenter(mAttachedTo.getCenterX(), mAttachedTo.getCenterY());
        return false;
    }

    @Override
    public boolean isMoving() {
        return false;
    }
}
