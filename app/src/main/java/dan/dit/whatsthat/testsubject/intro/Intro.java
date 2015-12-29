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

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubjectLevel;

/**
 * Created by daniel on 08.08.15.
 */
public class Intro {
    private static final String KEY_LAST_INTRO_LEVEL = "dan.dit.whatsthat.LAST_INTRO_LEVEL";
    private static final String KEY_CURRENT_EPISODE = "dan.dit.whatsthat.CURRENT_MAIN_EPISODE";
    private static final int INTERACTION_NEXT_UNMANAGED_EPISODE = 6;
    public static final int INTERACTION_QUESTION_ANSWERED = 7;
    private static final int INTERACTION_CURRENT_EPISODE_NOT_YET_DONE = -1;
    private static final int INTERACTION_NEXT_EPISODE = 1;

    private final View mIntroView;
    protected Episode mEpisode;
    private List<OnEpisodeSkippedListener> mListeners = new LinkedList<>();
    private OnInteractionListener mInteractionListener;

    public void setOnInteractionListener(OnInteractionListener listener) {
        mInteractionListener = listener;
    }

    public interface OnEpisodeSkippedListener {
        void onEpisodeSkipped(Episode skipped);
    }

    public interface OnInteractionListener {
        void onIntroInteraction(int actionCode);
    }

    private Intro(View introView) {
        mIntroView = introView;
        if (introView == null) {
            throw new IllegalArgumentException("No intro view given.");
        }
    }

    public static Intro makeIntro(View introView, TestSubjectLevel level) {
        return new Intro(introView).initEpisodes(level);
    }

    private Intro initEpisodes(TestSubjectLevel level) {
        mEpisode = level.makeEpisodes(this);
        if (mEpisode == null) {
            throw new IllegalArgumentException("No starting episode given.");
        }
        return this;
    }

    public void addOnEpisodeSkippedListener(OnEpisodeSkippedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeOnEpisodeSkippedListener(OnEpisodeSkippedListener listener) {
        mListeners.remove(listener);
    }

    public View findViewById(int viewId) {
        return mIntroView.findViewById(viewId);
    }

    public void load(SharedPreferences data, int level) {
        int savedLevel = data.getInt(KEY_LAST_INTRO_LEVEL, TestSubjectLevel.LEVEL_NONE);
        if (savedLevel != level) {
            return;
        }
        String key = data.getString(KEY_CURRENT_EPISODE, null);
        mEpisode = searchEpisode(mEpisode, key);
        mEpisode.init(key);
    }

    private static class EpisodeIterator implements Iterator<Episode> {

        private Set<EpisodeNode> mAllNodes;
        private List<EpisodeNode> mStack;
        private boolean mFoundCycle;
        private Episode mNext;

        private static class EpisodeNode {
            private Episode mEpisode;
            private int mChildIndex;
            public EpisodeNode(Episode episode) {
                mEpisode = episode;
            }
            @Override
            public int hashCode() {
                return mEpisode.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (other instanceof EpisodeNode) {
                    return mEpisode.equals(((EpisodeNode) other).mEpisode);
                }
                return super.equals(other);
            }
        }

        public EpisodeIterator(Episode start) {
            mAllNodes = new HashSet<>();
            mStack = new LinkedList<>();
            mFoundCycle = false;

            EpisodeNode startNode = new EpisodeNode(start);
            mAllNodes.add(startNode);
            mStack.add(startNode);
            mNext = start;
        }

        private void prepareNext() {
            EpisodeNode node;
            do {
                node = depthFirstSearchStep();
            } while (node == null && !mStack.isEmpty());
            mNext = node != null ? node.mEpisode : null;
        }

        private EpisodeNode getEpisodeNode(Episode of) {
            for (EpisodeNode node : mAllNodes) {
                if (node.mEpisode.equals(of)) {
                    return node;
                }
            }
            return null;
        }

        public boolean foundCycle() {
            return mFoundCycle;
        }
        @Override
        public boolean hasNext() {
            return mNext != null;
        }

        @Override
        public Episode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Episode curr = mNext;
            prepareNext();
            return curr;
        }

        private EpisodeNode depthFirstSearchStep() {
            EpisodeNode currNode = mStack.get(mStack.size() - 1);
            Episode curr = currNode.mEpisode;
            EpisodeNode newNode = null;
            if (currNode.mChildIndex < curr.getChildrenCount()) {
                Episode next = curr.getChild(currNode.mChildIndex);
                currNode.mChildIndex++;
                EpisodeNode nextNode = getEpisodeNode(next);
                if (nextNode != null) {
                    // this node was already reached sometime, check if in recursion stack
                    if (mStack.contains(nextNode)) {
                        mFoundCycle = true;
                    } else {
                        mStack.add(nextNode);
                    }
                } else {
                    nextNode = new EpisodeNode(next);
                    mStack.add(nextNode);
                    mAllNodes.add(nextNode);
                    newNode = nextNode;
                }
            } else {
                mStack.remove(mStack.size() - 1);
            }
            return newNode;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removing of episodes not supported.");
        }
    }

