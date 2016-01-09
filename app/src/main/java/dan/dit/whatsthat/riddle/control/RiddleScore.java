package dan.dit.whatsthat.riddle.control;

import dan.dit.whatsthat.riddle.types.Types;

/**
 * Created by daniel on 26.11.15.
 */
public class RiddleScore {
    private int mTotalScore;
    private int mBonus;
    private int mMultiplicator;

    public RiddleScore(int base, int multiplicator) {
        base = Math.max(0, base);
        mMultiplicator = Math.max(1, multiplicator);
        mTotalScore = base * mMultiplicator;
    }

    public static class NoBonus extends RiddleScore {
        public NoBonus(int base, int multiplicator) {
            super(base, multiplicator);
        }

        @Override
        public RiddleScore addBonus(int bonus) {
            return this; // do not add any bonus
        }
    }

    public static class NullRiddleScore extends NoBonus {
        public static final NullRiddleScore INSTANCE = new NullRiddleScore();
        private NullRiddleScore() {
            super(0, 1);
        }

    }

    public RiddleScore addBonus(int bonus) {
        bonus = Math.max(0, bonus);
        mBonus += bonus;
        mTotalScore += bonus;
        return this;
    }

    public int getTotalScore() {
        return mTotalScore;
    }

    public boolean hasBonus() {
        return mBonus > 0;
    }

    public int getMultiplicator() {
        return mMultiplicator;
    }

    public int getBonus() {
        return mBonus;
    }

    public static class SimpleNoBonus extends NoBonus {

        public SimpleNoBonus() {
            super(Types.SCORE_SIMPLE, 1);
        }
    }
}
