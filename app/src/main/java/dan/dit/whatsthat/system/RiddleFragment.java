package dan.dit.whatsthat.system;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
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
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleHintView;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.RiddleMaker;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.games.InitializedRiddle;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.util.PercentProgressListener;

/**
 * Created by daniel on 10.04.15.
 */
public class RiddleFragment extends Fragment implements PercentProgressListener, LoaderManager.LoaderCallbacks<Cursor>, RiddleManager.UnsolvedRiddleListener, SolutionInputListener {
    public static final String MODE_UNSOLVED_RIDDLES_KEY = "dan.dit.whatsthat.unsolved_riddle_mode_key";

    public static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    private static final boolean MODE_UNSOLVED_RIDDLES_DEFAULT = false;
    private RiddleManager mManager;
    private RiddleView mRiddleView;
    private SolutionInputView mSolutionView;
    private ImageButton mBtnNextRiddle;
    private ImageButton mBtnUnsolvedRiddles;
    private boolean mModeUnsolvedRiddles;
    private ImageButton mBtnNextType;
    private PracticalRiddleType mCurrRiddleType = PracticalRiddleType.CIRCLE_INSTANCE;
    private RiddleHintView mRiddleHint;

    public void onProgressUpdate(int progress) {
        //TODO handle
    }

    private void updateRiddleMode() {
        if (mModeUnsolvedRiddles) {
            mBtnNextRiddle.setImageResource(R.drawable.next_riddle_right);
        } else {
            mBtnNextRiddle.setImageResource(R.drawable.next_riddle_left);
        }
    }

    private void updateUnsolvedRiddleUI() {
        int unsolvedCount = mManager.getUnsolvedRiddleCount();
        unsolvedCount = Math.max(0, unsolvedCount - (mRiddleView != null && mRiddleView.hasController() ? 1 : 0)); // subtract the currently displayed one as this counts as unsolved too
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
    }

    private boolean canClickNextRiddle() {
        return !ImageManager.isSyncing() && !mManager.isMakingRiddle();
    }

    private void updateNextRiddleButton() {
        mBtnNextRiddle.setEnabled(canClickNextRiddle());
    }

    private void nextRiddleIfEmpty() {
        if (!mRiddleView.hasController()) {
            long suggestedId = Riddle.getLastVisibleRiddleId(getActivity().getApplicationContext());
            if (suggestedId != Riddle.NO_ID || (mModeUnsolvedRiddles && mManager.getUnsolvedRiddleCount() > 0)) {
                nextUnsolvedRiddle(suggestedId);
            } else {
                nextRiddle();
            }
        }
    }

    private void onRiddleMade(InitializedRiddle riddle) {
        riddle.initViews(mRiddleView, mRiddleHint, mSolutionView, this);
        updateNextRiddleButton();
        updateUnsolvedRiddleUI();
    }

    private void nextRiddle() {
        if (!canClickNextRiddle()) {
            mBtnNextRiddle.setEnabled(false);
            return;
        }
        Log.d("HomeStuff", "Could click next riddle, has controller? " + mRiddleView.hasController());
        if (mRiddleView.hasController()) {
            clearRiddle();
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        mManager.makeRiddle(getActivity().getApplicationContext(), mCurrRiddleType,
                mRiddleView.getDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
            @Override
            public void onProgressUpdate(int progress) {
                RiddleFragment.this.onProgressUpdate(progress);
            }

            @Override
            public void onRiddleReady(InitializedRiddle riddle) {
                onRiddleMade(riddle);
                playNewRiddleViewAnimation();
            }

            @Override
            public void onError() {
                Log.e("HomeStuff", "Riddle maker on error.");
                RiddleFragment.this.onProgressUpdate(0);
                updateNextRiddleButton();
            }
        });
        updateNextRiddleButton();
    }

