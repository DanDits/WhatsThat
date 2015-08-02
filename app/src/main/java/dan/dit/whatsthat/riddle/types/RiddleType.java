package dan.dit.whatsthat.riddle.types;

import android.text.TextUtils;
import android.util.Log;

/**
 * Defines an riddle type that identifies an attribute of a riddle or even the riddle game itself.
 * A riddle type is uniquely identified by its full name.which consists of its name with prefixes.
 * There are three major sub categories: Practical, for types that describe a riddle game, format, for types
 * that describe the format for a riddle and content, that describes the content of the riddle.
 * Created by daniel on 26.03.15.
 */
public abstract class RiddleType {
    private static final String RIDDLE_TYPE_PREFIX = "RT";
    protected static final char RIDDLE_TYPE_PREFIX_PRACTICAL = 'p';
    protected static final char RIDDLE_TYPE_PREFIX_FORMAT = 'f';
    protected static final char RIDDLE_TYPE_PREFIX_CONTENT ='c';
    protected static final int FULL_NAME_MIN_LENGTH = RIDDLE_TYPE_PREFIX.length() + 1;
    private static final int TYPE_PREFIX_INDEX = RIDDLE_TYPE_PREFIX.length();

    protected abstract char getTypePrefix();
    protected abstract String getName();
    protected abstract void registerType();

    private String mFullName;

    protected RiddleType() {
        mFullName = RIDDLE_TYPE_PREFIX + getTypePrefix() + getName();
        registerType();
    }

    /**
     * Returns the full name of the riddle type. The riddle can be retrieved
     * by this full name and is uniquely identified.
     * @return The full name.
     */
    public String getFullName() {
        return mFullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RiddleType) {
            return this.getFullName().equals(((RiddleType) other).getFullName());
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return getFullName().hashCode();
    }


    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the instance for the given full name.
     * @param fullName The full name.
     * @return Searches practical riddle types, format riddle types or content riddle types
     * for an instance matching the given full, depending on the prefix. Will be null for empty or invalid
     * full names.
     */
    public static RiddleType getInstance(String fullName) {
        if (TextUtils.isEmpty(fullName) || fullName.length() < FULL_NAME_MIN_LENGTH) {
            return null;
        }
        switch (fullName.charAt(TYPE_PREFIX_INDEX)) {
            case RIDDLE_TYPE_PREFIX_PRACTICAL:
                return PracticalRiddleType.getInstance(fullName);
            case RIDDLE_TYPE_PREFIX_FORMAT:
                return FormatRiddleType.getInstance(fullName);
            case RIDDLE_TYPE_PREFIX_CONTENT:
                return ContentRiddleType.getInstance(fullName);
            default:
                Log.e("Riddle", "Illegal riddle type prefix for getInstance: " + fullName);
                return null;
        }
    }

    /**
     * Returns a value that describes how valuable the interest in this type is.
     * Defined by subclasses to weight some types more than others.
     * @return A positive interest value.
     */
    public abstract int getInterestValue();

    /**
     * Returns the interest value if the givent ype is equal to this type, else zero.
     * @param preferredTypeOfImage The type to compare this type to.
     * @return The interest value or 0.
     */
    public final int getInterestValueIfEqual(RiddleType preferredTypeOfImage) {
        return equals(preferredTypeOfImage) ? getInterestValue() : 0;
    }
}
