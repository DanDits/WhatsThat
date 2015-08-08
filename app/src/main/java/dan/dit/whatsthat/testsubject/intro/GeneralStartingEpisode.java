package dan.dit.whatsthat.testsubject.intro;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubjectLevel;

/**
 * Created by daniel on 08.08.15.
 */
public class GeneralStartingEpisode extends Episode {

    private final TestSubjectLevel mLevel;
    private final ImageView mIntroKid;
    private View mIntroAbduction;

    public GeneralStartingEpisode(Intro intro, String message, TestSubjectLevel level) {
        super(intro, message);
        mLevel = level;
        mIntroKid = (ImageView) intro.findViewById(R.id.intro_subject);
        mIntroAbduction = intro.findViewById(R.id.intro_abduction);
    }

    private void startAnimation() {
        final long fallDownDuration = 4000;
        final long fallDownLiftDelta = 500;
        final long liftDuration = 12000;
        final long suckInDelta = -500;
        final long suckInDuration = 600;
        mIntroKid.setImageResource(mLevel.getImageResourceId());

        AlphaAnimation alphaAnimation = new AlphaAnimation(0.f, 1.f);
        alphaAnimation.setInterpolator(new AccelerateInterpolator(3));
        alphaAnimation.setDuration(liftDuration);

        AnimationSet kidStuff = new AnimationSet(false);
        RotateAnimation rot = new RotateAnimation(0.f, 100.f, Animation.RELATIVE_TO_SELF, 0.8f, Animation.RELATIVE_TO_SELF, 1.f);
        //rot.setInterpolator(new BounceInterpolator());
        rot.setDuration(fallDownDuration);
        TranslateAnimation move = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.2f, Animation.RELATIVE_TO_PARENT, -0.15f, //x
                Animation.RELATIVE_TO_PARENT,-0.f, Animation.RELATIVE_TO_PARENT, -0.7f); //y
        move.setStartOffset(fallDownLiftDelta);
        move.setDuration(liftDuration);

        ScaleAnimation s = new ScaleAnimation(1.f, 0, 1f, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        s.setInterpolator(new AccelerateInterpolator(1.5f));
        s.setStartOffset(liftDuration + suckInDelta);
        s.setDuration(suckInDuration);
        RotateAnimation rotSpinning = new RotateAnimation(0.f, 360.f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        s.setInterpolator(new AccelerateInterpolator(2.5f));
        rotSpinning.setStartOffset(liftDuration + suckInDelta);
        rotSpinning.setDuration(suckInDuration);

        kidStuff.addAnimation(s);
        kidStuff.addAnimation(rotSpinning);
        kidStuff.addAnimation(rot);
        kidStuff.addAnimation(move);
        kidStuff.setFillAfter(true);

        mIntroAbduction.setVisibility(View.VISIBLE);
        mIntroAbduction.startAnimation(alphaAnimation);
        mIntroKid.startAnimation(kidStuff);
    }

    @Override
    public void start() {
        super.start();
        startAnimation();
    }
}
