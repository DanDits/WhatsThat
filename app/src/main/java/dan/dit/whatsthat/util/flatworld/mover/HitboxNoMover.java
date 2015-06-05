package dan.dit.whatsthat.util.flatworld.mover;

import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 01.06.15.
 */
public class HitboxNoMover extends HitboxMover {
    public static final HitboxNoMover INSTANCE = new HitboxNoMover();
    private HitboxNoMover() {}

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public float getAcceleration() {
        return 0;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public boolean update(Hitbox toMove, long updatePeriod) {
        return false;
    }

    @Override
    public boolean isMoving() {
        return false;
    }
}
