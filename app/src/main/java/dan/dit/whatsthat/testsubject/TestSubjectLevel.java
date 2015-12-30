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

package dan.dit.whatsthat.testsubject;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.User;
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
    protected int[] mImageResId = new int[TestSubject.GENDERS_COUNT]; // indexed by gender
    protected int mRiddleSolvedCandy;
    protected String[] mTextMain;
    protected String[] mTextNuts;

    protected TestSubjectLevel(TestSubject testSubject) {
        mTestSubject = testSubject;
    }

    protected static TestSubjectLevel[] makeAll(TestSubject testSubject) {
        return new TestSubjectLevel[] {
                new TestSubjectLevel0(testSubject),
                new TestSubjectLevel1(testSubject),
                new TestSubjectLevel2(testSubject),
                new TestSubjectLevel3(testSubject),
                new TestSubjectLevel4(testSubject),
                new TestSubjectLevel5(testSubject),
                new TestSubjectLevel6(testSubject),
                new TestSubjectLevel7(testSubject)
                //new TestSubjectLevel8(testSubject),
                //new TestSubjectLevel9(testSubject)
        };
    }

    public Episode makeEpisodes(Intro intro) {
        EpisodeBuilder builder = new EpisodeBuilder(intro);
        makeMainIntroEpisodes(builder);
        makeSubIntroEpisodes(builder);
        return builder.build();
    }

    protected abstract void makeMainIntroEpisodes(EpisodeBuilder builder);

    public void makeSubIntroEpisodes(EpisodeBuilder builder) {
        if (mTextNuts != null && mTextNuts.length > 0) {
            int lastIndex = builder.getAll().size() - 1;
            builder.nextEpisodes("SA", mTextNuts);
            // make cyclic: last sub episode has first sub episode as child
            builder.getCurrentEpisode().addChild(builder.getAll().get(lastIndex + 1));
        }
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
        int gender = TestSubject.getInstance().getGender();
        if (gender == TestSubject.GENDER_NOT_CHOSEN) {
            return R.drawable.kid_abduction;
        }
        int resId = mImageResId[gender];
        if (resId == 0) {
            return R.drawable.kid_abduction; // fallback image in case some was not set
        }
        return resId;
    }

    public abstract double getLevelUpAchievementScoreFraction();


    private static class TestSubjectLevel0 extends TestSubjectLevel {

        protected TestSubjectLevel0(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {

            // general intro
            builder.setCurrentIcon(0)
            .nextEpisodes("M0", R.array.test_subject_0_0_intro_main)

            //dr kulg presentation
            .setCurrentIcon(R.drawable.intro_dr_kulg)
            .nextEpisodes("M1", R.array.test_subject_0_1_intro_main)

            // gender question, mandatory
            .setCurrentIcon(0)
            .nextEpisode(new QuestionEpisode("Q2",
                    builder.getIntro(),
                    true,
                    R.array.test_subject_0_2_intro_main,
                    new int[]{R.string.intro_test_subject_gender_answer_male, R.string.intro_test_subject_gender_answer_female, R.string.intro_test_subject_gender_answer_whatever},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            switch (answerIndex) {
                                case 0:
                                    mTestSubject.setGender(TestSubject.GENDER_MALE);
                                    break;
                                case 1:
                                    mTestSubject.setGender(TestSubject.GENDER_FEMALE);
                                    break;
                                case 2:
                                    mTestSubject.setGender(TestSubject.GENDER_WHATEVER);
                                    break;
                                case 3:
                                    mTestSubject.setGender(TestSubject.GENDER_ALIEN);
                                    break;
                            }
                            return QuestionEpisode.ANSWER_REACTION_NEXT_EPISODE;
                        }
                    }));

            // rest of the episodes
            builder.setCurrentIcon(0)
            .nextEpisodes("M3", R.array.test_subject_0_3_intro_main);

        }

        @Override
        public void makeSubIntroEpisodes(EpisodeBuilder builder) {
            int firstSubIndex = builder.getAll().size();
            builder.setCurrentIcon(0);

            // Test: 13 is prime question
            builder.nextEpisodes("S0", R.array.test_subject_0_0_intro_nuts);
            builder.nextEpisode(new QuestionEpisode("Q1", builder.getIntro(), false,
                            R.array.test_subject_0_1_intro_nuts,
                            new int[]{R.string.intro_test_answer_yes, R.string.intro_test_answer_no, R.string.intro_test_answer_bullshit},
                            new QuestionEpisode.OnQuestionAnsweredCallback() {
                                @Override
                                public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                                    return QuestionEpisode.ANSWER_REACTION_NEXT_EPISODE;
                                }
                            })
            );

            // in between texts and then chocolate
            builder.nextEpisodes("S2", R.array.test_subject_0_2_intro_nuts)
                .nextEpisodes("S3", R.array.test_subject_0_3_intro_nuts)
                .setCurrentIcon(R.drawable.chocolate_thought)
                .nextEpisodes("S4", R.array.test_subject_0_4_intro_nuts)
                .setCurrentIcon(0)
                .nextEpisodes("S5", R.array.test_subject_0_5_intro_nuts)
                    .getCurrentEpisode().addChild(builder.getAll().get(firstSubIndex));
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_0_name;
            mIntelligenceResId = R.string.test_subject_0_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid00;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem00;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani00;
            mTextNuts = res.getStringArray(R.array.test_subject_0_0_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_0_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
            mTestSubject.addNewType(PracticalRiddleType.CIRCLE_INSTANCE);
            mTestSubject.saveTypes();
        }

        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.0;
        } // level is reached at
        // game start
    }

    private static class TestSubjectLevel1 extends TestSubjectLevel {

        protected TestSubjectLevel1(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_1_0_intro_main);

            builder.nextEpisode(new QuestionEpisode("Q1", builder.getIntro(), false, R.array
                    .test_subject_1_1_intro_main,
                    new int[]{R.string.intro_test_answer_chocolate, R.string
                            .intro_test_answer_donut, R.string.intro_test_answer_cookie},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_1_2a_intro_main)
                    .addAnswer(R.array.test_subject_1_2b_intro_main)
                    .addAnswer(R.array.test_subject_1_2c_intro_main));
            builder.joinCurrentChildrenToNext();


            builder.nextEpisodes("M3", R.array.test_subject_1_3_intro_main);
            builder.nextEpisodes("M4", R.array.test_subject_1_4_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_1_name;
            mIntelligenceResId = R.string.test_subject_1_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid01;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem01;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani01;
            mTextNuts = res.getStringArray(R.array.test_subject_1_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_1_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }

        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.25;
        }
    }
    private static class TestSubjectLevel2 extends TestSubjectLevel {

        protected TestSubjectLevel2(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_2_0_intro_main);
            builder.nextEpisodes("M1", R.array.test_subject_2_1_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_2_name;
            mIntelligenceResId = R.string.test_subject_2_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid02;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem02;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani02;
            mTextNuts = res.getStringArray(R.array.test_subject_2_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_2_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.4;
        }
    }

    private static class TestSubjectLevel3 extends TestSubjectLevel {

        protected TestSubjectLevel3(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_3_0_intro_main);
            builder.nextEpisodes("M1", R.array.test_subject_3_1_intro_main);
            builder.nextEpisodes("M2", R.array.test_subject_3_2_intro_main);
            builder.nextEpisodes("M3", R.array.test_subject_3_3_intro_main);
            builder.nextEpisodes("M4", R.array.test_subject_3_4_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_3_name;
            mIntelligenceResId = R.string.test_subject_3_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid03;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem03;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani03;
            mTextNuts = res.getStringArray(R.array.test_subject_3_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_3_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.55;
        }
    }
    
    private static class TestSubjectLevel4 extends TestSubjectLevel {

        protected TestSubjectLevel4(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_4_0_intro_main);
            builder.nextEpisodes("M1", R.array.test_subject_4_1_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_4_name;
            mIntelligenceResId = R.string.test_subject_4_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid04;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem04;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani04;
            mTextNuts = res.getStringArray(R.array.test_subject_4_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_4_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.65;
        }
    }

    private static class TestSubjectLevel5 extends TestSubjectLevel {

        protected TestSubjectLevel5(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_5_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_5_name;
            mIntelligenceResId = R.string.test_subject_5_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid05;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem05;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani05;
            mTextMain = res.getStringArray(R.array.test_subject_5_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_5_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_5_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.675;
        }
    }

    private static class TestSubjectLevel6 extends TestSubjectLevel {

        protected TestSubjectLevel6(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_6_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_6_name;
            mIntelligenceResId = R.string.test_subject_6_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid06;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem06;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani06;
            mTextMain = res.getStringArray(R.array.test_subject_6_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_6_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_6_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.7;
        }
    }

    private static class TestSubjectLevel7 extends TestSubjectLevel {

        protected TestSubjectLevel7(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_7_intro_main);
        }
        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_7_name;
            mIntelligenceResId = R.string.test_subject_7_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid07;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem07;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani07;
            mTextMain = res.getStringArray(R.array.test_subject_7_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_7_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_7_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.75;
        }
    }

    private static class TestSubjectLevel8 extends TestSubjectLevel {

        protected TestSubjectLevel8(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_8_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_8_name;
            mIntelligenceResId = R.string.test_subject_8_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid08;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem08;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani08;
            mTextMain = res.getStringArray(R.array.test_subject_8_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_8_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_8_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.8;
        }
    }

    private static class TestSubjectLevel9 extends TestSubjectLevel {

        protected TestSubjectLevel9(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_9_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_9_name;
            mIntelligenceResId = R.string.test_subject_9_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid09;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem09;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani09;
            mTextMain = res.getStringArray(R.array.test_subject_9_intro_main);
            mTextNuts = res.getStringArray(R.array.test_subject_9_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_9_riddle_solved_candy;
        }

        @Override
        protected void onLeveledUp() {
            User.getInstance().givePermission(User.PERMISSION_WORKSHOP);
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.9;
        }
    }
}
