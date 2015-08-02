package dan.dit.whatsthat.riddle.types;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleConfig;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.achievement.holders.TypeAchievementHolder;
import dan.dit.whatsthat.riddle.games.RiddleGame;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;

/**
 * Created by daniel on 01.04.15.
 */
public abstract class PracticalRiddleType extends RiddleType {
    private static final Map<String, PracticalRiddleType> ALL_TYPES = new HashMap<>();
    private static final int INTEREST_VALUE = 10;
    public static final Types.Circle CIRCLE_INSTANCE = new Types.Circle();
    public static final Types.Snow SNOW_INSTANCE = new Types.Snow();
    public static final Types.Dice DICE_INSTANCE = new Types.Dice();
    public static final Types.Developer DEVELOPER_INSTANCE = new Types.Developer();
    public static final Types.Triangle TRIANGLE_INSTANCE = new Types.Triangle();
    public static final Types.Jumper JUMPER_INSTANCE = new Types.Jumper();
    public static final Types.Memory MEMORY_INSTANCE = new Types.Memory();

    private SoftReference<Drawable> mIcon;
    private volatile AchievementDataRiddleType mAchievementData;
    private final AchievementDataRiddleGame mAchievementDataGame = new AchievementDataRiddleGame(this);

    public Drawable getIcon(Resources res) {
        if (mIcon != null)  {
            Drawable icon = mIcon.get();
            if (icon != null) {
                return icon;
            }
        }
        if (res != null) {
            Drawable icon = res.getDrawable(getIconResId());
            Log.d("Riddle", "Remaking icon for type: " + getName());
            mIcon = new SoftReference<>(icon);
            return icon;
        }
        return null;
    }

    public abstract int getIconResId();

    public AchievementDataRiddleType getAchievementData(AchievementManager manager) {
        if (mAchievementData != null || manager == null) {
            return mAchievementData;
        }
        synchronized (this) {
            if (mAchievementData == null) {
                try {
                    mAchievementData = new AchievementDataRiddleType(this, manager);
                } catch (CompactedDataCorruptException e) {
                    Log.e("Achievement", "Failed creating data for riddle type " + this + ": " + e);
                    mAchievementData = new AchievementDataRiddleType(this, manager, true);
                }
            }
        }
        return mAchievementData;
    }

    public double getSuggestedBitmapAspectRatio() {
        return getSuggestedCanvasAspectRatio(); // by default fit the bitmap into the canvas
    }

    public double getSuggestedCanvasAspectRatio() {
        return 1.; // square by default
    }

    /**
     * If the type enforces the ratio given by getSuggestedBitmapAspectRatio.
     * It has no real power on the aspect ratio of the given bitmap but the system
     * will take try its best to fit the biggest bitmap with this ratio inside the available canvas.
     * @return If the aspect ratio is not only meant as a suggestion.
     */
    public boolean enforcesBitmapAspectRatio() {
        return false;
    }

    /**
     * If this riddle type requires the device's orientation sensor, that is
     * the accelerometer and magnetometer. These, especially latter, isn't available
     * on all devices.
     * <br>If the type requires these sensors and they can't be used, the riddle
     * type must not be chosen.
     * @return If the orientation sensor is used by the riddle.
     */
    public boolean requiresOrientationSensor() {
        return false;
    }

    public abstract int getNameResId();

    public abstract int getExplanationResId();

    public int calculateRefusal(Image image) {
        List<RiddleType> refused = image.getRefusedRiddleTypes();
        int refusal = 0;
        if (refused != null) {
            for (RiddleType r : refused) {
                refusal += getInterestValue(r); // the image hates something which the riddle likes
            }
        }
        List<RiddleType> preferred= image.getPreferredRiddleTypes();
        if (preferred != null) {
            for (RiddleType p : preferred) {
                refusal += getRefusalValue(p); // the image is something the riddle doesn't like
            }
        }
        return refusal;
    }

    public int calculateInterest(Image image) {
        List<RiddleType> preferred= image.getPreferredRiddleTypes();
        int interest = 0;
        if (preferred != null) {
            for (RiddleType p : preferred) {
                interest += getInterestValue(p); // they like the same things
            }
        }
        List<RiddleType> refused = image.getRefusedRiddleTypes();
        if (refused != null) {
            for (RiddleType r : refused) {
                interest += getRefusalValue(r); // they hate the same things
            }
        }
        return interest;
    }

    public abstract RiddleGame makeRiddle(Riddle riddle, Image image, Bitmap bitmap, Resources res, RiddleConfig config, PercentProgressListener listener);

    @Override
    protected char getTypePrefix() {
        return RiddleType.RIDDLE_TYPE_PREFIX_PRACTICAL;
    }

    @Override
    protected void registerType() {
        ALL_TYPES.put(getFullName(), this);
    }

    public static List<PracticalRiddleType> getAll() {
        List<PracticalRiddleType> types = new ArrayList<>(ALL_TYPES.values());
        TestSubject.sortTypes(types);
        return types;
    }

    public static PracticalRiddleType getInstance(String fullName) {
        if (TextUtils.isEmpty(fullName) || fullName.length() < FULL_NAME_MIN_LENGTH) {
            return null;
        }
        return ALL_TYPES.get(fullName);
    }

    public int getInterestValue(RiddleType inType) {
        return getInterestValueIfEqual(inType); // always interested in itself
    }

    public int getRefusalValue(RiddleType refusedTypeOfImage) {return getInterestValueIfEqual(DEVELOPER_INSTANCE);} // we don't want images marked for developers

    @Override
    public final int getInterestValue() {
        return INTEREST_VALUE;
    }

    public @NonNull AchievementDataRiddleGame getAchievementDataGame() {
        return mAchievementDataGame;
    }
    public TypeAchievementHolder getAchievementHolder() {
        return null;
    }

    public View getGameWelcomeView(Context context) {
        return null;
    }
    public CharSequence getRiddleHint(Resources res, int hintNumber) {
        return null;
    }
    public int getAvailableHintsAtStartCount() {
        return 0;
    }
    public int getTotalAvailableHintsCount() {
        return 0;
    }

    public List<Integer> getHintCosts() {
        return null;
    }
}