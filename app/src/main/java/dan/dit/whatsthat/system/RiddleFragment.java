package dan.dit.whatsthat.system;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperToast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.RiddleMaker;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.UnsolvedRiddlesChooser;
import dan.dit.whatsthat.riddle.games.RiddleGame;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.system.store.StoreActivity;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ExternalStorage;
import dan.dit.whatsthat.util.ui.ImageButtonWithNumber;

/**
 * Created by daniel on 10.04.15.
 */
public class RiddleFragment extends Fragment implements PercentProgressListener, LoaderManager.LoaderCallbacks<Cursor>, SolutionInputListener, UnsolvedRiddlesChooser.Callback, NoPanicDialog.Callback {
   public static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    private RiddleManager mManager;
    private RiddleView mRiddleView;
    private SolutionInputView mSolutionView;
    private PercentProgressListener mProgressBar;
    private ImageButton mBtnRiddles;
    private ImageButton mBtnCheat;
    private ImageButtonWithNumber mOpenStore;
    private Iterator<Long> mOpenUnsolvedRiddlesId;
    private RiddlePickerDialog mRiddlePickerDialog;
    private boolean mErrorHandlingAttempted;

    public void onProgressUpdate(int progress) {
        mProgressBar.onProgressUpdate(progress);
    }

    private boolean canClickNextRiddle() {
        return !ImageManager.isSyncing() && !mManager.isMakingRiddle();
    }

    private void updateNextRiddleButton() {
        mBtnRiddles.setEnabled(canClickNextRiddle());
    }

    private void nextRiddleIfEmpty() {
        if (!mRiddleView.hasController()) {
            long suggestedId = Riddle.getLastVisibleRiddleId(getActivity().getApplicationContext());
            if (suggestedId != Riddle.NO_ID || (mManager.getUnsolvedRiddleCount() > 0)) {
                nextUnsolvedRiddle(suggestedId);
            } else {
                nextRiddle();
            }
        }
    }

    private int[] mLocation = new int[2];

    private void onRiddleMade(RiddleGame riddle) {
        mProgressBar.onProgressUpdate(0);
        riddle.initViews(mRiddleView, mSolutionView, this);
        updateNextRiddleButton();
        if (mRiddleView != null && mRiddleView.hasController()) {

            long currRiddleId = mRiddleView.getRiddleId();
            if (currRiddleId <= Riddle.NO_ID) {
                Log.e("Riddle", "Got riddle with no id and still no id: " + currRiddleId + " riddle " + riddle);
            }
            PracticalRiddleType currRiddleType = mRiddleView.getRiddleType();
            Riddle.saveLastVisibleRiddleId(getActivity().getApplicationContext(), currRiddleId);
            Riddle.saveLastVisibleRiddleType(getActivity().getApplicationContext(), currRiddleType);
            PracticalRiddleType type = mRiddleView.getRiddleType();
            int alreadyRun = Riddle.getRiddleTypeAlreadyRunCount(getActivity(), type);
            if (alreadyRun < Riddle.DISPLAY_INITIAL_RUN_HINT_COUNT) {
                mBtnRiddles.getLocationOnScreen(mLocation);
                TestSubject.getInstance().postToast(type.getInitialRunToast(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, mLocation[1] + mBtnRiddles.getHeight() + 50), 500L);
                Riddle.saveRiddleTypeAlreadyRun(getActivity(), type, alreadyRun + 1);
            }
        }
    }

    private void handleError(Image image, Riddle riddle) {
        if (image != null) {
            if (TextUtils.isEmpty(image.getRelativePath()) || ExternalStorage.isMounted()) {
                Log.e("Riddle", "No bitmap for image " + image + " has no relative image path or has one but storage is mounted -> removing from database.");
                ImageManager.removeInvalidImageImmediately(getActivity(), image); // will update cursor and therefore list of images
            } else if (mErrorHandlingAttempted) {
                Toast.makeText(getActivity(), R.string.handle_error_attempted, Toast.LENGTH_SHORT).show();
            }
        }
        if (riddle != null) {
            Log.e("Riddle", "Riddle on error. " + riddle + " removing from unsolved and database.");
            mManager.onRiddleInvalidated(riddle);
            Riddle.deleteFromDatabase(getActivity(), riddle.getId());
        }
        if (!mErrorHandlingAttempted) {
            mErrorHandlingAttempted = true;
            findSomeRiddle();
        }
    }

