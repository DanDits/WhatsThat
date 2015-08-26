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
