package dan.dit.whatsthat.riddle.control;

/**
 * Created by daniel on 26.11.15.
 */
public class RiddleScore {
    private final int mBase;
    private int mBonus;
    private int mMultiplicator;

    private RiddleScore(int base, int multiplicator) {
        mBase = Math.max(0, base);
        mMultiplicator = Math.max(1, multiplicator);
    }

    private RiddleScore addBonus(int bonus) {
        bonus = Math.max(0, bonus);
        mBonus += bonus;
        return this;
    }

    public final int getTotalScore() {
        return mMultiplicator * (mBase + mBonus);
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

    public interface Rewardable {
        Rewardable addBonus(int bonusDelta);
    }

    public static class Builder implements Rewardable {
        private int mBase;
        private int mBonus;
        private int mMultiplicator;

        Builder setBase(int base) {
            mBase = base;
            return this;
        }

        @Override
        public Builder addBonus(int bonusDelta) {
            mBonus += bonusDelta;
            return this;
        }

        Builder setMultiplicator(int multiplicator) {
            mMultiplicator = multiplicator;
            return this;
        }

        public RiddleScore build() {
            return new RiddleScore(mBase, mMultiplicator).addBonus(mBonus);
        }
    }

    public static class NoBonusBuilder extends Builder {
        @Override
        public Builder addBonus(int bonusDelta) {
            // do nothing
            return this;
        }
    }
}