    private Dimension makeRiddleDimension() {
        return new Dimension(mRiddleView.getWidth(), mRiddleView.getHeight());
    }

    private void nextRiddle() {
        if (!canClickNextRiddle()) {
            mBtnRiddles.setEnabled(false);
            return;
        }
        clearRiddle();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        mOpenUnsolvedRiddlesId = null;
        mManager.makeRiddle(getActivity().getApplicationContext(), findNextRiddleType(),
                makeRiddleDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        RiddleFragment.this.onProgressUpdate(progress);
                    }

                    @Override
                    public void onRiddleReady(RiddleGame riddle) {
                        onRiddleMade(riddle);
                        mRiddleView.getRiddleType().getAchievementData(AchievementManager.getInstance()).onNewGame();
                        playStartRiddleAnimation();
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateNextRiddleButton();
                    }
                });
        updateNextRiddleButton();
    }

    private void playStartRiddleAnimation() {
        final long inputAnimationDelay = 500L;
        final long inputAnimation = 500L;
        mSolutionView.setVisibility(View.INVISIBLE);
        mSolutionView.clearAnimation();
        TranslateAnimation moveIn = new TranslateAnimation(Animation.ABSOLUTE, 0.f, Animation.ABSOLUTE, 0.f, Animation.RELATIVE_TO_SELF, 1.f, Animation.RELATIVE_TO_SELF, 0.f);
        moveIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mSolutionView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        moveIn.setInterpolator(new OvershootInterpolator(2.5f));
        moveIn.setStartOffset(inputAnimationDelay);
        moveIn.setDuration(inputAnimation);
        mSolutionView.startAnimation(moveIn);
    }

    private void nextCheatedRiddle(Image image) {
        if (!canClickNextRiddle() || image == null) {
            return;
        }
        if (mRiddleView.hasController()) {
            clearRiddle();
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mManager.makeSpecific(getActivity().getApplicationContext(), image, findNextRiddleType(), makeRiddleDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        RiddleFragment.this.onProgressUpdate(progress);
                    }

                    @Override
                    public void onRiddleReady(RiddleGame riddle) {
                        onRiddleMade(riddle);
                        playStartRiddleAnimation();
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Unsolved Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateNextRiddleButton();
                    }
                }
        );
        updateNextRiddleButton();

    }

    private void nextUnsolvedRiddle(long suggestedId) {
        if (!canClickNextRiddle()) {
            return;
        }
        if (mManager.getUnsolvedRiddleCount() == 0) {
            nextRiddle();
            return;
        }
        clearRiddle();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mManager.remakeOld(getActivity().getApplicationContext(), suggestedId, makeRiddleDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        RiddleFragment.this.onProgressUpdate(progress);
                    }

                    @Override
                    public void onRiddleReady(RiddleGame riddle) {
                        onRiddleMade(riddle);
                        //playStartRiddleAnimation(mBtnUnsolvedRiddles);
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Unsolved Riddle maker on error.");
                        handleError(image, riddle);
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

    private void giveCandy(PracticalRiddleType solvedType) {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.solution_complete);
        mSolutionView.startAnimation(anim);
        SuperToast toast = new SuperToast(getActivity());
        toast.setAnimations(SuperToast.Animations.POPUP);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setBackground(SuperToast.Background.BLUE);
        toast.setText(TestSubject.getInstance().nextRiddleSolvedCandy());
        toast.setTextSize(40);
        toast.setDuration(SuperToast.Duration.SHORT);
        if (solvedType != null) {
            toast.setIcon(solvedType.getIconResId(), SuperToast.IconPosition.LEFT);
        }
        toast.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findSomeRiddle();
            }
        }, toast.getDuration() / 2);
    }

    private PracticalRiddleType findNextRiddleType() {
        return TestSubject.getInstance().findNextRiddleType();
    }

    private void findSomeRiddle() {
        if (mOpenUnsolvedRiddlesId != null && mOpenUnsolvedRiddlesId.hasNext()) {
            long nextId = mOpenUnsolvedRiddlesId.next();
            mOpenUnsolvedRiddlesId.remove();
            nextUnsolvedRiddle(nextId);
        } else {
            nextRiddle();
        }
    }

    @Override
    public boolean onSolutionComplete(String userWord) {
        PracticalRiddleType solvedType = null;
        if (mRiddleView.hasController()) {
            solvedType = mRiddleView.getRiddleType();
            mRiddleView.removeController();
        }
        mSolutionView.clearListener();
        giveCandy(solvedType);
        return true;
    }

    @Override
    public void onSolutionIncomplete() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRiddleView = (RiddleView) getView().findViewById(R.id.riddle_view);

        mProgressBar = (PercentProgressListener) getView().findViewById(R.id.progress_bar);
        mSolutionView = (SolutionInputView) getView().findViewById(R.id.solution_input_view);
        mOpenStore = (ImageButtonWithNumber) getView().findViewById(R.id.open_store);
        mOpenStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOpenStore();
            }
        });
        mBtnRiddles = (ImageButton) getView().findViewById(R.id.riddle_make_next);
        mBtnRiddles.setEnabled(false);
        mBtnRiddles.setImageResource(TestSubject.getInstance().getImageResId());
        mBtnRiddles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRiddlesClick();
            }
        });

        ImageButton mRiddleHint = (ImageButton) getView().findViewById(R.id.riddle_hint);
        mRiddleHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPanic();
            }
        });
        mBtnCheat = (ImageButton) getView().findViewById(R.id.riddle_cheat);
        mBtnCheat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheat();
            }
        });

        mManager = RiddleInitializer.INSTANCE.getRiddleManager();
    }

    private void onOpenStore() {
        if (mOpenStore.isEnabled()) {
            mOpenStore.setEnabled(false);
            Intent i = new Intent(getActivity(), StoreActivity.class);
            getActivity().startActivity(i);
            getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void onPanic() {
        // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHHHHHHHHHHHHHH
        Bundle args = new Bundle();
        mRiddleView.supplyNoPanicParams(args);
        NoPanicDialog dialog = new NoPanicDialog();
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), "PanicDialog");
    }

    @Override
    public void openUnsolvedRiddle(Collection<Long> toOpen) {
        if (toOpen != null && toOpen.size() > 0) {
            mOpenUnsolvedRiddlesId = toOpen.iterator();
            findSomeRiddle();
        }
    }

    @Override
    public boolean canSkip() {
        return mRiddleView != null && mRiddleView.hasController() && TestSubject.getInstance().canSkip();
    }

    @Override
    public void onSkip() {
        mOpenUnsolvedRiddlesId = null;
        nextRiddle();
    }

    @Override
    public void onComplain() {
        Toast.makeText(getActivity(), "Hättest du wohl gerne.", Toast.LENGTH_SHORT).show();
    }

    private void showRiddlesDialog() {
        Bundle args = new Bundle();
        if (mRiddleView.hasController()) {
            args.putLong(Riddle.LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, mRiddleView.getRiddleId());
        }
        if (mRiddlePickerDialog != null) {
            mRiddlePickerDialog.dismiss();
        }
        mRiddlePickerDialog = new RiddlePickerDialog();
        mRiddlePickerDialog.setArguments(args);
        mRiddlePickerDialog.show(getFragmentManager(), "RiddlePickerDialog");
    }

    private void onCheat() {

        final EditText input = new EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        new AlertDialog.Builder(getActivity())
                .setTitle("Rätsel laden")
                .setMessage("Bildname:")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().toLowerCase().trim();
                        Image selected = null;
                        for (Image image : ALL_IMAGES.values()) {
                            if (image.getName().equalsIgnoreCase(value)) {
                                selected = image;
                                break;
                            }
                        }
                        if (selected != null) {
                            nextCheatedRiddle(selected);
                        } else {
                            Toast.makeText(getActivity(), "Bild nicht gefunden.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void onRiddlesClick() {
        if (!canClickNextRiddle()) {
            mBtnRiddles.setEnabled(false);
            return;
        }
        showRiddlesDialog();
    }

    @Override
    public void onStart() {
        super.onStart();
        mErrorHandlingAttempted = false;
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRiddleView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mOpenStore.setEnabled(true);
        mRiddleView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mManager.cancelMakeRiddle();
        if (mRiddleView != null) {
            clearRiddle();
        }
        Log.d("Riddle", "Stopping riddle fragment");
        AchievementManager.commit();
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
                    Log.e("Riddle", "Cancelled loaded images task, currently in map: " + map.size());
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
            Log.d("Riddle", "On loader reset cancels loaded images task.");
            mLoadedImagesTask.cancel(true);
        }
    }

    public void onWindowFocusChange(boolean hasFocus) {
        if (mRiddleView != null) {
            if (hasFocus) {
                mRiddleView.onResume();
            } else {
                mRiddleView.onPause();
            }
        }
    }
}
