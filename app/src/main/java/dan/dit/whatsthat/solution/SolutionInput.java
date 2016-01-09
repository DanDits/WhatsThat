/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Abstract class of some way a user can find a {@link Solution}. A SolutionInput is drawn onto a
 * canvas and user can interact by pressing down or flinging on the view. The SolutionInput
 * allows a way to find the main solution word and -implementation dependant - the other solution
 * words if present.<br>State of the SolutionInput can be saved by compacting it and recreating
 * it with the compacted data.
 * Created by daniel on 30.03.15.
 */
public abstract class SolutionInput implements Compactable {
    Solution mSolution;
    SolutionInputListener mListener;


    /**
     * Recreates the SolutionInput from the given compacted data.
     * @param compacted The compacted data to recreate the SolutionInput from.
     * @throws CompactedDataCorruptException If any data is missing or corrupt.
     */
    SolutionInput(Compacter compacted) throws CompactedDataCorruptException {
        unloadData(compacted);
    }

    /**
     * Creates a new SolutionInput initializing it with the given Solution.
     * @param solution The non null Solution to initialize.
     */
    SolutionInput(@NonNull Solution solution) {
        if (solution == null) {
            throw new IllegalArgumentException("No solution given.");
        }
        initSolution(solution);
    }

    /**
     * Sets the SolutionInputListener.
     * @param listener The SolutionInputListener.
     */
    synchronized void setListener(SolutionInputListener listener) {
        mListener = listener;
    }

    /**
     * Resets any user input and makes the input look like it was newly created. This is useful
     * for sharing the solution input without showing the currently (partly) entered solution.
     */
    public abstract void reset();

    public static final int HINT_LEVEL_NONE = 0;
    public static final int HINT_LEVEL_MINIMAL = 1;
    public static final int HINT_LEVEL_GOOD_HELP = 10;
    public static final int HINT_LEVEL_LIMITLESS = Integer.MAX_VALUE;
    /**
     * Provides a hint, making it easier to find the solution. How this hint is given is up to the
     * implementation, though the effect of the hint should be accelerating, that is: the first
     * given hint(s) should have minimal effect and hints should never make the SolutionInput
     * auto solve itself.<br>
     * Possible parameters: {@link SolutionInput#HINT_LEVEL_MINIMAL}, {@link
     * SolutionInput#HINT_LEVEL_GOOD_HELP} or {@link SolutionInput#HINT_LEVEL_LIMITLESS}.
     * @param hintLevel The maximum level of impact of the hint to provide. If there are no more
     *                  hints available for this threshold, no new hint may be provided.
     * @return True if a new hint was provided.
     */
    public abstract boolean provideHint(int hintLevel);

    /**
     * Returns the maximum provided hint level currently given to the user. This is {@link
     * SolutionInput#HINT_LEVEL_NONE} if no hint was provided yet.
     * @return The maximum provided hint level.
     */
    public abstract int getProvidedHintLevel();

    /**
     * Returns an estimated solved value between {@link Solution#SOLVED_NOTHING} and {@link
     * Solution#SOLVED_COMPLETELY} to show how much the input to the SolutionInput reached the
     * initialized solution. If the given solution is reached, exactly {@link
     * Solution#SOLVED_COMPLETELY} is returned. Do not rely on intermediate values as this is
     * just a feature for some SolutionInputs.
     * @return An estimation of the solving state of the SolutionInput.
     */
    public abstract int estimateSolvedValue();

    /**
     * Initializes a new solution for this SolutionInput. This will build the SolutionInput as
     * required. If the SolutionInput supports more than the main word of the Solution is
     * implementation dependant.
     * @param solution A valid solution to initialize the SolutionInput with.
     */
    protected abstract void initSolution(@NonNull Solution solution);

    /**
     * Returns the assoziated SolutionInputLayout responsible for displaying the SolutionInput.
     * @return The SolutionInputLayout.
     */
    abstract @NonNull SolutionInputLayout getLayout();

    /**
     * A fling gesture happened with a set threshold velocity and can be handled by the
     * SolutionInput.
     * @param startEvent The start motion event that triggered the fling gesture.
     * @param endEvent The end motion event that triggered the fling gesture.
     * @param velocityX The fling velocity in x direction in pixels per second.
     * @param velocityY The fling velocity in y direction in pixels per second.
     * @return If true, the SolutionInput will be invalidated and redrawn afterwards.
     */
    public abstract boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY);

    /**
     * A touch down gesture happened at the given point.
     * @param x The x location of the touch down point.
     * @param y The y location of the touch down point.
     * @return If true, the SolutionInput will be invalidated and redrawn afterwards.
     */
    public abstract boolean onUserTouchDown(float x, float y);

    /**
     * Returns a String representation of the currently entered user solution. This can be an
     * empty String and mostly an arbitrary String as allowed by the SolutionInput.
     * @return The currently entered user solution.
     */
    public abstract @NonNull Solution getCurrentUserSolution();

}
