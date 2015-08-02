package dan.dit.whatsthat.riddle.types;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/** A riddle type that describes the content of a riddle. Used to specify the preference or refusal
 * of a riddle in a specific content. This affects images preferred or refused for the riddle game. Available are
 * contrast, which is an analysis of the brightness changes of the image; greyness which is an analysis of the average
 * greyness of each pixel. More types are planned and can be added freely. Images can use these types as preferences
 * to describe their content to access their content fast and make selection more easily.
 * Created by daniel on 01.04.15.
 */
public abstract class ContentRiddleType extends RiddleType {
    private static final Map<String, ContentRiddleType> ALL_TYPES = new HashMap<>();
    public static final ContentWeakContrast CONTRAST_WEAK_INSTANCE = new ContentWeakContrast();
    public static final ContentMediumContrast CONTRAST_MEDIUM_INSTANCE = new ContentMediumContrast();
    public static final ContentStrongContrast CONTRAST_STRONG_INSTANCE = new ContentStrongContrast();
    public static final ContentVeryGrey GREY_VERY_INSTANCE = new ContentVeryGrey();
    public static final ContentMediumGrey GREY_MEDIUM_INSTANCE = new ContentMediumGrey();
    public static final ContentLittleGrey GREY_LITTLE_INSTANCE = new ContentLittleGrey();
    private static final int INTEREST_VALUE = 2;

    protected int multiplier() {
        return 1;
    }

    public static class ContentMediumContrast extends ContentRiddleType {
        public static final String NAME = "ContentMediumContrast";

        @Override
        protected String getName() {return NAME;}
    }

    public static class ContentStrongContrast extends ContentRiddleType {
        public static final String NAME = "ContentStrongContrast";

        @Override
        protected String getName() {return NAME;}

        @Override
        protected int multiplier() {
            return 2;
        }
    }

    public static class ContentWeakContrast extends ContentRiddleType {
        public static final String NAME = "ContentWeakContrast";

        @Override
        protected String getName() {return NAME;}

        @Override
        protected int multiplier() {
            return 2;
        }
    }

    public static class ContentVeryGrey extends ContentRiddleType {
        public static final String NAME = "ContentVeryGrey";
        @Override
        protected String getName() {return NAME;}

        @Override
        protected int multiplier() {
            return 2;
        }
    }

    public static class ContentMediumGrey extends ContentRiddleType {
        public static final String NAME = "ContentMediumGrey";
        @Override
        protected String getName() {return NAME;}
    }

    public static class ContentLittleGrey extends ContentRiddleType {
        public static final String NAME = "ContentLittleGrey";
        @Override
        protected String getName() {return NAME;}

        @Override
        protected int multiplier() {
            return 2;
        }
    }

    @Override
    public int getInterestValue() {
        return INTEREST_VALUE * multiplier();
    }

    @Override
    protected char getTypePrefix() {
        return RiddleType.RIDDLE_TYPE_PREFIX_CONTENT;
    }

    public static ContentRiddleType getInstance(String fullName) {
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
