package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.util.flatworld.collision.Collider;
import dan.dit.whatsthat.util.flatworld.collision.Hitbox;

/**
 * Created by daniel on 03.06.15.
 */
public abstract class FlatWorld<W extends Hitbox<? extends W>> {
    protected final List<Actor<W>> mActors = new ArrayList<>();
    protected final Collider<? super W> mCollider;
    protected final FlatWorldCallback<Actor<W>> mCallback;
    protected List<Actor<W>> mActiveActors = new LinkedList<>();

    public FlatWorld(Collider<? super W> collider, FlatWorldCallback<Actor<W>> callback) {
        mCollider = collider;
        if (collider == null) {
            throw new IllegalArgumentException("No collider given (use NoCollider for no collision).");
        }
        mCallback = callback;
        if (callback == null) {
            throw new IllegalArgumentException("No callback.");
        }
    }

    public final void update(long updatePeriod) {
        mActiveActors.clear();
        for (Actor<W> actor : mActors) {
            if (actor.isActive()) {
                if (actor.update(updatePeriod)) {
                    mCallback.onMoverStateChange(actor);
                }
                mActiveActors.add(actor);
                checkReachOutside(actor);
                checkLeaveWorld(actor);
            }
        }
        checkCollision();
    }

    public void drawActors(Canvas canvas, Paint paint) {
        for (Actor<W> actor : mActors) {
            actor.draw(canvas, paint);
        }
    }

    protected abstract void checkLeaveWorld(Actor<W> actor);

    protected abstract void checkReachOutside(Actor<W> actor);

    protected void checkCollision() {
        // full O(n²) collision checks
        Iterator<Actor<W>> it = mActiveActors.iterator();
        while (it.hasNext()) {
            Actor<W> curr = it.next();
            it.remove();
            for (Actor<W> toCheck : mActiveActors) {
                if (mCollider.checkCollision(toCheck.getHitbox(), curr.getHitbox())) {
                    mCallback.onCollision(toCheck, curr);
                }
            }
        }
    }

    public void addActor(Actor<W> actor) {
        mActors.add(actor);
    }

    public abstract void setRandomPositionInside(Actor<W> actor, Random rand);
}
