package dan.dit.whatsthat.testsubject.intro;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectLevel;

/**
 * Created by daniel on 08.08.15.
 */
public class Intro {
    private static final String KEY_LAST_INTRO_LEVEL = "dan.dit.whatsthat.LAST_INTRO_LEVEL";
    private static final String KEY_CURRENT_MAIN_EPISODE = "dan.dit.whatsthat.CURRENT_MAIN_EPISODE";
    private static final String KEY_CURRENT_SUB_EPISODE = "dan.dit.whatsthat.CURRENT_SUB_EPISODE";
    private static final int INTERACTION_NEXT_MAIN_EPISODE_NOT_AVAILABLE = 1;
    private static final int INTERACTION_NEXT_SUB_EPISODE = 2;
    private static final int INTERACTION_NEXT_SUB_EPISODE_RESTART = 3;
    private static final int INTERACTION_NEXT_SUB_EPISODE_START = 4;
    private static final int INTERACTION_NEXT_MAIN_EPISODE = 5;
    private static final int INTERACTION_NEXT_UNMANAGED_EPISODE = 6;
    public static final int INTERACTION_QUESTION_ANSWERED = 7;

    private final View mIntroView;
    protected List<Episode> mMainEpisodes;
    protected List<Episode> mSubEpisodes;
    protected int mCurrentMainEpisode;
    protected int mCurrentSubEpisode;
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
        Intro intro = new Intro(introView);
        intro.initEpisodes(level.makeMainIntroEpisodes(intro), level.makeSubIntroEpisodes(intro));
        return intro;
    }

    private void initEpisodes(List<Episode> mainEpisodes, List<Episode> subEpisodes) {
        mMainEpisodes = mainEpisodes;
        if (mainEpisodes == null || mainEpisodes.isEmpty() || mainEpisodes.get(0) == null) {
            throw new IllegalArgumentException("No main episodes given.");
        }
        mSubEpisodes = subEpisodes;
        mCurrentMainEpisode = -1;
        mCurrentSubEpisode = -1;
        if (mSubEpisodes == null) {
            mSubEpisodes = Collections.EMPTY_LIST;
            if (BuildConfig.DEBUG && !mSubEpisodes.isEmpty()) {
                throw new IllegalStateException("Empty list instead of null list required for sub episodes.");
            }
        }
    }

    public void addOnEpisodeSkippedListener(OnEpisodeSkippedListener listener) {
        mListeners.add(listener);
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
        mCurrentMainEpisode = data.getInt(KEY_CURRENT_MAIN_EPISODE, -1);
        mCurrentSubEpisode = data.getInt(KEY_CURRENT_SUB_EPISODE, -1);
        if (mCurrentMainEpisode >= mMainEpisodes.size() && mCurrentSubEpisode < 0) {
            mCurrentSubEpisode = -1;
        } else if (mCurrentSubEpisode > mSubEpisodes.size()) {
            mCurrentSubEpisode = mSubEpisodes.size();
        }
        Log.d("HomeStuff", "Loading intro " + level + " " + mCurrentMainEpisode + " " + mCurrentSubEpisode);
    }

    public void save(SharedPreferences.Editor editor, int level) {
        // necessary to not skip mandatory not done episodes when saving since loading will not start this episode then
        int mainEpisode = mCurrentMainEpisode;
        for (int i = 0; i < Math.min(mMainEpisodes.size(), mCurrentMainEpisode + 1); i++) {
            Episode episode = mMainEpisodes.get(i);
            mainEpisode = i;
            if (episode.isMandatory() && !episode.isDone()) {
                break;
            }
        }
        Log.d("HomeStuff", "Saving intro " + level + " " + mainEpisode + " " + mCurrentSubEpisode);
        editor.putInt(KEY_LAST_INTRO_LEVEL, level).putInt(KEY_CURRENT_MAIN_EPISODE, mainEpisode)
                .putInt(KEY_CURRENT_SUB_EPISODE, mCurrentSubEpisode).apply();
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
        if (mCurrentSubEpisode >= 0) {
            return mSubEpisodes.get(mCurrentSubEpisode);
        } else if (mCurrentMainEpisode >= 0) {
            return mMainEpisodes.get(mCurrentMainEpisode);
        }
        return null;
    }

    public boolean isMandatoryEpisodeMissing() {
        // sub episodes cannot be mandatory
        if (mCurrentMainEpisode >= 0 && mCurrentSubEpisode < 0) {
            for (int i = 0; i < mMainEpisodes.size(); i++) {
                Episode episode = mMainEpisodes.get(i);
                if (i >= mCurrentMainEpisode && episode.isMandatory()) {
                    return true; // future episode not yet started or currently running episode is missing
                } else if (episode.isMandatory() && !episode.isDone()) {
                    return true; // previously started episode that is not done missing
                }
            }
        }
        return false;
    }


    public Episode nextEpisode() {
        // if we did not start with sub episodes yet, make sure all previous episodes are done before getting the next main or sub episode
        if (mCurrentSubEpisode < 0) {
            for (int i = 0; i <= mCurrentMainEpisode; i++) {
                if (i < mMainEpisodes.size()) {
                    Episode ep = mMainEpisodes.get(i);
                    if (!ep.isDone()) {
                        Log.d("HomeStuff", "Attempting to get next episode where current is not yet done: " + ep);
                        onInteraction(INTERACTION_NEXT_MAIN_EPISODE_NOT_AVAILABLE);
                        return getCurrentEpisode();
                    }
                }
            }
        }
        if (mCurrentSubEpisode >= 0) {
            mCurrentSubEpisode++;
            if (mCurrentSubEpisode >= mSubEpisodes.size()) {
                mCurrentSubEpisode = 0; // restart sub episodes
            }
            Episode next = mSubEpisodes.get(mCurrentSubEpisode);
            startEpisode(next);
            if (mCurrentSubEpisode == 0) {
                onInteraction(INTERACTION_NEXT_SUB_EPISODE_RESTART);
            } else {
                onInteraction(INTERACTION_NEXT_SUB_EPISODE);
            }
            return next;
        }
        mCurrentMainEpisode++;
        if (mCurrentMainEpisode >= mMainEpisodes.size()) {
            // main episodes done, start sub episodes
            mCurrentSubEpisode = 0;
            Episode next = mSubEpisodes.get(mCurrentSubEpisode);
            startEpisode(next);
            onInteraction(INTERACTION_NEXT_SUB_EPISODE_START);
            return next;
        }
        Episode next = mMainEpisodes.get(mCurrentMainEpisode);
        startEpisode(next);
        onInteraction(INTERACTION_NEXT_MAIN_EPISODE);
        return next;
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
    }

    protected void onQuestionAnswered(QuestionEpisode question) {
        onInteraction(INTERACTION_QUESTION_ANSWERED);
    }

    public Resources getResources() {
        return mIntroView.getResources();
    }
}
