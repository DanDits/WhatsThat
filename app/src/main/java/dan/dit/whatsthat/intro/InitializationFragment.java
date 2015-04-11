package dan.dit.whatsthat.intro;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.RiddleManager;

/**
 * Created by daniel on 10.04.15.
 */
public class InitializationFragment extends Fragment implements ImageManager.SynchronizationListener, RiddleManager.InitProgressListener {
    private static final int STATE_DATA_NONE = 0;
    private static final int STATE_DATA_SUFFICIENT = 1;
    private static final int STATE_DATA_COMPLETE = 2;
    private ProgressBar mInitProgress;
    private int mRiddleProgress;
    private int mImageProgress;
    private int mState = STATE_DATA_NONE;
    private Button mInitSkip;

    private void checkDataState() {
        if (RiddleManager.isInitialized()) {
            int currSyncVersion = ImageManager.getCurrentSynchronizationVersion(getActivity());
            if (currSyncVersion == ImageManager.SYNC_VERSION) {
                mState = STATE_DATA_COMPLETE;
                mInitSkip.setText(R.string.init_skip_available_all);
                mInitSkip.setEnabled(true);
                return;
            } else if (currSyncVersion >= 1 && mState == STATE_DATA_NONE) {
                mState = STATE_DATA_SUFFICIENT;
                mInitSkip.setText(R.string.init_skip_available);
                mInitSkip.setEnabled(true);
                return;
            }
        }
        mState = STATE_DATA_NONE;
        mInitSkip.setText(R.string.init_skip_unavailable);
        mInitSkip.setEnabled(false);
    }

    private void initProgressBar() {
        mInitProgress.setMax(RiddleManager.PROGRESS_COMPLETE + ImageManager.PROGRESS_COMPLETE);
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
    public void onSyncProgress(int syncingAtVersion, int imageProgress) {
        mImageProgress = (ImageManager.PROGRESS_COMPLETE * syncingAtVersion + imageProgress) / ImageManager.SYNC_VERSION;
        updateProgressBar();
    }

    @Override
    public void onSyncComplete(int syncedToVersion) {
        mImageProgress = (ImageManager.PROGRESS_COMPLETE * syncedToVersion) / ImageManager.SYNC_VERSION;
        updateProgressBar();
        if (syncedToVersion == ImageManager.SYNC_VERSION) {
            //TODO notify user that everything is ready
            Log.d("HomeStuff", "Sync complete");
        }
        checkDataState();
    }

    @Override
    public void onInitProgress(int progress) {
        mRiddleProgress = progress;
        updateProgressBar();
    }

    @Override
    public void onInitComplete() {
        mRiddleProgress = RiddleManager.PROGRESS_COMPLETE;
        updateProgressBar();
        checkDataState();
        Log.d("HomeStuff", "Init complete");
    }

    private void startInit() {
        if (!RiddleManager.isInitialized()) {
            RiddleManager.init(getActivity().getApplicationContext(), this);
        } else {
            onInitComplete();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initProgressBar();
        checkDataState();
        startInit();
        startSyncing();
    }
    @Override
    public void onStop() {
        super.onStop();
        ImageManager.unregisterSynchronizationListener(this);
        RiddleManager.unregisterInitProgressListener(this);
        Log.d("HomeStuff", "OnStop of SyncingFragment, init running=" + RiddleManager.isInitializing() + " sync running=" + ImageManager.isSyncing());
        RiddleManager.cancelInit();
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
    }
}
