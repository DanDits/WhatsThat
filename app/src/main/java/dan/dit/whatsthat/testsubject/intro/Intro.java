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

    private final View mIntroView;
    protected List<Episode> mMandatoryEpisodes = new LinkedList<>();
    protected List<Episode> mMainEpisodes;
    protected List<Episode> mSubEpisodes;
    protected int mCurrentMainEpisode;
    protected int mCurrentSubEpisode;

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

    public Intro(View introView, List<Episode> mainEpisodes, List<Episode> subEpisodes) {
        this(introView);
        initEpisodes(mainEpisodes, subEpisodes);
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

    public void addEpisodeAsCurrentMain(Episode toAdd) {
        if (mCurrentSubEpisode >= 0 || toAdd == null || toAdd.mIntro != this) {
            return;
        }
        if (mCurrentMainEpisode < 0) {
            mMainEpisodes.add(0, toAdd);
            Log.d("HomeStuff", "Added episode at start " + "now has " + mMainEpisodes.size());
        } else {
            mMainEpisodes.add(mCurrentMainEpisode, toAdd);
            Log.d("HomeStuff", "Added episode at " + mCurrentMainEpisode + " now has " + mMainEpisodes.size());
        }
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
        Log.d("HomeStuff", "Saving intro " + level + " " + mCurrentMainEpisode + " " + mCurrentSubEpisode);
        editor.putInt(KEY_LAST_INTRO_LEVEL, level).putInt(KEY_CURRENT_MAIN_EPISODE, mCurrentMainEpisode)
                .putInt(KEY_CURRENT_SUB_EPISODE, mCurrentSubEpisode).apply();
    }

    protected void setEpisodeMandatory(Episode episode) {
        if (episode != null && !mMandatoryEpisodes.contains(episode) && episode.mIntro == this) {
            mMandatoryEpisodes.add(episode);
        }
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
        return !mMandatoryEpisodes.isEmpty();
    }


    public Episode nextEpisode() {
        Episode current = getCurrentEpisode();
        if (current != null && !current.isDone()) {
            Log.e("HomeStuff", "Attempting to get next episode where current is not yet done.");
            return current; // not yet done like filling out forms or waiting or stuff
        }
        if (mCurrentSubEpisode >= 0) {
            mCurrentSubEpisode++;
            if (mCurrentSubEpisode >= mSubEpisodes.size()) {
                mCurrentSubEpisode = 0; // restart sub episodes
            }
            Episode next = mSubEpisodes.get(mCurrentSubEpisode);
            next.start();
            return next;
        }
        mCurrentMainEpisode++;
        if (mCurrentMainEpisode >= mMainEpisodes.size()) {
            // main episodes done, start sub episodes
            mCurrentSubEpisode = 0;
            Episode next = mSubEpisodes.get(mCurrentSubEpisode);
            next.start();
            return next;
        }
        Episode next = mMainEpisodes.get(mCurrentMainEpisode);
        Log.d("HomeStuff", "Getting main episode with number " + mCurrentMainEpisode + " of total " + mMainEpisodes.size());
        next.start();
        return next;
    }

    public Resources getResources() {
        return mIntroView.getResources();
    }
}
