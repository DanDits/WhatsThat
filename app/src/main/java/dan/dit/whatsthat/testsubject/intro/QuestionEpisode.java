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
import android.view.ViewGroup;
import android.widget.Button;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 10.08.15.
 */
public class QuestionEpisode extends Episode implements Intro.OnEpisodeSkippedListener, View.OnClickListener {
    public static final int ANSWER_REACTION_NOTHING = -1;
    public static final int ANSWER_REACTION_NEXT_EPISODE = -2;

    private final int[] mAnswers;
    private final OnQuestionAnsweredCallback mCallback;
    private boolean mVisible;
    private boolean mCompleted;
    private ViewGroup mAnswersContainer;
    private final boolean mMandatory;

    public QuestionEpisode addAnswer(int strArrayResId) {
        Episode answer = new Episode("Answer" + getEpisodeKey() + getChildrenCount(), mIntro, mIntro
                .getResources()
                .getStringArray(strArrayResId));
        addChild(answer);
        return this;
    }

    public interface OnQuestionAnsweredCallback {
        int onQuestionAnswered(QuestionEpisode episode, int answerIndex);
    }
    public QuestionEpisode(String episodeKey, Intro intro, boolean mandatory, int messageArrayId,
                           int[] answersResId, OnQuestionAnsweredCallback callback) {
        super(episodeKey, intro, intro.getResources().getStringArray(messageArrayId));
        mMandatory = mandatory;
        mAnswers = answersResId;
        mCallback = callback;
        mAnswersContainer = (ViewGroup) intro.findViewById(R.id.intro_answers_container);
    }

    @Override
    public void onEpisodeSkipped(Episode skipped) {
        if (mVisible && skipped == this) {
            if (!hasNextMessage()) {
                stopQuestions();
            }
        }
    }


    protected void init(String key) {
        mCurrMessageIndex = 0;
    }

    @Override
    public boolean isDone() {
        return !mVisible || mCompleted || hasNextMessage();
    }

    @Override
    public boolean isMandatory() {
        return mMandatory && !mCompleted;
    }

    public void stopQuestions() {
        if (!mVisible) {
            return;
        }
        mIntro.removeOnEpisodeSkippedListener(this);
        mVisible = false;
        if (mAnswersContainer != null) {
            mAnswersContainer.setVisibility(View.INVISIBLE);
            for (int i = 0; i < mAnswersContainer.getChildCount(); i++) {
                Button child = (Button) mAnswersContainer.getChildAt(i);
                child.setOnClickListener(null);
            }
        }
    }

    @Override
    public void start() {
        if (mAnswersContainer != null && !mVisible) {
            mIntro.addOnEpisodeSkippedListener(this);
            super.start();
            mVisible = true;
            if (!mCompleted) {
                for (int i = 0; i < mAnswersContainer.getChildCount(); i++) {
                    Button child = (Button) mAnswersContainer.getChildAt(i);
                    if (mAnswers != null && i < mAnswers.length && mAnswers[i] != 0) {
                        child.setVisibility(View.VISIBLE);
                        child.setText(mAnswers[i]);
                        child.setOnClickListener(this);
                    } else {
                        child.setOnClickListener(null);
                        child.setVisibility(View.GONE);
                    }
                }
                mAnswersContainer.setVisibility(View.VISIBLE);
            } else {
                mAnswersContainer.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        mCompleted = true;
        stopQuestions();
        int reaction = ANSWER_REACTION_NOTHING;
        if (mCallback != null) {
            reaction = mCallback.onQuestionAnswered(this, mAnswersContainer.indexOfChild(v));
        }
        if (reaction >= 0) {
            mIntro.nextEpisode(reaction);
        } else if (reaction == ANSWER_REACTION_NEXT_EPISODE) {
            mIntro.nextEpisode();
        }
        mIntro.onQuestionAnswered(this);
    }
}