    private int[] mLocation1 = new int[2];
    private int[] mLocation2 = new int[2];
    public void playShelfRiddleViewAnimation() {
        // fly in the riddle view pretty fast from the shelf
        playRiddleViewAnimation(mBtnUnsolvedRiddles);
    }


    public void playNewRiddleViewAnimation() {
        // fly in the riddle view pretty fast from the brain
        playRiddleViewAnimation(mBtnNextType);
    }


    private void playRiddleViewAnimation(View fromView) {
        final long duration = 350;
        fromView.getLocationOnScreen(mLocation1);
        mRiddleView.getLocationOnScreen(mLocation2);
         AnimationSet animationSet = new AnimationSet(false);
        float startX = mLocation1[0] + fromView.getWidth() / 2.f - mLocation2[0];
        float startY = mLocation1[1] + fromView.getHeight() / 2.f - mLocation2[1] ;
        TranslateAnimation a = new TranslateAnimation(
                Animation.ABSOLUTE, startX , Animation.ABSOLUTE, 0.f,
                Animation.ABSOLUTE, startY , Animation.ABSOLUTE, 0.f);
        a.setDuration(duration);
        a.setInterpolator(new AnticipateOvershootInterpolator(5));

        ScaleAnimation s = new ScaleAnimation(0.05f, 1, 0.05f, 1, Animation.ABSOLUTE, startX, Animation.ABSOLUTE, startY);
        s.setInterpolator(new AccelerateInterpolator(1.5f));
        s.setDuration(duration);
        animationSet.addAnimation(a);

        animationSet.addAnimation(s);

        mRiddleView.startAnimation(animationSet);
    }



