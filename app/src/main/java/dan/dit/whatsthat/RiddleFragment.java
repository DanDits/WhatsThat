package dan.dit.whatsthat;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;

/**
 * Created by daniel on 10.04.15.
 */
public class RiddleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ImageManager.SynchronizationListener, RiddleManager.UnsolvedRiddleListener, SolutionInputListener {
    public static final String LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY = "dan.dit.whatsthat.unsolved_riddle_id_key";
    public static final String MODE_UNSOLVED_RIDDLES_KEY = "dan.dit.whatsthat.unsolved_riddle_mode_key";

    public static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    private static final boolean MODE_UNSOLVED_RIDDLES_DEFAULT = false;
    private RiddleView mRiddleView;
    private SolutionInputView mSolutionView;
    private Button mBtnNextRiddle;
    private boolean mIsMakingRiddle;
    private ImageButton mBtnUnsolvedRiddles;
    private ProgressBar mRiddleMakeProgress;
    private boolean mModeUnsolvedRiddles;

    private void updateUnsolvedRiddleUI() {
        int unsolvedCount = RiddleManager.getUnsolvedRiddleCount();
        int resId;
        switch (unsolvedCount) {
            case 0:
                resId = R.drawable.shelf0; break;
            case 1:
                resId = R.drawable.shelf1; break;
            case 2:
                resId = R.drawable.shelf2; break;
            case 3:
                resId = R.drawable.shelf3; break;
            case 4:
                resId = R.drawable.shelf4; break;
            case 5:
                resId = R.drawable.shelf5; break;
            default:
                resId = R.drawable.shelf6; break;
        }
        mBtnUnsolvedRiddles.setImageResource(resId);
        if (mModeUnsolvedRiddles) {
            mBtnUnsolvedRiddles.setColorFilter(Color.YELLOW);
        } else {
            mBtnUnsolvedRiddles.clearColorFilter();;
        }
    }

    private boolean canClickNextRiddle() {
        return !mIsMakingRiddle && ImageManager.getCurrentSynchronizationVersion(getActivity().getApplicationContext()) >= 1 && !RiddleManager.isInitializing();
    }

    private void updateNextRiddleButton() {
        mBtnNextRiddle.setEnabled(canClickNextRiddle());
    }