    private @NonNull
    Episode searchEpisode(@NonNull Episode start, @Nullable String key) {
        String episodeKey = TextUtils.isEmpty(key) ? null : Episode.extractEpisodeKey(key);
        Iterator<Episode> it = new EpisodeIterator(start);
        while (it.hasNext()) {
            Episode next = it.next();
            if (next.getEpisodeKey().equals(episodeKey)) {
                return next;
            }
        }
        return null;
    }

    public void save(SharedPreferences.Editor editor, int level) {
        editor.putInt(KEY_LAST_INTRO_LEVEL, level).putString(KEY_CURRENT_EPISODE,
                mEpisode.getKey()).apply();
    }

    protected void applyMessage(int messageResId) {
        TextView view = ((TextView) mIntroView.findViewById(R.id.intro_message));
        view.setVisibility(messageResId == 0 ? View.INVISIBLE : View.VISIBLE);
        view.setText(messageResId);
    }

    protected void applyMessage(String message) {
        TextView view = ((TextView) mIntroView.findViewById(R.id.intro_message));
        view.setVisibility(TextUtils.isEmpty(message) ? View.INVISIBLE : View.VISIBLE);
        view.setText(message);
    }

    protected void applyIcon(int iconResId) {
        ImageView view = ((ImageView) mIntroView.findViewById(R.id.intro_icon));
        view.clearAnimation();
        view.setVisibility(iconResId == 0 ? View.INVISIBLE : View.VISIBLE);
        view.setImageResource(iconResId);
    }

    public Episode getCurrentEpisode() {
        return mEpisode;
    }

    public boolean isMandatoryEpisodeMissing() {
        Iterator<Episode> it = new EpisodeIterator(mEpisode);
        while (it.hasNext()) {
            Episode next = it.next();
            if (next.isMandatory()) {
                return true;
            }
        }
        return false;
    }

    public Episode nextEpisode() {
        return nextEpisode(-1);
    }

    public Episode nextEpisode(int childIndex) {
        // make sure current episode is done
        // before getting the next episode
        if (!mEpisode.isDone()) {
            Log.d("HomeStuff", "Attempting to get next episode where current is not yet done: " +
                    mEpisode);
            onInteraction(INTERACTION_CURRENT_EPISODE_NOT_YET_DONE);
            return mEpisode;
        }
        Episode next = mEpisode.next(childIndex);
        if (next != null) {
            startEpisode(next);
            onInteraction(INTERACTION_NEXT_EPISODE);
            return next;
        }
        return mEpisode;
    }

    private void onInteraction(int actionId) {
        if (mInteractionListener != null) {
            mInteractionListener.onIntroInteraction(actionId);
        }
    }
    public void startUnmanagedEpisode(Episode episode) {
        if (episode == null || episode.mIntro != this) {
            return;
        }
        onInteraction(INTERACTION_NEXT_UNMANAGED_EPISODE);
        episode.start();
    }

    private void startEpisode(Episode episode) {
        if (episode == null) {
            return;
        }
        Episode current = getCurrentEpisode();
        if (current != null) {
            for (OnEpisodeSkippedListener listener : mListeners) {
                listener.onEpisodeSkipped(current);
            }
        }
        episode.start();
        mEpisode = episode;
    }

    protected void onQuestionAnswered(QuestionEpisode question) {
        onInteraction(INTERACTION_QUESTION_ANSWERED);
    }

    public Resources getResources() {
        return mIntroView.getResources();
    }
}
