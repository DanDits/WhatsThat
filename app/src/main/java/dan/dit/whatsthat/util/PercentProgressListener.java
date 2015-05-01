package dan.dit.whatsthat.util;

/**
 * A generic interface for any progress that is expressed in percent
 * from 0% to 100% represented by integers from 0 to 100.
 * Created by daniel on 30.04.15.
 */
public interface PercentProgressListener {
    /**
     * The constant for completion, 100%.
     */
    public static final int PROGRESS_COMPLETE = 100;

    /**
     * Hint that progress was done.
     * @param progress The new total progress in percent.
     */
    void onProgressUpdate(int progress);
}
