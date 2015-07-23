package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.RectF;

import java.util.Random;

import dan.dit.whatsthat.util.flatworld.collision.CollisionController;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;
import dan.dit.whatsthat.util.flatworld.effects.WorldEffect;
import dan.dit.whatsthat.util.flatworld.look.NinePatchLook;

/**
 * Created by daniel on 05.06.15.
 */
public class FlatRectWorld extends FlatWorld {
    public static final int BORDER_FLAG_LEFT = 1;
    public static final int BORDER_FLAG_RIGHT = 2;
    public static final int BORDER_FLAG_TOP = 4;
    public static final int BORDER_FLAG_BOTTOM = 8;
    private static final float TEXT_EFFECT_MAX_WIDTH_FACTOR = 0.3f;

    private RectF mWorldRect;

    public FlatRectWorld(RectF worldRect, CollisionController collider, FlatWorldCallback callback) {
        super(collider, callback);
        mWorldRect = worldRect;
        if (worldRect == null || worldRect.width() <= 0.f || worldRect.height() <= 0.f) {
            throw new IllegalArgumentException("Illegal world: " + worldRect);
        }
    }

    @Override
    protected void checkReachOutside(Actor actor) {
        RectF bound = actor.getHitbox().getBoundingRect();
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
    protected void checkLeaveWorld(Actor actor) {
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

    @Override
    public void setRandomPositionInside(Actor actor, Random rand) {
        RectF bounds = actor.getHitbox().getBoundingRect();
        float paddingLeft = bounds.centerX() - bounds.left;
        float paddingRight = bounds.right - bounds.centerX();
        float paddingTop = bounds.centerY() - bounds.top;
        float paddingBottom = bounds.bottom - bounds.centerY();
        float x = mWorldRect.left + paddingLeft + rand.nextFloat() * (mWorldRect.width() - paddingRight - paddingLeft);
        float y = mWorldRect.top + paddingTop + rand.nextFloat() * (mWorldRect.height() - paddingTop - paddingBottom);
        actor.getHitbox().setCenter(x, y);
        checkReachOutside(actor);
        checkLeaveWorld(actor);
    }

    public WorldEffect attachTimedMessage(Actor attachTo, NinePatchLook[] cornerPatches, String text, long duration) {
        float width = getRight() - getLeft();
        float height = getBottom() - getTop();
        NinePatchLook look;
        Hitbox hitbox = attachTo.getHitbox();
        float x = hitbox.getCenterX();
        float y = hitbox.getCenterY();
        float offsetX;
        float offsetY;
        float heightFactor;
        if (x < getLeft() + width / 2) {
            offsetX = hitbox.getBoundingRect().width() / 2.f;
            if (y < getTop() + height / 2) {
                offsetY = hitbox.getBoundingRect().height() / 2.f;
                look = cornerPatches[0];
                heightFactor = 0;
            } else {
                offsetY = -hitbox.getBoundingRect().height() / 2.f;
                look = cornerPatches[3];
                heightFactor = -1;
            }
        } else {
            offsetX = -hitbox.getBoundingRect().width() / 2.f;
            if (y < getTop() + height / 2) {
                offsetY = hitbox.getBoundingRect().height() / 2.f;
                look = cornerPatches[1];
                heightFactor = 0;
            } else {
                offsetY = -hitbox.getBoundingRect().height() / 2.f;
                look = cornerPatches[2];
                heightFactor = -1;
            }
        }
        look.reset();
        WorldEffect effect = attachTimedMessage(look, text, (int) (width * TEXT_EFFECT_MAX_WIDTH_FACTOR), attachTo, duration);
        look.setOffset(offsetX + (offsetX < 0 ? -look.getWidth() : 0), offsetY + heightFactor * look.getHeight());
        return effect;
    }

    public float getLeft() {
        return mWorldRect.left;
    }

    public float getWidth() {
        return mWorldRect.width();
    }

    public float getRight() {
        return mWorldRect.right;
    }

    public float getTop() {
        return mWorldRect.top;
    }

    public float getBottom() {
        return mWorldRect.bottom;
    }

    public float getHeight() {
        return mWorldRect.height();
    }
}
