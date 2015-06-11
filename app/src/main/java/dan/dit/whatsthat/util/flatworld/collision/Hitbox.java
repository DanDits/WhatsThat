package dan.dit.whatsthat.util.flatworld.collision;

import android.graphics.RectF;

import java.util.Random;

/**
 * Created by daniel on 31.05.15.
 */
public abstract class Hitbox<W extends Collidable<? extends W>> implements  Collidable<W> {
    protected RectF mBoundingRect = new RectF();

    public abstract RectF getBoundingRect();


    public abstract boolean isInside(float x, float y);
    public abstract boolean checkRandomPointCollision(Hitbox other, Random rand, RectF pointInRect);

    public abstract void move(float deltaX, float deltaY);

    /**
     * Sets the topmost point of the hitbox.
     * @param top The topmost point.
     */
    public abstract void setTop(float top);

    /**
     * Sets the leftmost point of the hitbox to the given value.
     * @param left The leftmost point of the hitbox.
     */
    public abstract void setLeft(float left);

    public abstract void setRight(float right);

    public abstract void setBottom(float bottom);

    /**
     * Sets the center point of the hitbox to the given value.
     * @param centerX The center x value.
     * @param centerY The center y value.
     */
    public abstract void setCenter(float centerX, float centerY);

    protected static float getRandomFloatInRange(float min, float max, Random random) {
        return min + random.nextFloat() * (max - min);
    }

    public abstract float getCenterX();
    public abstract float getCenterY();
}
