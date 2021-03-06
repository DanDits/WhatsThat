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

package dan.dit.whatsthat.util.image;

/**
 * Mutable helper class for holding a width and height pair of ints.
 * These values are non-negative.
 * Created by daniel on 22.04.15.
 */
public class Dimension {
    private int mWidth;
    private int mHeight;

    public Dimension(int width, int height) {
        set(width, height);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidthForDensity(int screenDensity) {
        return (int) ImageUtil.convertDpToPixel(mWidth, screenDensity);
    }

    public int getHeightForDensity(int screenDensity) {
        return (int) ImageUtil.convertDpToPixel(mHeight, screenDensity);
    }

    public void set(int width, int height) {
        mWidth = width;
        mHeight = height;
        if (mWidth < 0 || mHeight < 0) {
            throw new IllegalArgumentException("Negative dimension given." + width + "x" + height);
        }
    }

    public Dimension(Dimension toCopy) {
        mWidth = toCopy.mWidth;
        mHeight = toCopy.mHeight;
    }

    public double getRatio() {
        return mWidth / ((double) mHeight);
    }

    public void fitInsideWithRatio(double ratio) {
        int maxWidth = (int) (mHeight * ratio);
        int maxHeight = (int) (mWidth / ratio);

        if (mWidth  > maxWidth) {
            mWidth = maxWidth;
        } else {
            // now width <= maxWidth and maxHeight <= height
            mHeight = maxHeight;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Dimension) {
            return mWidth == ((Dimension) other).mWidth && mHeight == ((Dimension) other).mHeight;
        } else {
            return super.equals(other);
        }
    }

    @Override
    public String toString() {
        return "Dim: " + mWidth + "x" + mHeight;
    }

    @Override
    public int hashCode() {
        return mWidth + (mWidth + mHeight)*(mWidth + mHeight + 1) / 2; // cantor's bijection (though not for values of zero)
    }

    /**
     * Ensures that both dimensions are divisible by the given positive divisors and greater than zero.
     * @param widthDivisor The width divisor.
     * @param heightDivisor The height divisor.
     * @param preferSmaller If true then the smaller resulting dimension will be smaller than previously
     *                      if this is possible.
     */
    public void ensureDivisibleBy(int widthDivisor, int heightDivisor, boolean preferSmaller) {
        // make sure that both dimension are divisible by the given divisor and greater than zero
        int widthDelta = -mWidth;
        if (preferSmaller) {
            widthDelta = -(mWidth % widthDivisor);
        }
        if (mWidth + widthDelta <= 0) {
            widthDelta = widthDivisor + mWidth % widthDivisor;
        }
        mWidth += widthDelta;

        int heightDelta = -mHeight;
        if (preferSmaller) {
            heightDelta = -(mHeight % heightDivisor);
        }
        if (mHeight + heightDelta <= 0) {
            heightDelta = heightDivisor + mHeight % heightDivisor;
        }
        mHeight += heightDelta;
    }
}
