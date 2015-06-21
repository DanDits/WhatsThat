package dan.dit.whatsthat.testsubject;

import android.content.Context;

import com.github.johnpersano.supertoasts.SuperToast;

/**
 * Created by daniel on 11.05.15.
 */
public class TestSubjectToast {
    public int mTextResId;
    public int mIconResId;
    public SuperToast.IconPosition mIconPosition;
    public int mGravity;
    public int mOffsetX;
    public int mOffsetY;
    public long mDuration;
    public CharSequence mText;
    public SuperToast.Animations mAnimations;
    public int mBackground;
    public int mTextColor;

    public TestSubjectToast(int gravity, int offsetX, int offsetY, int iconId, int textId, int duration) {
        mTextResId = textId;
        mIconResId = iconId;
        mGravity = gravity;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mDuration = duration;
    }

    public SuperToast makeSuperToast(Context context) {
        if (context == null) {
            return null;
        }
        SuperToast superToast = new SuperToast(context);
        if (mTextResId != 0) {
            superToast.setText(context.getResources().getText(mTextResId));
        } else if (mText != null) {
            superToast.setText(mText);
        }
        if (mAnimations != null) {
            superToast.setAnimations(mAnimations);
        }
        if (mBackground != 0) {
            superToast.setBackground(mBackground);
        }
        if (mIconResId != 0) {
            superToast.setIcon(mIconResId, mIconPosition == null ? SuperToast.IconPosition.LEFT : mIconPosition);
        }
        if (mTextColor != 0) {
            superToast.setTextColor(mTextColor);
        }
        superToast.setGravity(mGravity, mOffsetX, mOffsetY);
        superToast.setDuration((int) mDuration);
        return superToast;
    }

    @Override
    public String toString() {
        return "Text: " + mText + " text id " + mTextResId;
    }
}
