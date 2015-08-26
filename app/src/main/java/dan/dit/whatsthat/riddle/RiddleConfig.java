package dan.dit.whatsthat.riddle;

import android.util.DisplayMetrics;
import android.util.Log;

import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleGame;
import dan.dit.whatsthat.riddle.achievement.AchievementDataRiddleType;
import dan.dit.whatsthat.riddle.games.RiddleControllerFactory;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;

/**
 * Configures a riddle game, providing additional (required) hints how it should behave and look like.
 * Also provides metadata hints produces by the RiddleMaker. This is a structure class and therefore
 * some data might not be initialized (properly).
 * Created by daniel on 23.04.15.
 */
public class RiddleConfig {
    public boolean mHintImageReused; // hints that the image was reused by the same type, bad!
    public boolean mHintImageReusedOtherType; // hints that the image was already used by another type
    public boolean mHintImageRefused; // gives a hint if this image's riddle was refused by the riddle type
    public boolean mHintNoImageSecrets; // if true this allows the RiddleHintView to show image details like source and name
    public int mScreenDensity = DisplayMetrics.DENSITY_HIGH; // default hdpi

    public int mHeight; // initialized on initBitmap call
    public int mWidth;
    public RiddleControllerFactory mControllerFactory;
    public AchievementDataRiddleGame mAchievementGameData;
    public AchievementDataRiddleType mAchievementTypeData;

    public RiddleConfig(int width, int height) {
        mWidth = width;
        mHeight = height;
        if (width <= 0) {
            Log.e("Riddle", "Setting negative or zero width for RiddleConfig: " + width);
            mWidth = 480;
        }
        if (height <= 0) {
            Log.e("Riddle", "Setting negative or zero height for RiddleConfig: " + height);
            mHeight = 480;
        }
    }

    public void setAchievementData(PracticalRiddleType type) {
        mAchievementGameData = type.getAchievementDataGame();
        mAchievementTypeData = type.getAchievementData(null);
    }


}
