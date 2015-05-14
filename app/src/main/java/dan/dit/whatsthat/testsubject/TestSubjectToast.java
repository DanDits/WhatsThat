package dan.dit.whatsthat.testsubject;

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
    public int mDuration;

    public TestSubjectToast(int gravity, int offsetX, int offsetY, int iconId, int textId, int duration) {
        mTextResId = textId;
        mIconResId = iconId;
        mGravity = gravity;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mDuration = duration;
    }
}
