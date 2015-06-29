package dan.dit.whatsthat.util.flatworld.collision;

/**
 * Created by daniel on 26.06.15.
 */
public class CollisionTester {
    public static final int RESULT_INDEFINITE = -1;
    public static final int RESULT_NO_COLLISION = 0;
    public static final int RESULT_COLLISION = 1;
    public static final CollisionTester NO_COLLISION = new CollisionTester() {
        @Override
        public int collisionTest(Hitbox hitbox) {
            return RESULT_NO_COLLISION;
        }

        @Override
        public int collisionTest(HitboxCircle hitbox) {
            return RESULT_NO_COLLISION;
        }

        public int collisionTest(HitboxRect hitbox) {
            return RESULT_NO_COLLISION;
        }
    };

    public int collisionTest(Hitbox hitbox) {
        return RESULT_INDEFINITE;
    }

    public int collisionTest(HitboxCircle hitbox) {
        return RESULT_INDEFINITE;
    }

    public int collisionTest(HitboxRect hitbox) {
        return RESULT_INDEFINITE;
    }
}
