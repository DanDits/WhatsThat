package dan.dit.whatsthat.riddle.types;

import android.content.res.Resources;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementCircle;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementDice;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementFlow;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementJumper;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementLazor;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementMemory;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementSnow;
import dan.dit.whatsthat.riddle.achievement.holders.AchievementTriangle;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.games.RiddleCircle;
import dan.dit.whatsthat.riddle.games.RiddleDeveloper;
import dan.dit.whatsthat.riddle.games.RiddleDice;
import dan.dit.whatsthat.riddle.games.RiddleFlow;
import dan.dit.whatsthat.riddle.games.RiddleGame;
import dan.dit.whatsthat.riddle.games.RiddleJumper;
import dan.dit.whatsthat.riddle.games.RiddleLazor;
import dan.dit.whatsthat.riddle.games.RiddleMemory;
import dan.dit.whatsthat.riddle.games.RiddleSnow;
import dan.dit.whatsthat.riddle.games.RiddleTorchlight;
import dan.dit.whatsthat.riddle.games.RiddleTriangle;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Static helper class that holds static PracticalRiddleType implementations
 * for RiddleGames. Each supported type is a singleton whose instance is held and returned by the
 * PracticalRiddleType class.
 * Created by daniel on 18.05.15.
 */
public class Types {
    public static final int SCORE_SIMPLE = 1;
    public static final int SCORE_MEDIUM = 2;
    public static final int SCORE_HARD = 3;
    public static final int SCORE_VERY_HARD = 4;
    public static final int SCORE_ULTRA = 5;

    private Types() {} // do not instantiate

    /**
     * The matching type for RiddleCircle.
     */
    public static class Circle extends PracticalRiddleType {
        public static final String NAME = "Circle";
        public TypeAchievementHolder mHolder = new AchievementCircle(this);

