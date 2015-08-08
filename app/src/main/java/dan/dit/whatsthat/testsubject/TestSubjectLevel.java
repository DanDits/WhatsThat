package dan.dit.whatsthat.testsubject;

import android.content.res.Resources;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.intro.Episode;
import dan.dit.whatsthat.testsubject.intro.EpisodeBuilder;
import dan.dit.whatsthat.testsubject.intro.GeneralStartingEpisode;
import dan.dit.whatsthat.testsubject.intro.Intro;

/**
 * Created by daniel on 08.08.15.
 */
public abstract class TestSubjectLevel {
    public static final int LEVEL_NONE = -1;
    protected TestSubject mTestSubject;
    protected int mNameResId;
    protected int mIntelligenceResId;
    protected int mImageResId;
    protected int mRiddleSolvedCandy;
    protected String[] mTextMain;
    protected String[] mTextNuts;

    protected TestSubjectLevel(TestSubject testSubject) {
        mTestSubject = testSubject;
    }

    protected static TestSubjectLevel[] makeAll(TestSubject testSubject) {
        return new TestSubjectLevel[] {
                new TestSubjectLevel0(testSubject),
                new TestSubjectLevel1(testSubject)};
    }

    public abstract List<Episode> makeMainIntroEpisodes(Intro intro);

    public List<Episode> makeSubIntroEpisodes(Intro intro) {
        if (mTextNuts != null && mTextNuts.length > 0) {
            EpisodeBuilder builder = new EpisodeBuilder(intro);
            builder.nextEpisodes(mTextNuts, 0, mTextNuts.length);
            return builder.build();
        }
        return null;
    }

    /**
     * Invoked whenever necessary to set and load required fields for this level.
     */
    protected abstract void applyLevel(Resources res);

    /**
     * Invoked when the level is reached for the first time.
     */
    protected abstract void onLeveledUp();

    public int getImageResourceId() {
        return mImageResId;
    }


    private static class TestSubjectLevel0 extends TestSubjectLevel {

        protected TestSubjectLevel0(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public List<Episode> makeMainIntroEpisodes(Intro intro) {
            EpisodeBuilder builder = new EpisodeBuilder(intro);
            builder.setCurrentIcon(0);
            builder.nextEpisodes(mTextMain, 0, 6); // 0 to 5
            builder.setCurrentIcon(R.drawable.intro_dr_gluk);
            builder.nextEpisodes(mTextMain, 6, 2); // 6 to 7
            // now add mandatory episode where we ask for and set gender
            builder.setCurrentIcon(0);
            builder.nextEpisodes(mTextMain, 8, 1); // 8
            builder.setCurrentIcon(0);
            builder.nextEpisodes(mTextMain, 9, mTextMain.length); // 9 to rest
            return builder.build();
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_0_name;
            mIntelligenceResId = R.string.test_subject_0_int;
            mImageResId = R.drawable.kid0;
            mTextMain = res.getStringArray(R.array.test_subject_0_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_0_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_0_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
            mTestSubject.addNewType(PracticalRiddleType.CIRCLE_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.SNOW_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.TRIANGLE_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.DICE_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.JUMPER_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.MEMORY_INSTANCE);
            mTestSubject.addNewType(PracticalRiddleType.TORCHLIGHT_INSTANCE);
            //mTestSubject.addNewType(PracticalRiddleType.DEVELOPER_INSTANCE);
            mTestSubject.saveTypes();
        }
    }

    private static class TestSubjectLevel1 extends TestSubjectLevel {

        protected TestSubjectLevel1(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public List<Episode> makeMainIntroEpisodes(Intro intro) {
            EpisodeBuilder builder = new EpisodeBuilder(intro);
            builder.nextEpisodes(mTextMain, 0, mTextMain.length);
            return builder.build();
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_1_name;
            mIntelligenceResId = R.string.test_subject_1_int;
            mImageResId = R.drawable.kid;
            mTextMain = res.getStringArray(R.array.test_subject_1_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_1_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_1_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {

        }
    }

}
