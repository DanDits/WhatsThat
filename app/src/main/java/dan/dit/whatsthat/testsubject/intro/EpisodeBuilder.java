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

import java.util.LinkedList;
import java.util.List;

/**
 * Created by daniel on 08.08.15.
 */
public class EpisodeBuilder {
    List<Episode> mEpisodes;
    private int mCurrentIcon;
    private Intro mIntro;
    private Episode mCurrentEpisode;
    private boolean mJoinCurrentChildrenToNext;

    public EpisodeBuilder(Intro intro) {
        mIntro = intro;
        mEpisodes = new LinkedList<>();
    }

    public Intro getIntro() {
        return mIntro;
    }

    public EpisodeBuilder setCurrentIcon(int iconResId) {
        mCurrentIcon = iconResId;
        return this;
    }

    private void growChain(Episode next) {
        mEpisodes.add(next);
        if (mCurrentEpisode != null) {
            if (mJoinCurrentChildrenToNext) {
                mJoinCurrentChildrenToNext = false;
                for (int i = 0; i < mCurrentEpisode.getChildrenCount(); i++) {
                    mCurrentEpisode.getChild(i).addChild(next);
                }
            } else {
                mCurrentEpisode.addChild(next);
            }
        }
        mCurrentEpisode = next;
    }

    public EpisodeBuilder nextEpisode(String episodeKey, String message) {
        growChain(new Episode(episodeKey, mIntro, message).setIcon(mCurrentIcon));
        return this;
    }

    public EpisodeBuilder nextEpisodes(String episodeKey, String[] messages) {
        growChain(new Episode(episodeKey, mIntro, messages).setIcon(mCurrentIcon));
        return this;
    }

    public EpisodeBuilder nextEpisodes(String episodeKey, int strArrayResId) {
        return nextEpisodes(episodeKey, mIntro.getResources().getStringArray(strArrayResId));
    }

    public EpisodeBuilder nextEpisode(Episode episode) {
        if (episode.mIntro != mIntro) {
            throw new IllegalArgumentException("Episode with different intro reference given.");
        }
        growChain(episode);
        return this;
    }

    public void setCurrentEpisode(Episode episode) {
        mCurrentEpisode = episode;
        if (!mEpisodes.contains(episode)) {
            throw new IllegalArgumentException("Must set current episode to an episode added to " +
                    "this builder.");
        }
    }

    public Episode getCurrentEpisode() {
        return mCurrentEpisode;
    }

    public List<Episode> getAll() {
        return mEpisodes;
    }

    public Episode build() {
        return mEpisodes.get(0);
    }

    public EpisodeBuilder joinCurrentChildrenToNext() {
        mJoinCurrentChildrenToNext = true;
        return this;
    }
}
