package dan.dit.whatsthat.util;

/**
 * Created by daniel on 06.09.15.
 */
public abstract class SimpleInterpolation {

    public abstract double interpolate(double at);

    public static class LinearInterpolation extends  SimpleInterpolation {

        private final double mStartArg;
        private final double mStartValue;
        private final double mAscend;

        public LinearInterpolation(double startArg, double startValue, double endArg, double endValue) {
            mStartArg = startArg;
            mStartValue = startValue;
            mAscend = (endValue - mStartValue) / (endArg - mStartArg);
        }

        @Override
        public double interpolate(double at) {
            return mStartValue +  mAscend * (at - mStartArg);
        }
    }

    public static class QuadraticInterpolation extends SimpleInterpolation {
        private final double mA;
        private final double mExtremalValue;
        private final double mExtremalArg;

        public QuadraticInterpolation(double extremalArg, double extremalValue, double otherArg, double otherValue) {
            // a(x - mx)² + mv    , es gilt a(ox - mx)² + mv = ov also (ov - mv) / (ox - mx)²  = a
            mA = (otherValue - extremalValue) / ((otherArg - extremalArg) * (otherArg - extremalArg));
            mExtremalArg = extremalArg;
            mExtremalValue = extremalValue;
        }

        @Override
        public double interpolate(double at) {
            return mA * (at - mExtremalArg) * (at - mExtremalArg) + mExtremalValue;
        }
    }
}
