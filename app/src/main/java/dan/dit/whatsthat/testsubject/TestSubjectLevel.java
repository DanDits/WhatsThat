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
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.intro.Episode;
import dan.dit.whatsthat.testsubject.intro.EpisodeBuilder;
import dan.dit.whatsthat.testsubject.intro.GeneralStartingEpisode;
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
    protected int[] mBaseImageResId = new int[TestSubject.GENDERS_COUNT]; // indexed by gender
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

    /**
     * Returns the basic testsubject image without fancy markers and level indicators.
     * @return The basic testsubject image resource id, if not specified the default image
     * resource id.
     */
    public int getBaseImageResourceId() {
        int gender = TestSubject.getInstance().getGender();
        if (gender == TestSubject.GENDER_NOT_CHOSEN) {
            return R.drawable.kid_abduction;
        }
        int resId = mBaseImageResId[gender];
        if (resId == 0) {
            return getImageResourceId(); // default to the image resource
        }
        return resId;
    }

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

    public Intro makeIntro(Intro intro) {
        Resources res = intro.getResources();

        // Create the running text
        SpannableStringBuilder longDescription = new SpannableStringBuilder();
        longDescription.append(res.getString(R.string.intro_test_subject_name));
        int start = longDescription.length();
        longDescription.append(res.getString(mNameResId));
        longDescription.setSpan(new StyleSpan(Typeface.ITALIC), start,
                longDescription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        longDescription.append('\t');
        longDescription.append(res.getString(R.string.intro_test_subject_estimated_intelligence));
        start = longDescription.length();
        longDescription.append(res.getString(mIntelligenceResId));
        longDescription.setSpan(new StyleSpan(Typeface.ITALIC), start,
                longDescription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);



        TextView subjectDescr = ((TextView) intro.findViewById(R.id.intro_subject_descr));
        subjectDescr.setText(longDescription);
        subjectDescr.setVisibility(View.INVISIBLE);


        new GeneralStartingEpisode(intro, res.getString(R.string.intro_starting_episode, res
                .getString(mNameResId)), this).start();
        if (intro.getCurrentEpisode() == null) {
            intro.nextEpisode(); // if this level is loaded for the first time we need to set the initial episode
        } else {
            intro.startUnmanagedEpisode(intro.getCurrentEpisode());
        }
        return intro;
    }


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
            .nextEpisode(new QuestionEpisode("MQ2",
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
            builder.nextEpisode(new QuestionEpisode("SQ1", builder.getIntro(), false,
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

            builder.nextEpisode(new QuestionEpisode("MQ1", builder.getIntro(), false, R.array
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

            builder.nextEpisode(new QuestionEpisode("MQ4", builder.getIntro(), true, R.array
                    .test_subject_1_4_intro_main,
                    new int[]{R.string.intro_test_answer_minus18, R.string.intro_test_answer_9,
                            R.string.intro_test_answer_256, R.string.intro_test_answer_217341},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_1_5a_intro_main)
                    .addAnswer(R.array.test_subject_1_5a_intro_main)
                    .addAnswer(R.array.test_subject_1_5a_intro_main)
                    .addAnswer(R.array.test_subject_1_5b_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisodes("M6", R.array.test_subject_1_6_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_1_name;
            mIntelligenceResId = R.string.test_subject_1_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid01;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem01;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani01;
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid00;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem00;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani01_base;
            mTextNuts = res.getStringArray(R.array.test_subject_1_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_1_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 2);
        }

        @Override
        protected void onLeveledUp() {
        }

        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.09;
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
            builder.nextEpisodes("M2", R.array.test_subject_2_2_intro_main);


            builder.nextEpisode(new QuestionEpisode("MQ3", builder.getIntro(), false, R.array
                    .test_subject_2_3_intro_main,
                    new int[]{R.string.intro_test_answer_6, R.string.intro_test_answer_potatosalad},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_2_4a_intro_main)
                    .addAnswer(R.array.test_subject_2_4b_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisodes("M5", R.array.test_subject_2_5_intro_main);

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
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 3);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.15;
        }
    }

    private static class TestSubjectLevel3 extends TestSubjectLevel {

        protected TestSubjectLevel3(TestSubject testSubject) {
            super(testSubject);
        }

        private int mCorrectQuestionAnsweredCount;
        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_3_0_intro_main);

            builder.nextEpisode(new QuestionEpisode("MQ1", builder.getIntro(), false, R.array
                    .test_subject_3_1_intro_main,
                    new int[]{R.string.intro_test_answer_3, R.string.intro_test_answer_4,
                            R.string.intro_test_answer_5, R.string.intro_test_answer_6},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            if (answerIndex == 2) {
                                mCorrectQuestionAnsweredCount++;
                            }
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_3_2a_intro_main)
                    .addAnswer(R.array.test_subject_3_2a_intro_main)
                    .addAnswer(R.array.test_subject_3_2b_intro_main)
                    .addAnswer(R.array.test_subject_3_2a_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisode(new QuestionEpisode("MQ3", builder.getIntro(), false, R.array
                    .test_subject_3_3_intro_main,
                    new int[]{R.string.intro_test_answer_3, R.string.intro_test_answer_4,
                            R.string.intro_test_answer_5, R.string.intro_test_answer_6},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            if (answerIndex == 2) {
                                mCorrectQuestionAnsweredCount++;
                            }
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_3_4a_intro_main)
                    .addAnswer(R.array.test_subject_3_4a_intro_main)
                    .addAnswer(R.array.test_subject_3_4b_intro_main)
                    .addAnswer(R.array.test_subject_3_4a_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisode(new QuestionEpisode("MQ5", builder.getIntro(), false, R.array
                    .test_subject_3_5_intro_main,
                    new int[]{R.string.intro_test_answer_3, R.string.intro_test_answer_4,
                            R.string.intro_test_answer_5, R.string.intro_test_answer_6},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            if (answerIndex == 3) {
                                mCorrectQuestionAnsweredCount++;
                                if (mCorrectQuestionAnsweredCount ==3 ){
                                    return 4;
                                }
                            }
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_3_6a_intro_main)
                    .addAnswer(R.array.test_subject_3_6a_intro_main)
                    .addAnswer(R.array.test_subject_3_6a_intro_main)
                    .addAnswer(R.array.test_subject_3_6c_intro_main)
                    .addAnswer(R.array.test_subject_3_6b_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisodes("M7", R.array.test_subject_3_7_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_3_name;
            mIntelligenceResId = R.string.test_subject_3_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid03;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem03;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani03;
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid02;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem02;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani03_base;
            mTextNuts = res.getStringArray(R.array.test_subject_3_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_3_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 4);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.18;
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
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid02;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem02;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani04_base;
            mTextNuts = res.getStringArray(R.array.test_subject_4_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_4_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 5);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.25;
        }
    }

    private static class TestSubjectLevel5 extends TestSubjectLevel {

        protected TestSubjectLevel5(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_5_0_intro_main);
            builder.nextEpisodes("M1", R.array.test_subject_5_1_intro_main);
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_5_name;
            mIntelligenceResId = R.string.test_subject_5_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid05;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem05;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani05;
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid02;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem02;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani05_base;
            mTextNuts = res.getStringArray(R.array.test_subject_5_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_5_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 6);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.35;
        }
    }

    private static class TestSubjectLevel6 extends TestSubjectLevel {

        protected TestSubjectLevel6(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_6_0_intro_main);
            builder.nextEpisodes("M1", R.array.test_subject_6_1_intro_main);

            builder.nextEpisode(new QuestionEpisode("MQ3", builder.getIntro(), false, R.array
                    .test_subject_6_2_intro_main,
                    new int[]{R.string.intro_test_answer_radiation, R.string.intro_test_answer_eh,
                            R.string.intro_test_answer_potatosalad},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_6_3a_intro_main)
                    .addAnswer(R.array.test_subject_6_3b_intro_main)
                    .addAnswer(R.array.test_subject_6_3c_intro_main));
            builder.joinCurrentChildrenToNext();

            builder.nextEpisodes("M4", R.array.test_subject_6_4_intro_main);

            builder.nextEpisode(new QuestionEpisode("MQ5", builder.getIntro(), false, R.array
                    .test_subject_6_5_intro_main,
                    new int[]{R.string.intro_test_answer_no, R.string.intro_test_answer_nope,
                            R.string.intro_test_answer_eh, R.string.intro_test_answer_lolno},
                    new QuestionEpisode.OnQuestionAnsweredCallback() {
                        @Override
                        public int onQuestionAnswered(QuestionEpisode episode, int answerIndex) {
                            return answerIndex;
                        }
                    })
                    .addAnswer(R.array.test_subject_6_intro_nuts)); //TODO same way to answer like "Test: 13 is prime question"?
            builder.joinCurrentChildrenToNext();
        }

        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_6_name;
            mIntelligenceResId = R.string.test_subject_6_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid06;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem06;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani06;
            mTextNuts = res.getStringArray(R.array.test_subject_6_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_6_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 7);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.5;
        }
    }

    private static class TestSubjectLevel7 extends TestSubjectLevel {

        protected TestSubjectLevel7(TestSubject testSubject) {
            super(testSubject);
        }

        @Override
        public void makeMainIntroEpisodes(EpisodeBuilder builder) {
            builder.nextEpisodes("M0", R.array.test_subject_7_0_intro_main);
        }
        @Override
        protected void applyLevel(Resources res) {
            mNameResId = R.string.test_subject_7_name;
            mIntelligenceResId = R.string.test_subject_7_int;
            mImageResId[TestSubject.GENDER_MALE] = R.drawable.kid07;
            mImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem07;
            mImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani07;
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid06;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem06;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani07_base;
            mTextNuts = res.getStringArray(R.array.test_subject_7_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_7_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 8);
        }

        @Override
        protected void onLeveledUp() {
            User.getInstance().givePermission(User.PERMISSION_WORKSHOP);
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.60;
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
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid06;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem06;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani08_base;
            mTextNuts = res.getStringArray(R.array.test_subject_8_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_8_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 8);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.7;
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
            mBaseImageResId[TestSubject.GENDER_MALE] = R.drawable.kid06;
            mBaseImageResId[TestSubject.GENDER_FEMALE] = R.drawable.kid_fem06;
            mBaseImageResId[TestSubject.GENDER_WHATEVER] = R.drawable.kid_ani09;//FIXME change
            mTextNuts = res.getStringArray(R.array.test_subject_9_intro_nuts);
            mRiddleSolvedCandy = R.array.test_subject_9_riddle_solved_candy;
            mTestSubject.ensureSkipableGames(TestSubject.DEFAULT_SKIPABLE_GAMES + 8);
        }

        @Override
        protected void onLeveledUp() {
        }
        @Override
        public double getLevelUpAchievementScoreFraction() {
            return 0.8;
        }
    }
}
