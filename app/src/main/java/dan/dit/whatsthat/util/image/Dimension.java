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
}
