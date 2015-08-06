package dan.dit.whatsthat.util.flatworld.world;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dan.dit.whatsthat.util.flatworld.collision.CollisionController;
import dan.dit.whatsthat.util.flatworld.effects.WorldEffect;
import dan.dit.whatsthat.util.flatworld.effects.WorldEffectMovedText;
import dan.dit.whatsthat.util.flatworld.look.NinePatchLook;
import dan.dit.whatsthat.util.flatworld.mover.HitboxAttachedMover;
import dan.dit.whatsthat.util.flatworld.mover.HitboxNoMover;

/**
 * Created by daniel on 03.06.15.
 */
public abstract class FlatWorld {
    protected final CollisionController mCollider;
    protected final FlatWorldCallback mCallback;

    protected List<Actor> mActorsIterateData = new ArrayList<>();
    protected final List<Actor> mActorsData = Collections.synchronizedList(new ArrayList<Actor>());
    protected final List<Actor> mActiveActors = new ArrayList<>();

    protected List<WorldEffect> mEffectsIterateData = new ArrayList<>();
    protected final List<WorldEffect> mEffectsData = Collections.synchronizedList(new ArrayList<WorldEffect>());

    public FlatWorld(CollisionController collider, FlatWorldCallback callback) {
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
        updateActors(updatePeriod);
        updateEffects(updatePeriod);
        checkCollision();
    }

    protected void updateEffects(long updatePeriod) {
        List<WorldEffect> effectIterate = mEffectsIterateData;
        for (int i = 0; i < effectIterate.size(); i++) {
            effectIterate.get(i).update(updatePeriod);
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        drawActors(canvas, paint);
        drawEffects(canvas, paint);
    }

    protected void drawActors(Canvas canvas, Paint paint) {
        List<Actor> actorIterate = mActorsIterateData;
        for (int i = 0; i < actorIterate.size(); i++) {
            actorIterate.get(i).draw(canvas, paint);
        }
    }

    protected void drawEffects(Canvas canvas, Paint paint) {
        List<WorldEffect> effectIterate = mEffectsIterateData;
        for (int i = 0; i < effectIterate.size(); i++) {
            effectIterate.get(i).draw(canvas, paint);
        }
    }

    protected abstract void checkLeaveWorld(Actor actor);

    protected abstract void checkReachOutside(Actor actor);

    private void updateActors(long updatePeriod) {
        mActiveActors.clear();
        List<Actor> actorIterate = mActorsIterateData;
        for (int i = 0; i < actorIterate.size(); i++) {
            Actor actor = actorIterate.get(i);
            if (actor.isActive()) {
                if (actor.update(updatePeriod)) {
                    mCallback.onMoverStateChange(actor);
                }
                mActiveActors.add(actor);
                checkReachOutside(actor);
                checkLeaveWorld(actor);
            }
        }
    }

    protected void checkCollision() {
        mCollider.checkCollision(mActiveActors, mCallback);
    }

    public void pushEffect(WorldEffect effect) {
        mEffectsData.add(effect);
        synchronized (mEffectsData) {
            Iterator<WorldEffect> it = mEffectsData.iterator();
            while (it.hasNext()) {
                if (it.next().getState() == WorldEffect.STATE_TIMEOUT) {
                    it.remove();
                }
            }
            mEffectsIterateData = new ArrayList<>(mEffectsData);
        }
    }

    public WorldEffect addTimedMessage(NinePatchLook background, String message, float x, float y, long duration) {
        WorldEffectMovedText effect = new WorldEffectMovedText(background, x, y, HitboxNoMover.INSTANCE, message, -1);
        effect.setDuration(duration);
        pushEffect(effect);
        return effect;
    }

    public WorldEffect attachTimedMessage(NinePatchLook background, String message, int textMaxWidth, Actor toAttach, long duration) {
        WorldEffectMovedText effect = new WorldEffectMovedText(background, 0, 0, new HitboxAttachedMover(toAttach.getHitbox()), message, textMaxWidth);
        effect.setDuration(duration);
        pushEffect(effect);
        return effect;
    }

    public void addActor(Actor actor) {
        mActorsData.add(actor);
        synchronized (mActorsData) {
            mActorsIterateData = new ArrayList<>(mActorsData);
        }
    }

    public boolean removeActor(Actor actor) {
        boolean removed = mActorsData.remove(actor);
        synchronized (mActorsData) {
            mActorsIterateData = new ArrayList<>(mActorsData);
        }
        return removed;
    }

    public abstract void setRandomPositionInside(Actor actor, Random rand);

    public CollisionController getCollider() {
        return mCollider;
    }


}
