package dan.dit.whatsthat;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
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
import dan.dit.whatsthat.riddle.PracticalRiddleType;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;

/**
 * Created by daniel on 10.04.15.
 */
public class RiddleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int MINIMUM_IMAGES_COUNT = 30;
    public static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    public static final String FLAG_SYNCING_STILL_IN_PROGRESS = "FLAG_SYNCING_IN_PROGRESS";
    private RiddleView mRiddleView;
    private Button mBtnNextRiddle;
    private boolean mIsMakingRiddle;
    private ImageButton mBtnUnsolvedRiddles;
    private ProgressBar mRiddleMakeProgress;

    private boolean canClickNextRiddle() {
        return !mIsMakingRiddle && ALL_IMAGES.size() >= MINIMUM_IMAGES_COUNT && RiddleManager.STATE_INITIALIZED;
    }

    private void updateNextRiddleButton() {
        mBtnNextRiddle.setEnabled(canClickNextRiddle());
    }

    private void nextRiddleIfEmpty() {
        if (!mRiddleView.hasController()) {
            nextRiddle();
        }
    }

    private void nextRiddle() {
        if (!canClickNextRiddle()) {
            mBtnNextRiddle.setEnabled(false);
            return;
        }
        if (mRiddleView.hasController()) {
            mRiddleView.removeController();
        }
        mIsMakingRiddle = true;
        updateNextRiddleButton();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        RiddleManager.makeRiddle(getActivity().getApplicationContext(), PracticalRiddleType.Circle.INSTANCE,
                mRiddleView.getWidth(), mRiddleView.getHeight(),displaymetrics.densityDpi,
                new RiddleManager.RiddleMakerListener() {
            @Override
            public void onProgressUpdate(int progress) {
                Log.d("HomeStuff", "Riddle maker progress: " + progress);
                mRiddleMakeProgress.setProgress(progress);
            }

            @Override
            public void onRiddleReady(Riddle riddle) {
                Log.d("HomeStuff", "Riddle ready! " + riddle + " image: " + riddle.getImage());
                mRiddleView.setController(riddle.getController());
                mIsMakingRiddle = false;
                updateNextRiddleButton();
            }

            @Override
            public void onError() {
                Log.d("HomeStuff", "Riddle maker on error.");
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

    private void nextUnsolvedRiddle() {
        if (RiddleManager.getUnsolvedRiddleCount() > 0 && canClickNextRiddle()) {
            mIsMakingRiddle = true;
            updateNextRiddleButton();
            if (mRiddleView.hasController()) {
                mRiddleView.removeController();
            }

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            RiddleManager.remakeUnsolvedRiddle(getActivity().getApplicationContext(), mRiddleView.getWidth(), mRiddleView.getHeight(), displaymetrics.densityDpi,
                    new RiddleManager.RiddleMakerListener() {
                        @Override
                        public void onProgressUpdate(int progress) {
                            Log.d("HomeStuff", "Unsolved Riddle maker progress: " + progress);
                            mRiddleMakeProgress.setProgress(progress );
                        }

                        @Override
                        public void onRiddleReady(Riddle riddle) {
                            Log.d("HomeStuff", "Unsolved Riddle ready! " + riddle + " image: " + riddle.getImage());
                            mRiddleView.setController(riddle.getController());
                            playRiddleViewAnimation();
                            mIsMakingRiddle = false;
                            updateNextRiddleButton();
                        }

                        @Override
                        public void onError() {
                            Log.d("HomeStuff", "Unsolved Riddle maker on error.");
                            mIsMakingRiddle = false;
                            mRiddleMakeProgress.setProgress(0);
                            updateNextRiddleButton();
                        }
                    }
                    );
        }
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

    public void onSyncingComplete() {
        applySyncingStillInProgress(false);
        nextRiddleIfEmpty();
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
                nextUnsolvedRiddle();
            }
        });
        mBtnUnsolvedRiddles.setVisibility(View.GONE);
        mRiddleMakeProgress = (ProgressBar) getView().findViewById(R.id.riddle_make_progress);
        mRiddleMakeProgress.setMax(RiddleManager.PROGRESS_COMPLETE);
    }

    @Override
    public void onStart() {
        super.onStart();
        getLoaderManager().initLoader(0, null, this);
        Bundle args = getArguments();
        if (args != null) {
            applySyncingStillInProgress(args.getBoolean(FLAG_SYNCING_STILL_IN_PROGRESS));
        }
        nextRiddleIfEmpty();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("Riddle", "Stopping riddle fragment.");
        if (mRiddleView.hasController()) {
            mRiddleView.removeController();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.riddle_home, null);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created
        return new CursorLoader(getActivity(), ImagesContentProvider.CONTENT_URI_IMAGE, ImageTable.ALL_COLUMNS, null, null, ImageTable.COLUMN_TIMESTAMP);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        Log.d("Image", "Loaded images with loader: " + data.getCount());
        if ((data.getCount() >= MINIMUM_IMAGES_COUNT && ALL_IMAGES.isEmpty()) || !ImageManager.isSyncing()) {
            // we got no images and loaded at least several or are no longer syncing:
            ALL_IMAGES.clear();
            data.moveToFirst();
            while (!data.isAfterLast()) {
                Image curr = Image.loadFromCursor(getActivity().getApplicationContext(), data);
                if (curr != null) {
                    ALL_IMAGES.put(curr.getHash(), curr);
                }
                data.moveToNext();
            }
            updateNextRiddleButton();
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.

    }

}
