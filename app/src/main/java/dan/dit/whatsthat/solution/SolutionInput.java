package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 30.03.15.
 */
public abstract class SolutionInput implements Compactable {
    protected Solution mSolution;



    public SolutionInput(Compacter compacted) throws CompactedDataCorruptException {
        unloadData(compacted);
    }

    public SolutionInput(Solution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("No solution given.");
        }
        initSolution(solution);
    }

    public abstract int estimateSolvedValue();
    protected abstract void initSolution(Solution solution);
    public abstract void draw(Canvas canvas);
    public abstract boolean performClick(float x, float y);
    public abstract @NonNull Solution getCurrentUserSolution();

    public abstract void calculateLayout(float width, float height, DisplayMetrics displayMetrics);
}
