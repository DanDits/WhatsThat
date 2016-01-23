package dan.dit.whatsthat.riddle.control;

import dan.dit.whatsthat.util.general.MathFunction;

/**
 * Created by daniel on 23.01.16.
 */
public class RiddleScoreConfig {
    private static final double MAXIMUM_EXPECTED_BASE_SCORE_FACTOR = 3.5;
    private final long mExpectedTimeNeeded;//ms
    private final MathFunction mFasterThanExpectedScore;
    private final MathFunction mSlowerThanExpectedScore;

    /**
     * Constructs a new RiddleScoreConfig that describes a way to obtain the base score earned
     * for solving a certain riddle type.
     * @param expectedTimeNeeded The expected (mean) time taken for solving this riddle type.
     *                           Should be positive, at least 1000ms. Measured in milliseconds.
     * @param expectedBaseScore The expected base score expected to be gained when solving took
     *                          the expected needed time. Should be non negative.
     * @param maxBaseScore The maximum score that can be earned for being very(!) fast. Will take
     *                     a value not smaller than the expected base score.
     */
    public RiddleScoreConfig(long expectedTimeNeeded, int expectedBaseScore, double maxBaseScore) {
        // ensure parameters are reasonable
        expectedTimeNeeded = Math.max(1000L, expectedTimeNeeded);
        expectedBaseScore = Math.max(expectedBaseScore, 0);
        maxBaseScore = Math.max(expectedBaseScore, maxBaseScore);
        // init score functions
        mExpectedTimeNeeded = expectedTimeNeeded;
        mFasterThanExpectedScore = new MathFunction.QuadraticInterpolation(0, maxBaseScore,
                expectedTimeNeeded, expectedBaseScore);
        mSlowerThanExpectedScore = new MathFunction.Max(Math.min(1, expectedBaseScore),
                new MathFunction.LinearInterpolation(expectedTimeNeeded, expectedBaseScore, 2
                        * expectedTimeNeeded, 0.5 * expectedBaseScore));
    }

    public RiddleScoreConfig(long expectedTimeNeeded, int expectedBaseScore) {
        this(expectedTimeNeeded, expectedBaseScore, expectedBaseScore *
                MAXIMUM_EXPECTED_BASE_SCORE_FACTOR);
    }

    public int getBaseScore(long timeNeeded) {
        return (int) (timeNeeded <= mExpectedTimeNeeded ?
            mFasterThanExpectedScore.evaluate(timeNeeded)
                : mSlowerThanExpectedScore.evaluate(timeNeeded));
    }
}
