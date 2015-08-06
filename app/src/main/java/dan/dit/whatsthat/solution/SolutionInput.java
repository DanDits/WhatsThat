package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 30.03.15.
 */
public abstract class SolutionInput implements Compactable {
    Solution mSolution;
    SolutionInputListener mListener;



    SolutionInput(Compacter compacted) throws CompactedDataCorruptException {
        unloadData(compacted);
    }

    SolutionInput(Solution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("No solution given.");
        }
        initSolution(solution);
    }

    void setListener(SolutionInputListener listener) {
        mListener = listener;
    }

    public abstract int estimateSolvedValue();
    protected abstract void initSolution(@NonNull Solution solution);
    public abstract void draw(Canvas canvas);
    public abstract boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY);
    public abstract boolean onUserTouchDown(float x, float y);
    public abstract @NonNull Solution getCurrentUserSolution();

    public abstract void calculateLayout(float width, float height, DisplayMetrics displayMetrics);
}
