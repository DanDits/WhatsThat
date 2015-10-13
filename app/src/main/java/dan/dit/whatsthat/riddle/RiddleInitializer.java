package dan.dit.whatsthat.riddle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * A class that is responsible for initializing the unsolved riddles at application start and remembering
 * which images were already used by riddles.<br>
 * The RiddleManager can be accessed through this singleton class after it is initialized.
 * Created by daniel on 23.04.15.
 */
public final class RiddleInitializer {
    /**
     * The instance, can be freely accessed. State control is ensured through exceptions. Needs to be initialized first!
     */
    public static final RiddleInitializer INSTANCE = new RiddleInitializer();
    private static final String IMPORTANT_PREFERENCES_FILE = "dan.dit.whatsthat.important_preferences";
    /*
     * Key to shared preferences entry that stores the highest used id for Riddle cores. Used to retrieve new ids
     * for new cores as those are not assigned by the database. The database takes over these ids and so its possible
     * to uniquely identify a riddle before it is saved to permanent storage. This will overflow at max long and cause
     * errors, but if this ever happens we face a bug or a serious addict.
     */
    private static final String KEY_HIGHEST_USED_ID = "highest_used_id";

    private InitTask mInitTask;
    private List<InitProgressListener> mListener = new LinkedList<>();
    private final Map<RiddleType, Set<String>> mUsedImagesForType = new HashMap<>();
    private RiddleManager mManager;
    private long mHighestUsedId;
    private SharedPreferences mPrefs;

    private RiddleInitializer() {} // singleton

    /**
     * If this class is no longer initializing and the RiddleManager is available, returns it.
     * @return The RiddleManager instance created by this RiddleInitializer.
     */
    public RiddleManager getRiddleManager() {
        if (mManager == null || isInitializing()) {
            throw new IllegalStateException("No manager yet available, initialize first!");
        }
        return mManager;
    }

    /**
     * Registers a riddle. The image will be remembered to be already used by the riddle's type.
     * @param riddle The riddle to register. Not null.
     */
    void registerUsedRiddleImage(Riddle riddle) {
        if (riddle == null) {
            throw new IllegalArgumentException("Null riddle given, what image you want to register?");
        }
        Set<String> typeSet = mUsedImagesForType.get(riddle.getType());
        if (typeSet == null) {
            typeSet = new HashSet<>();
            mUsedImagesForType.put(riddle.getType(), typeSet);
        }
        typeSet.add(riddle.getImageHash());
    }

    /**
     * Checks if this RiddleInitializer is initialized and will return a valid RiddleManager and not throw
     * an Exception.
     * @return If it is not initialized.
     */
    public boolean isNotInitialized() {
        return mManager == null || isInitializing();
    }

    // used in success or failure, in good days or in bad days
    private void onInitFinish() {
        mInitTask = null;
        mListener.clear();
    }

    public long nextId() {
        long next = ++mHighestUsedId;
        saveHighestUsedId();
        return next;
    }

    /**
     * The cancellable task that loads the list of unsolved riddles, used images and other data
     * stored in the RiddleTable.
     */
    protected class InitTask extends AsyncTask<Void, Integer, List<Riddle>> implements InitProgressListener {
        private int mBaseProgress;
        private static final int INIT_STEPS = 2; // step1: unsolved riddles, step2: used images
        private Context mContext;

        public InitTask(Context context) {
            mContext = context;
        }

        public List<Riddle> doInBackground(Void... voids) {
            mBaseProgress = 0;
            checkedIdsInit(mContext);
            List<Riddle> unsolved = Riddle.loadUnsolvedRiddles(mContext, this);
            if (isCancelled()) {
                return null;
            }
            long highestUnsolvedId = Long.MIN_VALUE;
            for (Riddle rid : unsolved) {
                if (rid.getId() > highestUnsolvedId) {
                    highestUnsolvedId = rid.getId();
                }
            }
            if (highestUnsolvedId > mHighestUsedId) {
                Log.e("Riddle", "Unsolved riddle id higher than loaded highest used id! " + mHighestUsedId + " < " + highestUnsolvedId);
                if (BuildConfig.DEBUG) {
                    Toast.makeText(mContext, "Error, wrong highest id.", Toast.LENGTH_LONG).show();
                }
            }
            mUsedImagesForType.putAll(Riddle.loadUsedImagesForTypes(mContext, this));
            if (isCancelled()) {
                return null;
            }
            return unsolved;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress.length == 1) {
                for (InitProgressListener listener : mListener) {
                    listener.onProgressUpdate(progress[0]);
                }
            }
        }