    private void nextUnsolvedRiddle(long suggestedId) {
        if (!canClickNextRiddle()) {
            return;
        }
        if (mManager.getUnsolvedRiddleCount() == 0) {
            nextRiddle();
            return;
        }
        if (mRiddleView.hasController()) {
            clearRiddle();
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mManager.remakeOld(getActivity().getApplicationContext(), suggestedId, mRiddleView.getDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        RiddleFragment.this.onProgressUpdate(progress);
                    }

                    @Override
                    public void onRiddleReady(InitializedRiddle riddle) {
                        onRiddleMade(riddle);
                        playShelfRiddleViewAnimation();
                    }

                    @Override
                    public void onError() {
                        Log.e("HomeStuff", "Unsolved Riddle maker on error.");
                        RiddleFragment.this.onProgressUpdate(0);
                        updateNextRiddleButton();
                    }
                }
                );
        updateNextRiddleButton();

    }

    private void clearRiddle() {
        if (mRiddleView.hasController()) {
            mRiddleView.removeController();
        }
        mSolutionView.setSolutionInput(null, null);
        mSolutionView.clearListener();
    }

    private void toggleModeUnsolvedRiddles() {
        mModeUnsolvedRiddles = !mModeUnsolvedRiddles;
        getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                .putBoolean(MODE_UNSOLVED_RIDDLES_KEY, mModeUnsolvedRiddles).apply();
        updateRiddleMode();
    }

    @Override
    public boolean onSolutionComplete(String userWord) {
        int unsolvedRiddlesCount = mManager.getUnsolvedRiddleCount();
        if (mRiddleView.hasController()) {
            unsolvedRiddlesCount--;
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

        mBtnNextRiddle = (ImageButton) getView().findViewById(R.id.riddle_make_next);
        mBtnNextRiddle.setEnabled(false);
        mBtnNextRiddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextRiddleClick();
            }
        });
        mBtnUnsolvedRiddles = (ImageButton) getView().findViewById(R.id.riddle_unsolved);
        mBtnUnsolvedRiddles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUnsolvedRiddleClick();
            }
        });
        mSolutionView = (SolutionInputView) getView().findViewById(R.id.solution_input_view);
        mBtnNextType = (ImageButton) getView().findViewById(R.id.riddle_next_type);
        mBtnNextType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRiddleTypeClick();
            }
        });
        mRiddleHint = (RiddleHintView) getView().findViewById(R.id.riddle_hint);
        mManager = RiddleInitializer.INSTANCE.getRiddleManager();
    }

    private void onRiddleTypeClick() {
        if (mModeUnsolvedRiddles) {
            toggleModeUnsolvedRiddles();
        } else {
            if (mCurrRiddleType == PracticalRiddleType.CIRCLE_INSTANCE) {
                mCurrRiddleType = PracticalRiddleType.SNOW_INSTANCE;
            } else if (mCurrRiddleType == PracticalRiddleType.SNOW_INSTANCE) {
                mCurrRiddleType = PracticalRiddleType.DICE_INSTANCE;
            } else if (mCurrRiddleType == PracticalRiddleType.DICE_INSTANCE) {
                mCurrRiddleType = PracticalRiddleType.CIRCLE_INSTANCE;
            }
        }
        Toast.makeText(getActivity(), "'" + mCurrRiddleType.getFullName() + "' gewählt.", Toast.LENGTH_SHORT).show();
    }

    private void onUnsolvedRiddleClick() {
        if (!mModeUnsolvedRiddles) {
            toggleModeUnsolvedRiddles();
        }
    }

    private void onNextRiddleClick() {
        if (!canClickNextRiddle()) {
            mBtnNextRiddle.setEnabled(false);
            return;
        }
        if (mModeUnsolvedRiddles) {
            nextUnsolvedRiddle(Riddle.NO_ID);
        } else {
            nextRiddle();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mModeUnsolvedRiddles = getActivity().getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE)
                .getBoolean(MODE_UNSOLVED_RIDDLES_KEY, MODE_UNSOLVED_RIDDLES_DEFAULT);
        updateRiddleMode();
        updateUnsolvedRiddleUI();
        getLoaderManager().initLoader(0, null, this);
        mManager.registerUnsolvedRiddleListener(this);
        updateUnsolvedRiddleUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRiddleView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRiddleView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mManager.cancelMakeRiddle();
        long currRiddleId = Riddle.NO_ID;
        if (mRiddleView.hasController()) {
            currRiddleId = mRiddleView.getRiddleId();
            clearRiddle();
        }
        Log.d("Riddle", "Stopping riddle fragment, current riddle id: " + currRiddleId);

        Riddle.saveLastVisibleRiddleId(getActivity().getApplicationContext(), currRiddleId);
        mManager.unregisterUnsolvedRiddleListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.riddle_home, null);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created
        return new CursorLoader(getActivity(), ImagesContentProvider.CONTENT_URI_IMAGE, ImageTable.ALL_COLUMNS, null, null, ImageTable.COLUMN_TIMESTAMP);
    }

    private Cursor mLoadedImagesCursor;
    private AsyncTask mLoadedImagesTask;
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        Log.d("Image", "Loaded images with loader: " + data.getCount());
        mLoadedImagesCursor = data;
        mLoadedImagesTask = new AsyncTask<Void, Void, Map<String, Image>>() {
            @Override
            public Map<String, Image> doInBackground(Void... nothing) {
                mLoadedImagesCursor.moveToFirst();
                Map<String, Image> map = new HashMap<>(mLoadedImagesCursor.getCount());
                while (!isCancelled() && !mLoadedImagesCursor.isAfterLast()) {
                    Image curr = Image.loadFromCursor(getActivity().getApplicationContext(), mLoadedImagesCursor);
                    if (curr != null) {
                        map.put(curr.getHash(), curr);
                    }
                    mLoadedImagesCursor.moveToNext();
                }
                if (isCancelled()) {
                    mLoadedImagesCursor = null;
                }
                return map;
            }

            @Override
            public void onPostExecute(Map<String, Image> result) {
                mLoadedImagesTask = null;
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
        if (mLoadedImagesTask != null) {
            mLoadedImagesTask.cancel(true);
        }
    }

    @Override
    public void onUnsolvedRiddlesChanged() {
        updateUnsolvedRiddleUI();
    }
}
