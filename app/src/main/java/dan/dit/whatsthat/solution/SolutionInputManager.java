package dan.dit.whatsthat.solution;

import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 31.03.15.
 */
public class SolutionInputManager {

    private SolutionInputManager() {}

    public static final SolutionInput getSolutionInput(RiddleType type, Solution solution) {
        return new SolutionInputLetterClick(solution);
    }

    public static SolutionInput reconstruct(Compacter data) throws CompactedDataCorruptException {
        if (data == null || data.getSize() == 0) {
            throw new CompactedDataCorruptException("No data to reconstruct solution input.");
        }
        switch (data.getData(0)) {
            case SolutionInputLetterClick.IDENTIFIER:
                return new SolutionInputLetterClick(data);
            default:
                throw new CompactedDataCorruptException("No solution input found.").setCorruptData(data);
        }
    }
}
