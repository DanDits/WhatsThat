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

    public EpisodeBuilder(Intro intro) {
        mIntro = intro;
        mEpisodes = new LinkedList<>();
    }

    public EpisodeBuilder setCurrentIcon(int iconResId) {
        mCurrentIcon = iconResId;
        return this;
    }

    public EpisodeBuilder nextEpisode(String message) {
        Episode episode = new Episode(mIntro, message);
        episode.setIcon(mCurrentIcon);
        mEpisodes.add(episode);
        return this;
    }

    public EpisodeBuilder nextEpisodes(String[] messages, int start, int length) {
        for (int i = start; i < Math.min(start + length, messages.length); i++) {
            nextEpisode(messages[i]);
        }
        return this;
    }

    public EpisodeBuilder nextEpisodes(String[] messages) {
        return nextEpisodes(messages, 0, messages.length);
    }

    public EpisodeBuilder nextEpisode(int messageResId) {
        Episode episode = new Episode(mIntro, messageResId);
        episode.setIcon(mCurrentIcon);
        mEpisodes.add(episode);
        return this;
    }

    public EpisodeBuilder nextEpisode(Episode episode) {
        if (episode.mIntro != mIntro) {
            throw new IllegalArgumentException("Episode with different intro reference given.");
        }
        mEpisodes.add(episode);
        return this;
    }

    public List<Episode> build() {
        return mEpisodes;
    }
}