        @Override
        protected String getName() {return NAME;}

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleCircle(riddle, image, bitmap, res, config, listener);
        }

        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }

        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            // if image prefers square format and is strong or medium in contrast we like it
            interest += typeToCheck.getInterestValueIfEqual(FormatRiddleType.SQUARE_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_STRONG_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_MEDIUM_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.GREY_VERY_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.GREY_MEDIUM_INSTANCE);
            return interest;
        }

        @Override
        public int getRefusalValue(RiddleType preferredTypeOfImage) {
            int refusal = super.getRefusalValue(preferredTypeOfImage);
            refusal += preferredTypeOfImage.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return refusal;
        }

        @Override
        public int getIconResId() {
            return R.drawable.icon_circle;
        }

        @Override
        public boolean enforcesBitmapAspectRatio() {return true;}

        @Override
        public int getNameResId() {
            return R.string.riddle_type_circle;
        }

        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_circle_explanation;
        }
        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_circle_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 1;
        }
        @Override
        public int getTotalAvailableHintsCount() {
            return 9;
        }

        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(0);
            costs.add(5);
            costs.add(5);
            costs.add(15);
            costs.add(15);
            costs.add(30);
            return costs;
        }

        @Override
        public int getBaseScore() {
            return SCORE_SIMPLE;
        }

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_circle_advertising;
        }
    }

    /**
     * The matching type for RiddleSnow.
     */
    public static class Snow extends PracticalRiddleType {
        public static final String NAME = "Snow";
        public TypeAchievementHolder mHolder = new AchievementSnow(this);
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_snow_advertising;
        }

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleSnow(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getIconResId() {
            return R.drawable.icon_snow;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_snow;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_snow_explanation;
        }
        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }
        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_snow_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 2;
        }
        @Override
        public int getTotalAvailableHintsCount() {
            return 6;
        }
        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(0);
            costs.add(15);
            return costs;
        }

        @Override
        public int getBaseScore() {
            return SCORE_MEDIUM;
        }
    }

    /**
     * The matching type for RiddleDice.
     */
    public static class Dice extends PracticalRiddleType {
        public static final String NAME = "Dice";
        public TypeAchievementHolder mHolder = new AchievementDice(this);
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_dice_advertising;
        }

        @Override
        public boolean enforcesBitmapAspectRatio() {
            return true;
        }

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleDice(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(FormatRiddleType.SQUARE_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.GREY_LITTLE_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return interest;
        }

        @Override
        public int getIconResId() {
            return R.drawable.icon_dice;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_dice;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_dice_explanation;
        }

        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }
        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_dice_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 2;
        }
        @Override
        public int getTotalAvailableHintsCount() {return 9;}

        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(0);
            costs.add(15);
            costs.add(15);
            costs.add(15);
            costs.add(25);
            return costs;
        }

        @Override
        public int getBaseScore() {
            return SCORE_VERY_HARD;
        }
    }

    /**
     * The matching type for RiddleTriangle.
     */
    public static class Triangle extends PracticalRiddleType {
        public static final String NAME = "Triangle";
        public TypeAchievementHolder mHolder = new AchievementTriangle(this);
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_triangle_advertising;
        }
        @Override
        public boolean enforcesBitmapAspectRatio() {return true;}

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleTriangle(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.GREY_LITTLE_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return interest;
        }

        @Override
        public int getIconResId() {
            return R.drawable.icon_triangle;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_triangle;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_triangle_explanation;
        }
        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }
        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_triangle_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 1;
        }
        @Override
        public int getTotalAvailableHintsCount() {return 6;
        }
        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(20);
            costs.add(10);
            costs.add(15);
            return costs;
        }
        @Override
        public int getBaseScore() {
            return SCORE_SIMPLE;
        }
    }

    /**
     * The matching type for RiddleJumper.
     */
    public static class Jumper extends PracticalRiddleType {
        public static final String NAME = "Jumper";
        public static final double BITMAP_ASPECT_RATIO = 5. / 4.;
        public TypeAchievementHolder mHolder = new AchievementJumper(this);

        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_jumper_advertising;
        }
        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleJumper(riddle, image, bitmap, res, config, listener);
        }

        @Override
        public int getIconResId() {
            return R.drawable.icon_jumprun;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_jumper;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_jumper_explanation;
        }

        @Override
        public double getSuggestedBitmapAspectRatio() {
            return BITMAP_ASPECT_RATIO;
        }
        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }
        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_jumper_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 2;
        }
        @Override
        public int getTotalAvailableHintsCount() {
            return 12;
        }
        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(0);
            costs.add(5);
            costs.add(10);
            costs.add(15);
            costs.add(15);
            costs.add(20);
            costs.add(25);
            costs.add(30);
            costs.add(40);
            costs.add(45);
            costs.add(50);
            return costs;
        }
        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return interest;
        }
        @Override
        public int getBaseScore() {
            return SCORE_ULTRA;
        }
    }

    /**
     * The matching type for RiddleMemory.
     */
    public static class Memory extends PracticalRiddleType {
        public static final String NAME = "Memory";
        private TypeAchievementHolder mHolder = new AchievementMemory(this);
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_memory_advertising;
        }
        @Override
        public TypeAchievementHolder getAchievementHolder() {
        return mHolder;
    }
        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleMemory(riddle, image, bitmap, res, config, listener);
        }

        @Override
        public int getIconResId() {
            return R.drawable.icon_memory;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_memory;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_memory_explanation;
        }

        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_memory_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 1;
        }
        @Override
        public int getTotalAvailableHintsCount() {return 5;}
        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(10);
            costs.add(10);
            costs.add(30);
            costs.add(30);
            return costs;
        }

        @Override
        public int getBaseScore() {
            return SCORE_HARD;
        }

    }

    /**
     * The matching type for RiddleDeveloper. Not to be really used as the game does nothing
     * and is meant for testing purposes and stuff only available to the developers.
     */
    public static class Developer extends PracticalRiddleType {
        public static final String NAME = "Developer";
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_developer_advertising;
        }

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleDeveloper(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getIconResId() {
            return R.drawable.icon_plain;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_developer;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_developer_explanation;
        }

        @Override
        public int getBaseScore() {
            return SCORE_SIMPLE;
        }
    }

    /**
     * New testing project by FD
     */
    public static class Torchlight extends PracticalRiddleType {
        public static final String NAME = "Torchlight";
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_torchlight_advertising;
        }
        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleTorchlight(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getIconResId() {
            return R.drawable.icon_plain;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_torchlight;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_torchlight_explanation;
        }

        @Override
        public int getBaseScore() {
            return SCORE_SIMPLE;
        }
    }

    /**
     * The riddle type for the Flow riddle.
     */
    public static class Flow extends PracticalRiddleType {
        public static final String NAME = "Flow";
        private TypeAchievementHolder mHolder = new AchievementFlow(this);

        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_flow_advertising;
        }
        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }
        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleFlow(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getIconResId() {
            return R.drawable.icon_flow;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_flow;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_flow_explanation;
        }

        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_STRONG_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_MEDIUM_INSTANCE);
            return interest;
        }

        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber) {
            String[] hints = res.getStringArray(R.array.riddle_type_flow_hints);
            if (hintNumber >= 0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {
            return 1;
        }
        @Override
        public int getTotalAvailableHintsCount() {
            return 4;
        }
        public List<Integer> getHintCosts() {
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(20);
            costs.add(30);
            return costs;
        }
        @Override
        public int getBaseScore() {
            return SCORE_MEDIUM;
        }
    }

    /**
     * The riddle type for the Lazor riddle.
     */
    public static class Lazor extends PracticalRiddleType {
        public static final String NAME = "Lazor";
        private static final double BITMAP_ASPECT_RATIO = 5. / 4.;
        private TypeAchievementHolder mHolder = new AchievementLazor(this);

        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_lazor_advertising;
        }
        @Override
        public double getSuggestedBitmapAspectRatio() {
            return BITMAP_ASPECT_RATIO;
        }
        @Override
        public boolean enforcesBitmapAspectRatio() {return true;}
        @Override
        public TypeAchievementHolder getAchievementHolder() {
            return mHolder;
        }

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleLazor(riddle, image, bitmap, res, config, listener);
        }
        @Override
        public int getIconResId() {
            return R.drawable.icon_lazor;
        }
        @Override
        public int getNameResId() {
            return R.string.riddle_type_lazor;
        }
        @Override
        public int getExplanationResId() {
            return R.string.riddle_type_lazor_explanation;
        }

        @Override
        public CharSequence getRiddleHint(Resources res, int hintNumber){
            String[] hints = res.getStringArray(R.array.riddle_type_lazor_hints);
            if (hintNumber >=0 && hintNumber < hints.length) {
                return hints[hintNumber];
            }
            return null;
        }
        @Override
        public int getAvailableHintsAtStartCount() {return 1;}
        @Override
        public int getTotalAvailableHintsCount() {return 3;}
        public List<Integer> getHintCosts(){
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(30);
            costs.add(50);
            return costs;
        }

        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(FormatRiddleType.LANDSCAPE_INSTANCE);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return interest;
        }

        @Override
        public int getBaseScore() {
            return SCORE_HARD;
        }
    }
}
