package dan.dit.whatsthat.solution;

import dan.dit.whatsthat.riddle.RiddleType;

/**
 * Created by daniel on 31.03.15.
 */
public class SolutionInputManager {

    private SolutionInputManager() {}

    public static final SolutionInput getSolutionInput(RiddleType type) {
        return new SolutionInput();
    }
}