        @Override
        public void onCancelled(List<Riddle> nothing) {
            onInitFinish();
        }

        @Override
        public void onPostExecute(List<Riddle> unsolved) {
            mManager = new RiddleManager(unsolved);
            List<InitProgressListener> listeners = new ArrayList<>(mListener); // copy as it will be cleared by onInitFinished
            onInitFinish(); // finish first, so that listeners that check the state are not mislead

            for (InitProgressListener listener : listeners) {
                listener.onInitComplete();
            }
        }

        @Override
        public void onProgressUpdate(int progress) {
            //intern progress, not on UI thread here!
            publishProgress(mBaseProgress + progress / INIT_STEPS);
        }

        @Override
        public void onInitComplete() {
            // intern progress, not on UI thread here!
            mBaseProgress += PercentProgressListener.PROGRESS_COMPLETE / INIT_STEPS;
            publishProgress(mBaseProgress);
        }
    }

    private long loadHighestUsedId() {
        return mPrefs.getLong(KEY_HIGHEST_USED_ID, Riddle.NO_ID);
    }

    private void saveHighestUsedId() {
        if (mPrefs.getLong(KEY_HIGHEST_USED_ID, Riddle.NO_ID) < mHighestUsedId) {
            mPrefs.edit().putLong(KEY_HIGHEST_USED_ID, mHighestUsedId).apply();
        }
    }

    public void checkedIdsInit(Context context) {
        if (mPrefs == null && context != null) {
            mPrefs = context.getSharedPreferences(IMPORTANT_PREFERENCES_FILE, Context.MODE_PRIVATE);
            mHighestUsedId = loadHighestUsedId();
        }
    }

    /**
     * Unregisters previously registered listeners.
     * @param listener The listener to unregister. Does nothing if null.
     */
    public void unregisterInitProgressListener(InitProgressListener listener) {
        if ( listener != null) {
            mListener.remove(listener);
        }
    }

    /**
     * Cancels and finishes previously running initialization tasks.
     */
    public void cancelInit() {
        if (mInitTask != null) {
            mInitTask.cancel(true);
        }
        onInitFinish();
    }

    /**
     * Returns true if the RiddleInitializer is currently working and not yet finished.
     * @return If initializing is still in progress and not yet completed.
     */
    public boolean isInitializing() {
        return mInitTask != null;
    }

    /**
     * Cancels previous started initialization and starts a new initialization task.
     * @param context The context.
     * @param listener The listener.
     */
    public void init(@NonNull final Context context, @NonNull InitProgressListener listener) {
        cancelInit();
        mListener.add(listener);
        mInitTask = new InitTask(context);
        mInitTask.execute();
    }

    /**
     * The progress and callback interface for the RiddleInitializer.
     */
    public interface InitProgressListener extends PercentProgressListener {
        void onInitComplete();
    }

    /**
     * Returns a copy of the map of used images for each riddle type.
     * This is a deep copy meaning that the sets contained in the map are not backed by the initializer
     * can be changed freely.
     * @return A copy of the image hashes used by the riddle types. Can be empty.
     */
    Map<RiddleType, Set<String>> makeUsedImagesCopy() {
        // required because we might run into concurrency issues if an image gets added while we are still iterating over the list when making a new riddle
        Map<RiddleType, Set<String>> usedImagesForType = new HashMap<>();
        for (RiddleType type : mUsedImagesForType.keySet()) {
            usedImagesForType.put(type, new HashSet<>(mUsedImagesForType.get(type))); // make a copy of the Set<String>s
        }
        return usedImagesForType;
    }
}
