package dan.dit.whatsthat.util.image;

import android.graphics.Color;

/**
 * Created by daniel on 03.07.15.
 */
public abstract class ColorMetric {
    public abstract double getDistance(int color1, int color2, boolean useAlpha);
    public abstract double maxValue(boolean useAlpha);

    public static class Euclid2 extends ColorMetric {	/**
         * This value indicates the greatest possible value for two colors
         * if alpha is not used. Equal to 3*255*255.
         */
        private static final double GREATEST_VALUE_NO_ALPHA = 3 * 255 * 255;

        /** This value indicates the greatest possible value for two colors
         * if alpha is used. Equal to 4*255*255.
         */
        private static final double GREATEST_VALUE_ALPHA = 4 * 255 * 255;
        public static final Euclid2 INSTANCE = new Euclid2();
        private Euclid2() {}

        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            int result = 0;
            int currColValue1;
            int currColValue2;

            currColValue1 = Color.red(color1);
            currColValue2 = Color.red(color2);
            result += (currColValue1 - currColValue2) * (currColValue1 - currColValue2);

            currColValue1 = Color.green(color1);
            currColValue2 = Color.green(color2);
            result += (currColValue1 - currColValue2) * (currColValue1 - currColValue2);

            currColValue1 = Color.blue(color1);
            currColValue2 = Color.blue(color2);
            result += (currColValue1 - currColValue2) * (currColValue1 - currColValue2);

            if (useAlpha) {
                currColValue1 = Color.alpha(color1);
                currColValue2 = Color.alpha(color2);
                result += (currColValue1 - currColValue2) * (currColValue1 - currColValue2);
            }
            return result;
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? GREATEST_VALUE_ALPHA : GREATEST_VALUE_NO_ALPHA;
        }
    }

    public static class Absolute extends ColorMetric {
        private static final Absolute INSTANCE = new Absolute();
        private static final double GREATEST_VALUE_NO_ALPHA = 3 * 255;
        private static final double GREATEST_VALUE_ALPHA = 4 * 255;
        private Absolute() {}
        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return Math.abs(Color.red(color1) - Color.red(color2))
                    + Math.abs(Color.green(color1) - Color.green(color2))
                    + Math.abs(Color.blue(color1) - Color.blue(color2))
                    + (useAlpha ? Math.abs(Color.alpha(color1) - Color.alpha(color2)) : 0);

        }

        @Override
        public double maxValue(boolean useAlpha) {
            return useAlpha ? GREATEST_VALUE_ALPHA : GREATEST_VALUE_NO_ALPHA;
        }
    }

    public static class Greyness extends ColorMetric {
        private static final Greyness INSTANCE = new Greyness();
        private Greyness() {}
        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            return Math.abs(ColorAnalysisUtil.getGreyness(Color.red(color1), Color.green(color1), Color.blue(color1))
                    - ColorAnalysisUtil.getGreyness(Color.red(color2), Color.green(color2), Color.blue(color2)));
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return 1.0;
        }
    }

    public static class Brightness extends  ColorMetric {
        private static final Brightness INSTANCE = new Brightness();
        private Brightness() {}
        @Override
        public double getDistance(int color1, int color2, boolean useAlpha) {
            if (useAlpha) {
                return Math.abs(ColorAnalysisUtil.getBrightnessNoAlpha(color1) - ColorAnalysisUtil.getBrightnessNoAlpha(color2));
            } else {
                return Math.abs(ColorAnalysisUtil.getBrightnessWithAlpha(color1) - ColorAnalysisUtil.getBrightnessWithAlpha(color2));
            }
        }

        @Override
        public double maxValue(boolean useAlpha) {
            return 1.0;
        }
    }
}
