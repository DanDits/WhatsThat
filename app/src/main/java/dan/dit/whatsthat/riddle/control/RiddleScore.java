package dan.dit.whatsthat.riddle.control;

/**
 * Created by daniel on 26.11.15.
 */
public class RiddleScore {
    private int mTotalScore;
    private int mBonus;
    private int mMultiplicator;

    public RiddleScore(int base, int multiplicator) {
        base = Math.max(0, base);
        multiplicator = Math.max(1, multiplicator);
        mTotalScore = base * multiplicator;
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
}
