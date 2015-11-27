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

package dan.dit.whatsthat.util.general;

/**
 * Mathematical helper class to create continuous functions that can be evaluated at every given
 * double value. The functions are immutable objects. The functions may be any kind of function, most likely
 * though this will be linear or quadratic functions.
 * Created by daniel on 06.09.15.
 */
public abstract class MathFunction {

    /**
     * Evaluates this function at the given argument value.
     * @param at The argument value to evaluate.
     * @return The function value at the given value.
     */
    public abstract double evaluate(double at);

    /**
     * A modified basic sinus function. That is f(x)= a*sin(b*x)+c. The period is 2 * PI / b.
     */
    public static class Sinus extends MathFunction {
        private final double mAmplitude;
        private final double mPeriodFactor;
        private final double mOffset;

        /**
         * Creates a new sinus function. Will take values between (-amplitue and +amplitude) +
         * offset for any given input.
         * @param amplitude The amplitude.
         * @param period Sinus period.
         * @param offset Offset of the sinus values.
         */
        public Sinus(double amplitude, double period, double offset) {
            mAmplitude = amplitude;
            mPeriodFactor = 2. * Math.PI / period;
            mOffset = offset;
        }

        @Override
        public double evaluate(double at) {
            return mAmplitude * Math.sin(mPeriodFactor * at) + mOffset;
        }
    }
    /**
     * A linear one dimensional function. That is f(x) = a*x + c for some double
     * values a and c.
     */
    public static class LinearInterpolation extends MathFunction {

        private final double mStartArg;
        private final double mStartValue;
        private final double mAscend;

        /**
         * Creates a linear function interpolating the given pair of argument-value pairs.
         * The function will be linear and pass through the points (startArg,startValue) and
         * (endArg, endValue). The order and signum of all values is arbitrary.
         * @param startArg The first argument value.
         * @param startValue The first target value.
         * @param endArg The second argument value.
         * @param endValue The second target value.
         */
        public LinearInterpolation(double startArg, double startValue, double endArg, double endValue) {
            mStartArg = startArg;
            mStartValue = startValue;
            mAscend = (endValue - mStartValue) / (endArg - mStartArg);
        }

        @Override
        public double evaluate(double at) {
            return mStartValue +  mAscend * (at - mStartArg);
        }

        public static double evaluate(double startArg, double startValue, double endArg, double
                endValue, double at) {
            return startValue + (endValue - startValue) / (endArg - startArg) * (at - startArg);
        }

        public static float evaluate(float startArg, float startValue, float endArg, float
                endValue,float at) {
            return startValue + (endValue - startValue) / (endArg - startArg) * (at - startArg);
        }
    }

    /**
     * A quadratic one dimensional function. That is f(x)=a*x² + b*x + c for some
     * double values a, b and c.
     */
    public static class QuadraticInterpolation extends MathFunction {
        private final double mA;
        private final double mExtremalValue;
        private final double mExtremalArg;

        /**
         * Creates a quadratic function from the following constraints:
         * f'(extremalArg)=0, that means the extremal value will be at the extremalArg value.
         * f(extremalArg)=extremalValue and f(otherArg)=otherValue for interpolating.
         * @param extremalArg The first argument value that is also the point of a local minimum or maximum.
         * @param extremalValue The evaluated value at the given extremal argument value.
         * @param otherArg The second argument value.
         * @param otherValue The second target value.
         */
        public QuadraticInterpolation(double extremalArg, double extremalValue, double otherArg, double otherValue) {
            // a(x - mx)² + mv    , es gilt a(ox - mx)² + mv = ov also (ov - mv) / (ox - mx)²  = a
            mA = (otherValue - extremalValue) / ((otherArg - extremalArg) * (otherArg - extremalArg));
            mExtremalArg = extremalArg;
            mExtremalValue = extremalValue;
        }

        @Override
        public double evaluate(double at) {
            return mA * (at - mExtremalArg) * (at - mExtremalArg) + mExtremalValue;
        }

        public static float evaluate(float extremalArg, float extremalValue, float otherArg,
                                     float otherValue, float at) {
            return (otherValue - extremalValue) / ((otherArg - extremalArg) * (otherArg - extremalArg))
                    * (at - extremalArg) * (at - extremalArg) + extremalValue;
        }

        public static double evaluate(double extremalArg, double extremalValue, double otherArg,
                                      double otherValue, double at) {
            return (otherValue - extremalValue) / ((otherArg - extremalArg) * (otherArg - extremalArg))
                    * (at - extremalArg) * (at - extremalArg) + extremalValue;
        }
    }
}
