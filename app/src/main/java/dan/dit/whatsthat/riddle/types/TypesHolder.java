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

package dan.dit.whatsthat.riddle.types;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import dan.dit.whatsthat.riddle.control.RiddleScoreConfig;
import dan.dit.whatsthat.riddle.games.RiddleCircle;
import dan.dit.whatsthat.riddle.games.RiddleDeveloper;
import dan.dit.whatsthat.riddle.games.RiddleDice;
import dan.dit.whatsthat.riddle.games.RiddleFlow;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.games.RiddleJumper;
import dan.dit.whatsthat.riddle.games.RiddleLazor;
import dan.dit.whatsthat.riddle.games.RiddleMemory;
import dan.dit.whatsthat.riddle.games.RiddleSnow;
import dan.dit.whatsthat.riddle.games.RiddleTorchlight;
import dan.dit.whatsthat.riddle.games.RiddleTriangle;
import dan.dit.whatsthat.util.general.PercentProgressListener;

/**
 * Static helper class that holds static PracticalRiddleType implementations
 * for RiddleGames. Each supported type is a singleton whose instance is held and returned by the
 * PracticalRiddleType class.
 * Created by daniel on 18.05.15.
 */
public class TypesHolder {
    public static final int SCORE_MINIMAL = 1;
    public static final int SCORE_SIMPLE = 2;
    public static final int SCORE_MEDIUM = 3;
    public static final int SCORE_HARD = 4;
    public static final int SCORE_VERY_HARD = 5;
    public static final int SCORE_ULTRA = 6;
    private static final long SEC_TO_MS = 1000L;

    private TypesHolder() {} // do not instantiate

    /**
     * The matching type for RiddleCircle.
     */
    public static class Circle extends PracticalRiddleType {
        public static final String NAME = "Circle";

        protected Circle() {
            super(new RiddleScoreConfig(25 * SEC_TO_MS, SCORE_SIMPLE));
            mHolder = new AchievementCircle(this);
        }

        @Override
        protected String getName() {return NAME;}

        @Override
        public RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener) {
            return new RiddleCircle(riddle, image, bitmap, res, config, listener);
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
            costs.add(0);
            costs.add(5);
            costs.add(10);
            costs.add(15);
            costs.add(20);
            costs.add(30);
            return costs;
        }

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_circle_advertising;
        }

        @Override
        public int getId() {
            return 1;
        }
    }

    /**
     * The matching type for RiddleSnow.
     */
    public static class Snow extends PracticalRiddleType {
        public static final String NAME = "Snow";

        protected Snow() {
            super(new RiddleScoreConfig(60 * SEC_TO_MS, SCORE_MEDIUM));
            mHolder = new AchievementSnow(this);
        }

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
            costs.add(25);
            costs.add(25);
            costs.add(30);
            return costs;
        }

        @Override
        public int getId() {
            return 2;
        }
    }

    /**
     * The matching type for RiddleDice.
     */
    public static class Dice extends PracticalRiddleType {
        public static final String NAME = "Dice";

        protected Dice() {
            super(new RiddleScoreConfig(90 * SEC_TO_MS, SCORE_VERY_HARD));
            mHolder = new AchievementDice(this);
        }

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
            costs.add(30);
            costs.add(35);
            costs.add(35);
            costs.add(30);
            return costs;
        }

        @Override
        public int getId() {
            return 3;
        }
    }

    /**
     * The matching type for RiddleTriangle.
     */
    public static class Triangle extends PracticalRiddleType {
        public static final String NAME = "Triangle";

        protected Triangle() {
            super(new RiddleScoreConfig(22 * SEC_TO_MS, SCORE_SIMPLE));
            mHolder = new AchievementTriangle(this);
        }

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
            costs.add(30);
            costs.add(35);
            costs.add(25);
            return costs;
        }

        @Override
        public int getId() {
            return 4;
        }
    }

    /**
     * The matching type for RiddleJumper.
     */
    public static class Jumper extends PracticalRiddleType {
        public static final String NAME = "Jumper";
        public static final double BITMAP_ASPECT_RATIO = 5. / 4.;
        protected Jumper() {
            super(new RiddleScoreConfig(70 * SEC_TO_MS, SCORE_HARD));
            mHolder = new AchievementJumper(this);
        }

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
            costs.add(10);
            costs.add(20);
            costs.add(20);
            costs.add(35);
            costs.add(40);
            costs.add(40);
            costs.add(50);
            costs.add(70);
            return costs;
        }
        @Override
        public int getInterestValue(RiddleType typeToCheck) {
            int interest = super.getInterestValue(typeToCheck);
            interest += typeToCheck.getInterestValueIfEqual(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            return interest;
        }

        @Override
        public int getId() {
            return 5;
        }
    }

    /**
     * The matching type for RiddleMemory.
     */
    public static class Memory extends PracticalRiddleType {
        public static final String NAME = "Memory";
        protected Memory() {
            super(new RiddleScoreConfig(90 * SEC_TO_MS, SCORE_VERY_HARD));
            mHolder = new AchievementMemory(this);
        }
        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_memory_advertising;
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
            costs.add(20);
            costs.add(30);
            costs.add(40);
            return costs;
        }

        @Override
        public int getId() {
            return 6;
        }

    }

    /**
     * The matching type for RiddleDeveloper. Not to be really used as the game does nothing
     * and is meant for testing purposes and stuff only available to the developers.
     */
    public static class Developer extends PracticalRiddleType {
        public static final String NAME = "Developer";
        protected Developer() {
            super(new RiddleScoreConfig(1000L, 0));
        }
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
        public int getId() {
            return 7;
        }
    }

    /**
     * New testing project by FD
     */
    public static class Torchlight extends PracticalRiddleType {
        public static final String NAME = "Torchlight";
        protected Torchlight() {
            super(new RiddleScoreConfig(1000L, 0));
        }
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
        public int getId() {
            return 8;
        }
    }

    /**
     * The riddle type for the Flow riddle.
     */
    public static class Flow extends PracticalRiddleType {
        public static final String NAME = "Flow";
        protected Flow() {
            super(new RiddleScoreConfig(40 * SEC_TO_MS, SCORE_MEDIUM));
            mHolder = new AchievementFlow(this);
        }

        @Override
        protected String getName() {return NAME;}

        @Override
        public int getAdvertisingResId() {
            return R.string.riddle_type_flow_advertising;
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
            costs.add(10);
            costs.add(15);
            return costs;
        }

        @Override
        public int getId() {
            return 9;
        }
    }

    /**
     * The riddle type for the Lazor riddle.
     */
    public static class Lazor extends PracticalRiddleType {
        public static final String NAME = "Lazor";
        private static final double BITMAP_ASPECT_RATIO = 5. / 4.;
        protected Lazor() {
            super(new RiddleScoreConfig(60 * SEC_TO_MS, SCORE_HARD));
            mHolder = new AchievementLazor(this);
        }

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
        public int getAvailableHintsAtStartCount() {return 2;}
        @Override
        public int getTotalAvailableHintsCount() {return 7;}
        public List<Integer> getHintCosts(){
            List<Integer> costs = new ArrayList<>(getTotalAvailableHintsCount());
            costs.add(0);
            costs.add(10);
            costs.add(15);
            costs.add(35);
            costs.add(20);
            costs.add(30);
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
        public int getId() {
            return 10;
        }
    }
}
