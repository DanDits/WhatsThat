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

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 10.08.15.
 */
public class QuestionEpisode extends Episode implements Intro.OnEpisodeSkippedListener, View.OnClickListener {
    private final int[] mAnswers;
    private final View.OnClickListener mCallback;
    private int mQuestionEpisodeCounter;
    private boolean mVisible;
    private boolean mCompleted;
    private ViewGroup mAnswersContainer;
    private final boolean mMandatory;

    public QuestionEpisode(Intro intro, boolean mandatory, String question, int[] answersResId, int otherEpisodesCount, View.OnClickListener callback) {
        super(intro, question);
        mMandatory = mandatory;
        mAnswers = answersResId;
        mQuestionEpisodeCounter = Math.max(0, otherEpisodesCount);
        mCallback = callback;
        mAnswersContainer = (ViewGroup) intro.findViewById(R.id.intro_answers_container);
    }

    @Override
    public void onEpisodeSkipped(Episode skipped) {
        if (mVisible) {
            if (mQuestionEpisodeCounter <= 0) {
                stopQuestions();
            } else {
                mQuestionEpisodeCounter--;
            }
        }
    }

    @Override
    public boolean isDone() {
        return !mVisible || mCompleted || mQuestionEpisodeCounter > 0;
    }

    @Override
    public boolean isMandatory() {
        return mMandatory;
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
        }
    }

    @Override
    public void onClick(View v) {
        mCompleted = true;
        stopQuestions();
        if (mCallback != null) {
            mCallback.onClick(v);
        }
        mIntro.onQuestionAnswered(this);
    }
}
