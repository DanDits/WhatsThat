package dan.dit.whatsthat.testsubject.intro;

/**
 * Created by daniel on 08.08.15.
 */
public class Episode {
    private final int mMessageResId;
    private final String mMessage;
    protected final Intro mIntro;
    private int mIcon;

    public Episode(Intro intro) {
        this(intro, null);
    }

    public Episode(Intro intro, String message) {
        mIntro = intro;
        mMessageResId = 0;
        mMessage = message;
        if (intro == null) {
            throw new IllegalArgumentException("No intro given.");
        }
    }

    public Episode(Intro intro, int messageResId) {
        mIntro = intro;
        mMessage = null;
        mMessageResId = messageResId;
        if (intro == null) {
            throw new IllegalArgumentException("No intro given.");
        }
    }

    public void setIcon(int icon) {
        mIcon = icon;
    }

    protected boolean isDone() {
        return true;
    }

    protected boolean isMandatory() {
        return false;
    }

    protected void start() {
        if (mMessage != null) {
            mIntro.applyMessage(mMessage);
        } else if (mMessageResId != 0) {
            mIntro.applyMessage(mMessageResId);
        } else {
            mIntro.applyMessage(0);
        }
        mIntro.applyIcon(mIcon);
    }

}
