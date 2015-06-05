package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.RectF;

import dan.dit.whatsthat.util.flatworld.Actor;
import dan.dit.whatsthat.util.flatworld.collision.Collider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 05.06.15.
 */
public class FlatRectWorld<W extends Hitbox> extends FlatWorld<W> {
    public static final int BORDER_FLAG_LEFT = 1;
    public static final int BORDER_FLAG_RIGHT = 2;
    public static final int BORDER_FLAG_TOP = 4;
    public static final int BORDER_FLAG_BOTTOM = 8;

    private RectF mWorldRect;

    public FlatRectWorld(RectF worldRect, Collider<W> collider, FlatWorldCallback<Actor<W>> callback) {
        super(collider, callback);
        mWorldRect = worldRect;
        if (worldRect == null || worldRect.width() <= 0.f || worldRect.height() <= 0.f) {
            throw new IllegalArgumentException("Illegal world: " + worldRect);
        }
    }

    @Override
    protected void checkLeaveWorld(Actor<W> actor) {
        W box = actor.getHitbox();
        RectF bound = box.getBoundingRect();
        float x = bound.centerX();
        float y = bound.centerY();
        int flags = 0;
        if (bound.left < mWorldRect.left) {
            flags |= BORDER_FLAG_LEFT;
            x = mWorldRect.left;
        }
        if (bound.right > mWorldRect.right) {
            flags |= BORDER_FLAG_RIGHT;
            x = mWorldRect.right;
        }
        if (bound.top < mWorldRect.top) {
            flags |= BORDER_FLAG_TOP;
            y = mWorldRect.top;
        }
        if (bound.bottom > mWorldRect.bottom) {
            flags |= BORDER_FLAG_BOTTOM;
            y = mWorldRect.bottom;
        }
        if (flags != 0) {
            mCallback.onReachedEndOfWorld(actor, x, y, flags);
        }
    }

    @Override
    protected void checkReachOutside(Actor<W> actor) {
        RectF bound = actor.getHitbox().getBoundingRect();
        int flags = 0;
        if (bound.left > mWorldRect.right) {
            flags |= BORDER_FLAG_RIGHT;
        }
        if (bound.right < mWorldRect.left) {
            flags |= BORDER_FLAG_LEFT;
        }
        if (bound.top > mWorldRect.bottom) {
            flags |= BORDER_FLAG_BOTTOM;
        }
        if (bound.bottom < mWorldRect.top) {
            flags |= BORDER_FLAG_TOP;
        }
        if (flags != 0) {
            mCallback.onLeftWorld(actor, flags);
        }
    }
}
