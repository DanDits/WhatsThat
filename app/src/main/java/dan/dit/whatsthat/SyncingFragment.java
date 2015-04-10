package dan.dit.whatsthat;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.RiddleManager;

/**
 * Created by daniel on 10.04.15.
 */
public class SyncingFragment extends Fragment {
    //TODO create listener for image syncing and unsolved riddles to notify RiddleFragment when those are available
    private ProgressBar mSyncingProgress;

    private void startSyncing() {
        ImageManager.sync(getActivity().getApplicationContext(), new ImageManager.SynchronizationListener() {

            @Override
            public void onSyncProgress(int syncingAtVersion, int imageProgress) {
                //TODO progress
                Log.d("Riddle", "Progress with loading image: " + syncingAtVersion + " synced " + imageProgress + "%");
            }

            @Override
            public void onSyncComplete(int syncedToVersion) {
                Log.d("Riddle", "Completely synced to version " + syncedToVersion + "/" + ImageManager.SYNC_VERSION);
                if (syncedToVersion == ImageManager.SYNC_VERSION) {
                    //TODO notify riddle fragment
                } else {
                    //TODO progress
                }
            }
        }); // loads all images available
    }

    private void stopSyncing() {
        ImageManager.cancelSync();
    }

    RiddleManager.init(getActivity().getApplicationContext(), new OperationDoneListener() {

        @Override
        public void operationDone() {
            Log.d("HomeStuff", "Initialization done. Starting a riddle if images loaded.");
        }
    }); // loads all cores

    @Override
    public void onStart() {
        super.onStart();
        startSyncing();
    }
    @Override
    public void onStop() {
        super.onStop();
        stopSyncing();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.syncing_base, null);
        return baseView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSyncingProgress = (ProgressBar) getView().findViewById(R.id.syncing_progress);
        mSyncingProgress.setMax(((ImageManager.SYNC_VERSION - ImageManager.getCurrentSynchronizationVersion(getActivity().getApplicationContext()))* 100));
    }
}
