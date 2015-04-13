package dan.dit.whatsthat.solution;

/**
 * Created by daniel on 13.04.15.
 */
public interface SolutionInputListener {

    /**
     * Notifies that the solution got completed.
     * @param userWord The word the user entered to complete the solution.
     * @return If the solution input is allowed to show that the solution is valid.
     */
    boolean onSolutionComplete(String userWord);

    /**
     * Indicates that the solution previously was complete and then got changed again.
     */
    void onSolutionIncomplete();
}
