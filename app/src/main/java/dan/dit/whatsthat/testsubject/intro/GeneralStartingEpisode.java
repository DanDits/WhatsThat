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
        super("General", intro, message);
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
        mIntroKid.setImageResource(mLevel.getBaseImageResourceId());

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

        kidStuff.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIntro.findViewById(R.id.intro_subject_descr).setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
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
