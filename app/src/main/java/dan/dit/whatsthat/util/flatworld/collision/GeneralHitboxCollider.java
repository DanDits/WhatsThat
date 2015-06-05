package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

/**
 * Created by daniel on 05.06.15.
 */
public class GeneralHitboxCollider extends Collider<Hitbox> {
    private static final int DEFAULT_CHECKS_COUNT = 1;

    private final int mChecksCount = DEFAULT_CHECKS_COUNT;
    private RectF mBoundIntersection = new RectF();
    private Random mRand = new Random();

    @Override
    public boolean checkCollision(Hitbox box1, Hitbox box2) {
        boolean hasIntersection = getIntersection(box1, box2, mBoundIntersection);
        if (hasIntersection) {
            for (int i = 0; i < mChecksCount; i++) {
                if (box1.checkRandomPointCollision(box2, mRand, mBoundIntersection)
                        || box2.checkRandomPointCollision(box1, mRand, mBoundIntersection)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean getIntersection(Hitbox obj1, Hitbox obj2, RectF intersection) {
        RectF bound1 = obj1.getBoundingRect();
        RectF bound2 = obj2.getBoundingRect();
        intersection.left = Math.max(bound1.left, bound2.left);
        intersection.right = Math.min(bound1.right, bound2.right);
        intersection.top = Math.max(bound1.top, bound2.top);
        intersection.bottom = Math.min(bound1.bottom, bound2. bottom);
        return intersection.left <= intersection.right && intersection.top <= intersection.bottom;
    }
}
