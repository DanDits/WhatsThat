package dan.dit.whatsthat.testsubject;

import android.content.res.Resources;
import android.view.View;

import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.intro.Episode;
import dan.dit.whatsthat.testsubject.intro.EpisodeBuilder;
import dan.dit.whatsthat.testsubject.intro.Intro;
import dan.dit.whatsthat.testsubject.intro.QuestionEpisode;

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
        public List<Episode> makeMainIntroEpisodes(final Intro intro) {
            EpisodeBuilder builder = new EpisodeBuilder(intro);

            // general intro
            builder.setCurrentIcon(0);
            builder.nextEpisodes(intro.getResources().getStringArray(R.array.test_subject_0_0_intro_main));

            //dr kulg presentation
            builder.setCurrentIcon(R.drawable.intro_dr_kulg);
            builder.nextEpisodes(intro.getResources().getStringArray(R.array.test_subject_0_1_intro_main));

            // gender question, mandatory
            builder.setCurrentIcon(0);
            String[] questionEpisodeText = intro.getResources().getStringArray(R.array.test_subject_0_2_intro_main);
            builder.nextEpisode(new QuestionEpisode(intro,
                    true,
                    questionEpisodeText[0],
                    new int[] {R.string.intro_test_subject_gender_answer_male, R.string.intro_test_subject_gender_answer_female, R.string.intro_test_subject_gender_answer_whatever},
                    1,
                    new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.intro_answer1:
                            mTestSubject.setGender(TestSubject.GENDER_MALE);
                            break;
                        case R.id.intro_answer2:
                            mTestSubject.setGender(TestSubject.GENDER_FEMALE);
                            break;
                        case R.id.intro_answer3:
                            mTestSubject.setGender(TestSubject.GENDER_WHATEVER);
                            break;
                        case R.id.intro_answer4:
                            mTestSubject.setGender(TestSubject.GENDER_ALIEN);
                            break;
                    }
                    intro.nextEpisode();
                }
            }));
            if (questionEpisodeText.length > 1) {
                builder.nextEpisodes(questionEpisodeText, 1, questionEpisodeText.length);
            }

            // rest of the episodes
            builder.setCurrentIcon(0);
            builder.nextEpisodes(intro.getResources().getStringArray(R.array.test_subject_0_3_intro_main));
            return builder.build();

        }

        @Override
        public List<Episode> makeSubIntroEpisodes(final Intro intro) {
            if (mTextNuts.length < 8) {
                return null;
            }
            EpisodeBuilder builder = new EpisodeBuilder(intro);
            builder.setCurrentIcon(0);

            // Test: 13 is prime question, 0 to 3 and 4
            builder.nextEpisodes(mTextNuts, 0, 4);
            builder.nextEpisode(new QuestionEpisode(intro, false, mTextNuts[4], new int[]{R.string.intro_test_answer_yes, R.string.intro_test_answer_no, R.string.intro_test_answer_bullshit}, 0, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    intro.nextEpisode();
                }
            }));

            // in between texts and then chocolate, 5 to 6 and 7
            builder.nextEpisodes(mTextNuts, 5, 2);
            builder.setCurrentIcon(R.drawable.chocolate_thought);
            builder.nextEpisodes(mTextNuts, 7, 1);

            // rest of texts, 8 to end
            builder.setCurrentIcon(0);
            builder.nextEpisodes(mTextNuts, 8, mTextNuts.length);

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
            mTestSubject.addNewType(PracticalRiddleType.FLOW_INSTANCE);
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
