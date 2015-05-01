package dan.dit.whatsthat.system;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
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
import android.view.animation.BounceInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 10.04.15.
 */
public class InitializationFragment extends Fragment implements ImageManager.SynchronizationListener, RiddleInitializer.InitProgressListener {
    private static final int STATE_DATA_NONE = 0;
    private static final int STATE_DATA_COMPLETE = 1;
    private ProgressBar mInitProgress;
    private int mRiddleProgress;
    private int mImageProgress;
    private int mState = STATE_DATA_NONE;
    private Button mInitSkip;
    private ImageView mIntroAbduction;
    private ImageView mIntroKid;
    private TextView mIntroSubjectDescr;
    private View mIntroContainer;
    private View.OnTouchListener mNextTextListener;
    private TextView mIntroText;

    private void startAnimation() {
        final long fallDownDuration = 2000;
        final long fallDownLiftDelta = 500;
        final long liftDuration = 12000;
        final long suckInDelta = -500;
        final long suckInDuration = 600;
        mIntroKid.setImageResource(TestSubject.getInstance().getImageResId());

        AlphaAnimation alphaAnimation = new AlphaAnimation(0.f, 1.f);
        alphaAnimation.setInterpolator(new AccelerateInterpolator(3));
        alphaAnimation.setDuration(liftDuration);

        AnimationSet kidStuff = new AnimationSet(false);
        RotateAnimation rot = new RotateAnimation(0.f, 100.f, Animation.RELATIVE_TO_SELF, 0.8f, Animation.RELATIVE_TO_SELF, 1.f);
        rot.setInterpolator(new BounceInterpolator());
        rot.setDuration(fallDownDuration);
        TranslateAnimation move = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.2f, Animation.RELATIVE_TO_PARENT, -0.2f, //x
                Animation.RELATIVE_TO_PARENT,-0.f, Animation.RELATIVE_TO_PARENT, -0.65f); //y
        move.setStartOffset(fallDownDuration + fallDownLiftDelta);
        move.setDuration(liftDuration);

        ScaleAnimation s = new ScaleAnimation(1.f, 0, 1f, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        s.setInterpolator(new AccelerateInterpolator(1.5f));
        s.setStartOffset(liftDuration + suckInDelta);
        s.setDuration(suckInDuration);
        RotateAnimation rotSpinning = new RotateAnimation(0.f, 360.f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        s.setInterpolator(new AccelerateInterpolator(2.5f));
        rotSpinning.setStartOffset(liftDuration + suckInDelta);
        rotSpinning.setDuration(suckInDuration);

        kidStuff.addAnimation(s);
        kidStuff.addAnimation(rotSpinning);
        kidStuff.addAnimation(rot);
        kidStuff.addAnimation(move);
        kidStuff.setFillAfter(true);

        mIntroAbduction.startAnimation(alphaAnimation);
        mIntroKid.startAnimation(kidStuff);
    }

    private void onNextText() {
        TestSubject ts = TestSubject.getInstance();
        String nextText;
        if (ts.hasNextMainText()) {
            nextText = ts.nextMainText();
        } else {
            nextText = ts.nextNutsText();
        }
        if (TextUtils.isEmpty(nextText)) {
            mIntroText.setVisibility(View.INVISIBLE);
        } else {
            mIntroText.setVisibility(View.VISIBLE);
            mIntroText.setText(nextText);
        }
    }

    private void startIntro() {
        onNextText();
        Resources res = getResources();
        TestSubject subj = TestSubject.getInstance();
        StringBuilder builder = new StringBuilder();
        builder.append(res.getString(R.string.intro_test_subject_name));
        builder.append("\n");
        builder.append(res.getString(subj.getNameResId()));
        builder.append("\n");
        builder.append(res.getString(R.string.intro_test_subject_estimated_intelligence));
        builder.append("\n");
        builder.append(res.getString(subj.getIntelligenceResId()));
        mIntroSubjectDescr.setText(builder.toString());
        mNextTextListener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    onNextText();
                    return true;
                }
                return false;
            }
        };
        mIntroContainer.setOnTouchListener(mNextTextListener);
        if (TestSubject.getInstance().hasNextMainText()) {
            Log.d("HomeStuff", "starting animation");
            startAnimation();
        } else {
            Log.d("HomeStuff", "not starting animation");
            mIntroAbduction.setVisibility(View.INVISIBLE);
            mIntroKid.clearAnimation();
            mIntroKid.setVisibility(View.VISIBLE);
            //FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mIntroKid.getLayoutParams();

        }
    }

    private void checkDataState() {
        if (!RiddleInitializer.INSTANCE.isInitializing() && !ImageManager.isSyncing()) {
            Log.d("Image", "CheckDataState: is complete!");
            mState = STATE_DATA_COMPLETE;
            mInitSkip.setText(R.string.init_skip_available_all);
            mInitSkip.setEnabled(true);
            return;
        }
        mState = STATE_DATA_NONE;
        mInitSkip.setText(R.string.init_skip_unavailable);
        mInitSkip.setEnabled(false);
    }

    private void initProgressBar() {
        mInitProgress.setMax(PercentProgressListener.PROGRESS_COMPLETE + ImageManager.PROGRESS_COMPLETE);
        mRiddleProgress = 0;
        mImageProgress = 0;
    }

    private void updateProgressBar() {
        mInitProgress.setProgress(mImageProgress + mRiddleProgress);
    }

    private void startSyncing() {
        ImageManager.sync(getActivity().getApplicationContext(), this); // loads all images available
    }

    @Override
    public void onSyncProgress(int progress) {
        mImageProgress = progress;
        updateProgressBar();
    }

    @Override
    public void onSyncComplete() {
        mImageProgress = ImageManager.PROGRESS_COMPLETE;
        updateProgressBar();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Context context = getActivity();
                if (context != null) {
                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.shake);
                    mInitSkip.startAnimation(anim);
                }
            }
        }, 1500);
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

    private void startInit() {
        if (!RiddleInitializer.INSTANCE.isInitialized()) {
            initProgressBar();
            RiddleInitializer.INSTANCE.init(getActivity().getApplicationContext(), this);
        } else {
            onInitComplete();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startIntro();
        startInit();
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
        mIntroKid.clearAnimation();
        mIntroAbduction.clearAnimation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.initialization_base, null);
        return baseView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mInitProgress = (ProgressBar) getView().findViewById(R.id.init_progress);
        mInitSkip = (Button) getView().findViewById(R.id.init_skip);
        mIntroAbduction = (ImageView) getView().findViewById(R.id.init_abduction);
        mIntroKid = (ImageView) getView().findViewById(R.id.init_kid);
        mIntroSubjectDescr = (TextView) getView().findViewById(R.id.init_subject_descr);
        mIntroContainer = getView().findViewById(R.id.init_intro);
        mIntroText = (TextView) getView().findViewById(R.id.init_text);

        mInitSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState >= STATE_DATA_COMPLETE) {
                    mInitSkip.clearAnimation();
                    ((OnInitClosingCallback) getActivity()).onSkipInit();
                }
            }
        });
    }

    public interface OnInitClosingCallback {
        void onSkipInit();
    }
}