    private void nextRiddleIfEmpty() {
        if (!mRiddleView.hasController()) {
            long suggestedId = getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).
                    getLong(LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, Riddle.NO_ID);
            if (!mModeUnsolvedRiddles && suggestedId == Riddle.NO_ID) {
                nextRiddle();
            } else {
                nextUnsolvedRiddle(suggestedId);
            }
        }
    }

    private void nextRiddle() {
        if (!canClickNextRiddle()) {
            mBtnNextRiddle.setEnabled(false);
            return;
        }
        if (mRiddleView.hasController()) {
            clearRiddle();
        }
        mIsMakingRiddle = true;
        updateNextRiddleButton();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        RiddleManager.makeRiddle(getActivity().getApplicationContext(), PracticalRiddleType.Snow.INSTANCE,
                mRiddleView.getWidth(), mRiddleView.getHeight(),displaymetrics.densityDpi,
                new RiddleManager.RiddleMakerListener() {
            @Override
            public void onProgressUpdate(int progress) {
                mRiddleMakeProgress.setProgress(progress);
            }

            @Override
            public void onRiddleReady(Riddle riddle) {
                Log.d("HomeStuff", "Riddle ready! " + riddle + " image: " + riddle.getImage());
                mRiddleView.setController(riddle.getController());
                mSolutionView.setSolutionInput(riddle.getSolutionInput(), RiddleFragment.this);
                mIsMakingRiddle = false;
                updateNextRiddleButton();
            }

            @Override
            public void onError() {
                Log.e("HomeStuff", "Riddle maker on error.");
                mRiddleMakeProgress.setProgress(0);
                mIsMakingRiddle = false;
                updateNextRiddleButton();
            }
        });
    }

    private void playRiddleViewAnimation() {
        // fly in the riddle view pretty fast from the shelf

        AnimationSet animationSet = new AnimationSet(false);
        //animationSet.setInterpolator(new AccelerateInterpolator());
        float startX = mBtnUnsolvedRiddles.getX() - mRiddleView.getX() - mRiddleView.getRiddleOffsetX() + mBtnUnsolvedRiddles.getWidth() / 2.f;
        float startY = mBtnUnsolvedRiddles.getY() - mRiddleView.getY() - mRiddleView.getRiddleOffsetY() + mBtnUnsolvedRiddles.getHeight() / 2.f;
        TranslateAnimation a = new TranslateAnimation(
                Animation.ABSOLUTE, startX , Animation.ABSOLUTE, 0.f,
                Animation.ABSOLUTE, startY , Animation.ABSOLUTE, 0.f);
        a.setDuration(300);
        a.setInterpolator(new AnticipateOvershootInterpolator(3));

        /*RotateAnimation r = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        r.setInterpolator(new LinearInterpolator());
        r.setDuration(30);
        r.setRepeatCount(3);
        r.setRepeatMode(RotateAnimation.RESTART);*/

        ScaleAnimation s = new ScaleAnimation(0.05f, 1, 0.05f, 1, Animation.ABSOLUTE, startX, Animation.ABSOLUTE, startY);
        s.setInterpolator(new AccelerateInterpolator(1.5f));
        s.setDuration(300);
        //animationSet.addAnimation(r);
        animationSet.addAnimation(a);

        animationSet.addAnimation(s);

        mRiddleView.startAnimation(animationSet);
    }

    private void nextUnsolvedRiddle(long suggestedId) {
        if (!canClickNextRiddle()) {
            return;
        }
        if (RiddleManager.getUnsolvedRiddleCount() == 0) {
            nextRiddle();
            return;
        }
        mIsMakingRiddle = true;
        updateNextRiddleButton();
        if (mRiddleView.hasController()) {
            clearRiddle();
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        RiddleManager.remakeUnsolvedRiddle(getActivity().getApplicationContext(), suggestedId, mRiddleView.getWidth(), mRiddleView.getHeight(), displaymetrics.densityDpi,
                new RiddleManager.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        mRiddleMakeProgress.setProgress(progress );
                    }

                    @Override
                    public void onRiddleReady(Riddle riddle) {
                        Log.d("HomeStuff", "Unsolved Riddle ready! " + riddle + " image: " + riddle.getImage());
                        mRiddleView.setController(riddle.getController());
                        mSolutionView.setSolutionInput(riddle.getSolutionInput(), RiddleFragment.this);
                        playRiddleViewAnimation();
                        mIsMakingRiddle = false;
                        updateNextRiddleButton();
                    }

                    @Override
                    public void onError() {
                        Log.e("HomeStuff", "Unsolved Riddle maker on error.");
                        mIsMakingRiddle = false;
                        mRiddleMakeProgress.setProgress(0);
                        updateNextRiddleButton();
                    }
                }
                );

    }

    private void applySyncingStillInProgress(boolean inProgress) {
        if (inProgress) {
            AnimationDrawable progress = (AnimationDrawable) getResources().getDrawable(R.drawable.images_synching);
            progress.start();
            mBtnNextRiddle.setCompoundDrawables(progress, null, null, null);
        } else {
            mBtnNextRiddle.setCompoundDrawables(null, null, null, null);
        }
    }

    private void clearRiddle() {
        if (mRiddleView.hasController()) {
            mRiddleView.removeController();
        }
        mSolutionView.setSolutionInput(null, null);
    }

    private void toggleModeUnsolvedRiddles() {
        mModeUnsolvedRiddles = !mModeUnsolvedRiddles;
        getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(MODE_UNSOLVED_RIDDLES_KEY, mModeUnsolvedRiddles).apply();
        updateUnsolvedRiddleUI();
    }

    @Override
    public boolean onSolutionComplete(String userWord) {
        int unsolvedRiddlesCount = RiddleManager.getUnsolvedRiddleCount();
        if (mRiddleView.hasController()) {
            unsolvedRiddlesCount--;
            getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                    .putLong(LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, Riddle.NO_ID).apply();
            mRiddleView.removeController();
        }
        mSolutionView.clearListener();
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.solution_complete);
        mSolutionView.startAnimation(anim);
        if (mModeUnsolvedRiddles && unsolvedRiddlesCount > 0) {
            nextUnsolvedRiddle(Riddle.NO_ID);
        } else {
            nextRiddle();
        }
        return true;
    }

    @Override
    public void onSolutionIncomplete() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRiddleView = (RiddleView) getView().findViewById(R.id.riddle_view);

        mBtnNextRiddle = (Button) getView().findViewById(R.id.riddle_make_next);
        mBtnNextRiddle.setEnabled(false);
        mBtnNextRiddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextRiddle();
            }
        });
        mBtnUnsolvedRiddles = (ImageButton) getView().findViewById(R.id.riddle_unsolved);
        mBtnUnsolvedRiddles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextUnsolvedRiddle(Riddle.NO_ID);
            }
        });
        mBtnUnsolvedRiddles.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                toggleModeUnsolvedRiddles();
                return true;
            }
        });
        mRiddleMakeProgress = (ProgressBar) getView().findViewById(R.id.riddle_make_progress);
        mRiddleMakeProgress.setMax(RiddleManager.PROGRESS_COMPLETE);
        mSolutionView = (SolutionInputView) getView().findViewById(R.id.solution_input_view);
    }

    @Override
    public void onStart() {
        super.onStart();
        mModeUnsolvedRiddles = getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE)
                .getBoolean(MODE_UNSOLVED_RIDDLES_KEY, MODE_UNSOLVED_RIDDLES_DEFAULT);
        updateUnsolvedRiddleUI();
        if (ImageManager.isSyncing()) {
            applySyncingStillInProgress(true);
            ImageManager.registerSynchronizationListener(this);
            if (ALL_IMAGES.isEmpty()) {
                for (Image img : ImageManager.getCurrentImagesWhileSyncing()) {
                    ALL_IMAGES.put(img.getHash(), img);
                }
            }
            updateNextRiddleButton();
            nextRiddleIfEmpty();
        } else {
            applySyncingStillInProgress(false);
            getLoaderManager().initLoader(0, null, this);
        }
        RiddleManager.registerUnsolvedRiddleListener(this);
        updateUnsolvedRiddleUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        RiddleManager.cancelMakeRiddle();
        long currRiddleId = Riddle.NO_ID;
        if (mRiddleView.hasController()) {
            currRiddleId = mRiddleView.getRiddleId();
            clearRiddle();
        }
        Log.d("Riddle", "Stopping riddle fragment, current riddle id: " + currRiddleId);
        getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                .putLong(LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, currRiddleId).apply();
        ImageManager.unregisterSynchronizationListener(this);
        RiddleManager.unregisterUnsolvedRiddleListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.riddle_home, null);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created
        return new CursorLoader(getActivity(), ImagesContentProvider.CONTENT_URI_IMAGE, ImageTable.ALL_COLUMNS, null, null, ImageTable.COLUMN_TIMESTAMP);
    }

    public void onLoadFinished(Loader<Cursor> loader,final Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        Log.d("Image", "Loaded images with loader: " + data.getCount());
        new AsyncTask<Void, Void, Map<String, Image>>() {
            @Override
            public Map<String, Image> doInBackground(Void... nothing) {
                data.moveToFirst();
                Map<String, Image> map = new HashMap<>(data.getCount());
                while (!data.isAfterLast()) {
                    Image curr = Image.loadFromCursor(getActivity().getApplicationContext(), data);
                    if (curr != null) {
                        map.put(curr.getHash(), curr);
                    }
                    data.moveToNext();
                }
                return map;
            }

            @Override
            public void onPostExecute(Map<String, Image> result) {
                if (result != null) {
                    ALL_IMAGES.clear();
                    ALL_IMAGES.putAll(result);
                }
                updateNextRiddleButton();
                nextRiddleIfEmpty();
            }
        }.execute();

    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.

    }

    @Override
    public void onSyncProgress(int syncingAtVersion, int imageProgress) {

    }

    @Override
    public void onSyncComplete(int syncedToVersion) {
        if (syncedToVersion < ImageManager.SYNC_VERSION) {
            applySyncingStillInProgress(true);
        } else {
            applySyncingStillInProgress(false);
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public void onUnsolvedRiddlesChanged() {
        updateUnsolvedRiddleUI();
    }
}
