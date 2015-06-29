package dan.dit.whatsthat.util.flatworld.effects;

import dan.dit.whatsthat.util.flatworld.look.NinePatchLook;
import dan.dit.whatsthat.util.flatworld.mover.HitboxMover;

/**
 * Created by daniel on 26.06.15.
 */
public class WorldEffectMovedText extends WorldEffectMoved {

    public WorldEffectMovedText(NinePatchLook background, float x, float y, HitboxMover mover, String text, int maxTextWidth) {
        super(background, x, y, mover);
        background.setText(text, maxTextWidth);
    }

}
