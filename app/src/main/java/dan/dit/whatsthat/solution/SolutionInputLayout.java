package dan.dit.whatsthat.solution;

import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

/**
 * Created by daniel on 09.01.16.
 */
abstract class SolutionInputLayout {
    /**
     * Calculates the layout of the SolutionInput.
     * @param width The width of the layout in pixels.
     * @param height The height of the layout in pixels.
     * @param displayMetrics The display metrics to use for layout calculation. Can be null, this
     *                       method must not crash then but the layout is not assumed to be valid.
     */
    public abstract void calculateLayout(float width, float height, @Nullable DisplayMetrics
            displayMetrics);


    /**
     * Draws the SolutionInput onto the given canvas.
     * @param canvas The canvas to draw the solution input onto.
     */
    public abstract void draw(Canvas canvas);
}
