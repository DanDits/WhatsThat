package dan.dit.whatsthat.system;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.preferences.Language;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.intro.Episode;
import dan.dit.whatsthat.testsubject.intro.Intro;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.ui.LinearLayoutProgressBar;

/**
 * Created by daniel on 10.04.15.
 */
public class InitializationFragment extends Fragment implements ImageManager.SynchronizationListener, RiddleInitializer.InitProgressListener {
    private static final int STATE_DATA_NONE = 0;
    private static final int STATE_DATA_COMPLETE = 1;
    private int mRiddleProgress;
    private int mImageProgress;
    private LinearLayoutProgressBar mProgressBar;
    private int mState = STATE_DATA_NONE;
    private Button mInitSkip;
    private View mIntroContainer;
    private ImageButton mTongueSelect;
    private Intro mIntro;

    private void startIntro() {
        checkDataState();
        mIntro = TestSubject.getInstance().makeIntro(mIntroContainer);
        View.OnTouchListener nextTextListener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIntro.nextEpisode();
                    return true;
                }
                return false;
            }
        };
        mIntroContainer.setOnTouchListener(nextTextListener);
    }

    private synchronized void checkDataState() {
        if (!RiddleInitializer.INSTANCE.isInitializing() && !ImageManager.isSyncing() && TestSubject.isInitialized()) {
            if (mState != STATE_DATA_COMPLETE) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Context context = getActivity();
                        if (context != null) {
                            Animation anim = AnimationUtils.loadAnimation(context, R.anim.shake);
                            mProgressBar.startAnimation(anim);
                        }
                    }
                }, 1500);
            }
            mState = STATE_DATA_COMPLETE;
            mInitSkip.setText(R.string.init_skip_available_all);
            mInitSkip.setEnabled(true);
            mProgressBar.onProgressUpdate(0);
            return;
        }
        mState = STATE_DATA_NONE;
        mInitSkip.setText(R.string.init_skip_unavailable);
        mInitSkip.setEnabled(false);
    }

    private void initProgressBar() {
        mRiddleProgress = 0;
        mImageProgress = 0;
    }

    private void updateProgressBar() {
        mProgressBar.onProgressUpdate((mImageProgress + mRiddleProgress) / 2);
    }

    private void startSyncing() {
        if (!ImageManager.isSynced()) {
            ImageManager.sync(getActivity().getApplicationContext(), this); // loads all images available
        }
    }

    @Override
    public void onSyncProgress(int progress) {
        mImageProgress = progress;
        updateProgressBar();
    }

    @Override
    public void onSyncComplete() {
        mImageProgress = PercentProgressListener.PROGRESS_COMPLETE;
        updateProgressBar();
        Log.d("HomeStuff", "Sync complete");
        checkDataState();
    }

    @Override
    public boolean isSyncCancelled() {
        return false;
    }

    @Override
    public void onProgressUpdate(int progress) {
        mRiddleProgress = progress;
        updateProgressBar();
    }

    @Override
    public void onInitComplete() {
        mRiddleProgress = PercentProgressListener.PROGRESS_COMPLETE;
        updateProgressBar();
        checkDataState();
        Log.d("HomeStuff", "Init complete");
    }

    private void startRiddleInit() {
        if (RiddleInitializer.INSTANCE.isNotInitialized()) {
            RiddleInitializer.INSTANCE.init(getActivity().getApplicationContext(), this);
        } else {
            onInitComplete();
        }
    }

    private void initLanguage() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
        Tongue preferredTongue = Language.getTonguePreference(prefs);
        if (preferredTongue != null) {
            Language.makeInstance(preferredTongue);
        }
        updateTongueButton();
    }

    private void initTestSubject() {
        if (!TestSubject.isInitialized()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... nothing) {
                    TestSubject.initialize(getActivity().getApplicationContext());
                    return null;
                }

                @Override
                public void onPostExecute(Void nothing) {
                    TestSubject.getInstance().initToasts(); // has to run on ui thread since toasts will be displayed there
                    startIntro();
                }

            }.execute();
        } else {
            startIntro();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initProgressBar();
        initTestSubject();
        startRiddleInit();
        startSyncing();
        checkDataState();
    }

    @Override
    public void onStop() {
        super.onStop();
        ImageManager.unregisterSynchronizationListener();
        RiddleInitializer.INSTANCE.unregisterInitProgressListener(this);
        Log.d("HomeStuff", "OnStop of SyncingFragment, init running=" + RiddleInitializer.INSTANCE.isInitializing() + " sync running=" + ImageManager.isSyncing());
        RiddleInitializer.INSTANCE.cancelInit();
        TestSubject.getInstance().saveIntro(mIntro);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.initialization_base, null);
    }

    private void updateTongueButton() {
        mTongueSelect.setImageResource(Language.getInstance().getTongueIcon());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mInitSkip = (Button) getView().findViewById(R.id.init_skip);
        mIntroContainer = getView().findViewById(R.id.init_intro);
        mProgressBar = (LinearLayoutProgressBar) getView().findViewById(R.id.progress_bar);
        mTongueSelect = (ImageButton) getView().findViewById(R.id.tongue_select);
        mTongueSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tongue nextTongue = Tongue.nextTongue(Language.getInstance().getTongue());
                if (nextTongue != null) {
                    Language.makeInstance(nextTongue);
                    SharedPreferences prefs = getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
                    Language.getInstance().saveAsPreference(prefs);
                    updateTongueButton();
                }
            }
        });
        mTongueSelect.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getActivity(),
                        getResources().getString(R.string.tongue_select_explanation, Language.getInstance().getTongue().getLocalizedName()),
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }
        });
        mInitSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState >= STATE_DATA_COMPLETE) {
                    mInitSkip.clearAnimation();
                    mIntroContainer.setVisibility(View.GONE);
                    ((OnInitClosingCallback) getActivity()).onSkipInit();
                }
            }
        });
        initLanguage();
    }

    public interface OnInitClosingCallback {
        void onSkipInit();
    }
}
