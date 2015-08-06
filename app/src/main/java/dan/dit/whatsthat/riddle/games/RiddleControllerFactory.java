package dan.dit.whatsthat.riddle.games;

import dan.dit.whatsthat.riddle.Riddle;

/**
 * Created by daniel on 08.05.15.
 */
public class RiddleControllerFactory {
    public static final RiddleControllerFactory INSTANCE = new RiddleControllerFactory();

    private RiddleControllerFactory() {}

    RiddleController makeController(RiddleGame game, Riddle riddle) {
        return new RiddleController(game, riddle);
    }

    protected static class Silent extends RiddleControllerFactory {
        private Silent() {}

        protected RiddleController makeController(RiddleGame game, Riddle riddle) {
            return new SilentRiddleController(game, riddle);
        }
    }
}
