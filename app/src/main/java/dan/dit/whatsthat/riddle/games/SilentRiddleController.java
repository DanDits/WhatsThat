package dan.dit.whatsthat.riddle.games;

import android.content.Context;
import android.support.annotation.NonNull;

import dan.dit.whatsthat.riddle.Riddle;

/**
 * Created by daniel on 08.05.15.
 */
public class SilentRiddleController extends RiddleController {
    SilentRiddleController(@NonNull RiddleGame riddleGame, @NonNull Riddle riddle) {
        super(riddleGame, riddle);
    }

    protected void onPreRiddleClose() {
        // do nothing
    }

    protected void onRiddleClosed(final Context context) {
        // do nothing
    }

    protected void onRiddleGotVisible() {
        // do nothing
    }
}
