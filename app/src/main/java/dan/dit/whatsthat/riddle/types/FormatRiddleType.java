package dan.dit.whatsthat.riddle.types;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A riddle type that describes the format of a riddle. Used to specify the preference or refusal
 * of a riddle in a specific format, will affect which images are preferred or refused. Available are
 * Square (width=height), Portrait(height>width) and Landscape(height<width).
 * Created by daniel on 01.04.15.
 */
public abstract class FormatRiddleType extends RiddleType {
    private static final Map<String, FormatRiddleType> ALL_TYPES = new HashMap<>();
    public static final FormatSquare SQUARE_INSTANCE = new FormatSquare();
    public static final FormatLandscape LANDSCAPE_INSTANCE = new FormatLandscape();
    public static final FormatPortrait PORTRAIT_INSTANCE = new FormatPortrait();

    private static final int INTEREST_VALUE = 2;

    public static class FormatSquare extends FormatRiddleType {
        public static final String NAME = "FormatSquare";

        @Override
        protected String getName() {return NAME;}
    }

    public static class FormatLandscape extends FormatRiddleType {
        public static final String NAME = "FormatLandscape";

        @Override
        protected String getName() {return NAME;}
    }

    public static class FormatPortrait extends FormatRiddleType {
        public static final String NAME = "FormatPortrait";

        @Override
        protected String getName() {return NAME;}
    }

    @Override
    public int getInterestValue() {
        return INTEREST_VALUE;
    }

    @Override
    protected char getTypePrefix() {
        return RiddleType.RIDDLE_TYPE_PREFIX_FORMAT;
    }

    public static FormatRiddleType getInstance(String fullName) {
        if (TextUtils.isEmpty(fullName) || fullName.length() < FULL_NAME_MIN_LENGTH) {
            return null;
        }
        return ALL_TYPES.get(fullName);
    }

    @Override
    protected void registerType() {
        ALL_TYPES.put(getFullName(), this);
    }
}
