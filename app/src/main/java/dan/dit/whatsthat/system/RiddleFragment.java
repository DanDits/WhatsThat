/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.system;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperToast;
import com.plattysoft.leonids.ParticleSystem;

import junit.framework.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementDataEvent;
import dan.dit.whatsthat.achievement.AchievementManager;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.RiddleMaker;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.riddle.UnsolvedRiddlesChooser;
import dan.dit.whatsthat.riddle.achievement.MiscAchievement;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.achievement.holders.TestSubjectAchievementHolder;
import dan.dit.whatsthat.riddle.control.GameWelcomeDialog;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.solution.SolutionInput;
import dan.dit.whatsthat.solution.SolutionInputListener;
import dan.dit.whatsthat.solution.SolutionInputView;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.system.store.StoreActivity;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectRiddleType;
import dan.dit.whatsthat.testsubject.TestSubjectToast;
import dan.dit.whatsthat.util.general.PercentProgressListener;
import dan.dit.whatsthat.util.general.SimpleCrypto;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ExternalStorage;
import dan.dit.whatsthat.util.ui.ImageButtonWithNumber;
import dan.dit.whatsthat.util.ui.UiStyleUtil;
import dan.dit.whatsthat.util.wallet.Wallet;
import dan.dit.whatsthat.util.wallet.WalletEntry;

/**
 * Created by daniel on 10.04.15.
 */
public class RiddleFragment extends Fragment implements PercentProgressListener, LoaderManager.LoaderCallbacks<Cursor>, SolutionInputListener,
        UnsolvedRiddlesChooser.Callback, NoPanicDialog.Callback, RiddleView.PartyCallback {
    public static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    private static final String PRE_ENCRYPTED_COMPLAIN = "Image: ";
    private static final String POST_ENCRYPTED_COMPLAIN = "EndImage";
    private RiddleManager mManager;
    private RiddleView mRiddleView;
    private SolutionInputView mSolutionView;
    private PercentProgressListener mProgressBar;
    private ImageButton mBtnRiddles;
    private ImageButtonWithNumber mOpenStore;
    private Iterator<Long> mOpenUnsolvedRiddlesId;
    private RiddlePickerDialog mRiddlePickerDialog;
    private boolean mErrorHandlingAttempted;
    private ImageButton mBtnCheat;
    private TextView mScoreInfo;
    private long mFirstClickTime;
    private int mClickCount;
    private ImageButton mBtnPanic;
    private Handler mMainHandler;
    private TestSubjectAchievementHolder.UnclaimedAchievementsCountListener mUnclaimedChangedListener;
    private Wallet.OnEntryChangedListener mScoreChangedListener;

    public void onProgressUpdate(int progress) {
        mProgressBar.onProgressUpdate(progress);
    }

    private boolean isTotalMenuEnabled() {
        return !ImageManager.isSyncing() && !mManager.isMakingRiddle();
    }

    private void updateMenuButtons() {
        mBtnRiddles.setEnabled(isTotalMenuEnabled());
        mBtnRiddles.setImageResource(TestSubject.getInstance().getImageResId());
        mBtnPanic.setEnabled(isTotalMenuEnabled());
    }

    private void updateScoreInfo() {
        if (mScoreInfo != null) {
            mScoreInfo.setText(String.valueOf(TestSubject.getInstance().getCurrentScore()));
        }
    }

    private void nextRiddleIfEmpty() {
        if (!mRiddleView.hasController()) {
            Log.d("Riddle", "Next riddle as it is empty.");
            long suggestedId = Riddle.getLastVisibleRiddleId(getActivity().getApplicationContext());
            if (suggestedId != Riddle.NO_ID || (mManager.getUnsolvedRiddleCount() > 0)) {
                nextUnsolvedRiddle(suggestedId);
            } else {
                nextRiddle();
            }
        }
    }

    private void onRiddleMade(RiddleGame riddle, boolean newRiddle) {
        mProgressBar.onProgressUpdate(0);
        riddle.initViews(mRiddleView, mSolutionView, this);
        updateMenuButtons();
        if (mRiddleView != null && mRiddleView.hasController()) {
            mErrorHandlingAttempted = false; // clear flag
            long currRiddleId = mRiddleView.getRiddleId();
            if (currRiddleId <= Riddle.NO_ID) {
                Log.e("Riddle", "Got riddle with no id and still no id: " + currRiddleId + " riddle " + riddle);
            }
            PracticalRiddleType currRiddleType = mRiddleView.getRiddleType();
            Riddle.saveLastVisibleRiddleId(getActivity().getApplicationContext(), currRiddleId);
            Riddle.saveLastVisibleRiddleType(getActivity().getApplicationContext(), currRiddleType);
            if (newRiddle) {
                checkedShowHintDialog(mRiddleView.getRiddleType());
            } else if (TestSubject.isInitialized() && !TestSubject.getInstance().canSkip()
                        && mSolutionView != null) {
                mSolutionView.provideHint(SolutionInput.HINT_LEVEL_MINIMAL);
            }
            if (mSolutionView != null &&
                    mSolutionView.getProvidedHintLevel() > SolutionInput.HINT_LEVEL_NONE) {
                mRiddleView.forbidRiddleBonusScore();
            }
        }
    }

    private void checkedShowHintDialog(PracticalRiddleType type) {
        if (type == null) {
            return;
        }
        if (TestSubject.getInstance().hasAvailableHint(type)) {
            GameWelcomeDialog dialog = GameWelcomeDialog.makeInstance(type);
            dialog.show(getFragmentManager(), GameWelcomeDialog.GAME_WELCOME_DIALOG_TAG);
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

    private void remakeCurrentRiddle() {
        if (!isTotalMenuEnabled()) {
            updateMenuButtons();
            return;
        }
        if (!mRiddleView.hasController()) {
            return;
        }
        Riddle current = mRiddleView.getRiddle();
        if (current == null) {
            return;
        }

        // try to find a new riddle type:
        PracticalRiddleType newType = TestSubject.getInstance().getTypesController().findNextRiddleType(true, current
                .getType());
        if (newType == null || newType.equals(current.getType())) {
            newType = TestSubject.getInstance().getTypesController().findNextRiddleType(false, current.getType());
        }
        if (newType == null) {
            Toast.makeText(getActivity(), R.string.panic_retry_failed_no_types, Toast.LENGTH_SHORT).show();
            return; // only found the excluded one
        }

        clearRiddle();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        mManager.remakeCurrentWithNewType(getActivity().getApplicationContext(),
                current,
                newType,
                makeRiddleDimension(), displaymetrics.densityDpi,
                new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        RiddleFragment.this.onProgressUpdate(progress);
                    }

                    @Override
                    public void onRiddleReady(RiddleGame riddle) {
                        onRiddleMade(riddle, true);
                        playStartRiddleAnimation();
                        AchievementProperties data = TestSubject.getInstance()
                                .getAchievementHolder().getMiscData();
                        if (data != null) {
                            // notify achievements that a riddle was remade and how many times
                            // for this riddle
                            data.enableSilentChanges(AchievementDataEvent.EVENT_TYPE_DATA_UPDATE);
                            data.putValue(MiscAchievementHolder.KEY_REMADE_RIDDLE_CURRENT_REMADE_COUNT,
                                    (long) riddle.getRemadeCount(),
                                    AchievementProperties.UPDATE_POLICY_ALWAYS);
                            data.increment(MiscAchievementHolder.KEY_REMADE_RIDDLE_CURRENT_COUNT,
                                    1L, 0L);
                            data.disableSilentChanges();
                        }

                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateMenuButtons();
                    }
                });
        updateMenuButtons();
    }

    private void nextRiddle() {
        if (!isTotalMenuEnabled()) {
            updateMenuButtons();
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
                        onRiddleMade(riddle, true);
                        mRiddleView.getRiddleType().getAchievementData(AchievementManager.getInstance()).onNewGame();
                        playStartRiddleAnimation();
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateMenuButtons();
                    }
                });
        updateMenuButtons();
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
        if (!isTotalMenuEnabled() || image == null) {
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
                        onRiddleMade(riddle, true);
                        playStartRiddleAnimation();
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Cheated Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateMenuButtons();
                    }
                }
        );
        updateMenuButtons();

    }

    private void nextUnsolvedRiddle(long suggestedId) {
        if (!isTotalMenuEnabled()) {
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
                        onRiddleMade(riddle, false);
                        //playStartRiddleAnimation(mBtnUnsolvedRiddles);
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        Log.e("HomeStuff", "Unsolved Riddle maker on error.");
                        handleError(image, riddle);
                        RiddleFragment.this.onProgressUpdate(0);
                        updateMenuButtons();
                    }
                }
        );
        updateMenuButtons();

    }

    private void clearRiddle() {
        if (mRiddleView.hasController()) {
            mRiddleView.removeController();
        }
        mSolutionView.setSolutionInput(null, null);
        mSolutionView.clearListener();
    }

    @Override
    public void giveCandy(TestSubjectToast candyToast) {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.solution_complete);
        mSolutionView.startAnimation(anim);

        long delay = 0L;
        if (candyToast != null) {
            SuperToast toast = candyToast.makeSuperToast(getActivity());
            toast.show();
            delay = toast.getDuration() / 2L;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findSomeRiddle();
            }
        }, delay);
    }

    private PracticalRiddleType findNextRiddleType() {
        return TestSubject.getInstance().getTypesController().findNextRiddleType();
    }

    private void findSomeRiddle() {
        Log.d("Riddle", "Find some riddle.");
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
        if (mRiddleView.hasController()) {
            mRiddleView.checkParty(getResources(), this);
            mRiddleView.removeController();
        }
        mSolutionView.clearListener();
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
        mScoreInfo = (TextView) getView().findViewById(R.id.currency);

        // only allow cheats in debug build
        final boolean allowCheats = BuildConfig.DEBUG;
        if (allowCheats) {
            /*mScoreInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickCount == 0 || System.currentTimeMillis() - mFirstClickTime > 2000L) {
                        mClickCount = 0;
                        mFirstClickTime = System.currentTimeMillis();
                    }
                    mClickCount++;
                    if (mClickCount >= 10) {
                        mBtnCheat.setVisibility(View.VISIBLE);
                    }
                }
            });
            mScoreInfo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mBtnCheat.setVisibility(View.VISIBLE);
                    return false;
                }
            });*/
            getView().findViewById(R.id.riddle_cheat).setVisibility(View.VISIBLE);
        }
        mBtnRiddles.setEnabled(false);
        mBtnRiddles.setImageResource(TestSubject.getInstance().getImageResId());
        mBtnRiddles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRiddlesClick();
            }
        });

        mBtnPanic = (ImageButton) getView().findViewById(R.id.riddle_hint);
        mBtnPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPanic();
            }
        });
        mBtnCheat = (ImageButton) getView().findViewById(R.id.riddle_cheat);
        if (allowCheats) {
            mBtnCheat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    onCheat();
                }
            });
        } else {
            mBtnCheat.setVisibility(View.GONE);
        }

        mManager = RiddleInitializer.INSTANCE.getRiddleManager();
    }

    private void onOpenStore() {
        if (mOpenStore.isEnabled()) {
            mOpenStore.setEnabled(false);
            cleanUp();
            mRiddleView.onPause();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(getActivity(), StoreActivity.class);
                    getActivity().startActivity(i);
                    getActivity().overridePendingTransition(R.anim.store_enter, R.anim.riddles_exit);
                }
            }, 150L);
        }
    }

    private void onPanic() {
        // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHHHHHHHHHHHHHH
        Bundle args = new Bundle();
        mRiddleView.supplyNoPanicParams(args);
        mSolutionView.supplyNoPanicParams(args);
        NoPanicDialog dialog = new NoPanicDialog();
        dialog.setArguments(args);
        FragmentTransaction t = getFragmentManager().beginTransaction();
        dialog.show(t, "PanicDialog");
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

    private void decryptComplain() {
        Key privateKey = SimpleCrypto.getDeveloperPrivateKey();
        if (privateKey == null) {
            Toast.makeText(getActivity(), "Kein Schlüssel gefunden.", Toast.LENGTH_SHORT).show();
            return;
        }
        File complainFile = new File(ExternalStorage.getExternalStoragePathIfMounted(null) + "/kummerkasten.txt");
        if (!complainFile.exists()) {
            Toast.makeText(getActivity(), "Benötigt Datei " + complainFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }
        FileReader reader = null;
        String data = null;
        try {
            reader = new FileReader(complainFile);
            int read;
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[64];
            while ((read = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, read);
            }
            data = builder.toString();
        } catch (IOException ioe) {
            Toast.makeText(getActivity(), "Fehler beim Lesen der kummerkasten Datei", Toast.LENGTH_SHORT).show();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    //ignore
                }
            }
        }
        if (data == null) {
            return;
        }
        int start = data.lastIndexOf(PRE_ENCRYPTED_COMPLAIN);
        int end = data.lastIndexOf(POST_ENCRYPTED_COMPLAIN);
        if (start >= 0 && end >= 0 && start + PRE_ENCRYPTED_COMPLAIN.length() <= end) {
            String encrypted = data.substring(start + PRE_ENCRYPTED_COMPLAIN.length(), end);
            String decrypted = SimpleCrypto.decrypt(privateKey, encrypted);
            String result = data.substring(0, start + PRE_ENCRYPTED_COMPLAIN.length())
                    + decrypted
                    + POST_ENCRYPTED_COMPLAIN
                    + data.substring(end + POST_ENCRYPTED_COMPLAIN.length(), data.length());
            FileWriter writer = null;
            try {
                writer = new FileWriter(complainFile);
                writer.write(result);
                Toast.makeText(getActivity(), "Beschwerde bereit.. ;)", Toast.LENGTH_SHORT).show();
            } catch (IOException ioe) {
                Toast.makeText(getActivity(), "Fehler beim Schreiber der entschlüsselten Beschwerde", Toast.LENGTH_SHORT).show();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ioe) {
                        //ignore
                    }
                }
            }

        } else {
            Toast.makeText(getActivity(), "Verschlüsselter Bereich nicht gefunden.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onComplain(Image image) {
        if (mRiddleView == null) {
            return;
        }
        if (!mRiddleView.hasController()) {
            Toast.makeText(getActivity(), R.string.panic_complain_nothing, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{TestSubject.EMAIL_FEEDBACK});
            intent.putExtra(Intent.EXTRA_SUBJECT, "WhatsThat complain");
            StringBuilder builder = new StringBuilder();
            builder.append(getResources().getString(R.string.panic_complain_content))
                    .append("\n\nMetadata:\n")
                    .append("Type: ").append(mRiddleView.getRiddleType().getFullName()).append("\n")
                    .append("Riddle size: ").append(mRiddleView.getWidth()).append("x").append(mRiddleView.getHeight()).append("\n")
                    .append("Version: ").append(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName).append("\n");
            if (image != null) {
                builder.append("Image author: ").append(image.getAuthor().getName()).append("\n");
                Key publicKey = SimpleCrypto.getDeveloperPublicKey();
                if (publicKey != null) {
                    builder.append(PRE_ENCRYPTED_COMPLAIN).append(SimpleCrypto.encrypt(publicKey, System.currentTimeMillis() + ": " + image.getOrigin() + " " + image.getObfuscation() + " " + image.getName() + " " + image.getRelativePath() + " " + image.getHash()))
                            .append(POST_ENCRYPTED_COMPLAIN).append("\n");

                }
            }
            intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.e("HomeStuff", "Exception during complaining, better pretend or user gets mad :D " + e);
            Toast.makeText(getActivity(), R.string.panic_complain_dummy_toast, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRetryWithDifferentRiddle() {
        remakeCurrentRiddle();
        TestSubject.getInstance().getAchievementHolder().getMiscData().increment
                (MiscAchievementHolder.KEY_RETRYING_RIDDLE_COUNT, 1L, 0L);
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

    @SuppressWarnings("deprecation")
    private void onCheat() {

        final EditText input = new EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("CheatBox")
                .setMessage("Ch34t0r's h4ckZ:")
                .setView(input)
                .setPositiveButton("YEAH BABY", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String trimmedText = input.getText().toString().trim();
                        String value = trimmedText.toLowerCase();
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
                            if (value.matches("[0-9]+")) {
                                int money = 0;
                                try {
                                    money = Integer.parseInt(value);
                                } catch (NumberFormatException nfe) {
                                    //ignore
                                }
                                if (money > 0) {
                                    TestSubject.getInstance().addAchievementScore(money);
                                    Toast.makeText(getActivity(), "You got rich boy.", Toast.LENGTH_SHORT).show();
                                }
                            } else if (value.equalsIgnoreCase("kummerkasten")) {
                                decryptComplain();
                            } else if (value.equalsIgnoreCase("oneup")) {
                                if (TestSubject.getInstance().levelUp()) {
                                    Toast.makeText(getActivity(), "Level up!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), "Höher geht nicht...", Toast.LENGTH_SHORT).show();
                                }
                            } else if (value.equalsIgnoreCase("hide")) {
                                mBtnCheat.setVisibility(View.GONE);
                            } else if (value.equalsIgnoreCase("party")) {
                                doParty(0);
                            } else if (value.equalsIgnoreCase("boom")) {
                                throw new IllegalArgumentException("Boom.");
                            } else if (value.equalsIgnoreCase("sudo")) {
                                User.getInstance().givePermission(User.PERMISSION_SUPER_USER);
                                Toast.makeText(getActivity(), "You are now a local god", Toast.LENGTH_SHORT).show();
                            } else if (value.equalsIgnoreCase("sodu")) {
                                User.getInstance().removePermission(User.PERMISSION_SUPER_USER);
                                Toast.makeText(getActivity(), "Local god mode disabled", Toast.LENGTH_SHORT).show();
                            } else if (PracticalRiddleType.getInstance(trimmedText) != null) {
                                if (TestSubject.getInstance().addNewType(PracticalRiddleType.getInstance(trimmedText))) {
                                    Toast.makeText(getActivity(), "Experiment " + trimmedText + " jetzt verfügbar.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "Experiment " + trimmedText + " bereits vorhanden!", Toast.LENGTH_LONG).show();
                                }
                            } else if (trimmedText.endsWith(".xml")) {
                                if (ImageManager.calculateImagedataDeveloperFromFile(getActivity(), trimmedText)) {
                                    Toast.makeText(getActivity(), "XML Berechnung erfolgreich!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "XML Berechnung fehlgeschlagen!", Toast.LENGTH_LONG).show();
                                }
                            } else if (trimmedText.equalsIgnoreCase("calculate imagedata")) {
                                if (ImageManager.calculateImagedataDeveloper(getActivity())) {
                                    Toast.makeText(getActivity(), "XML Berechnung erfolgreich!!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "XML Berechnung fehlgeschlagen!!", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(getActivity(), "'" + value + "' nicht gefunden.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
        UiStyleUtil.setDialogDividerColor(dialog, getResources(), getResources().getColor(R.color.alien_purple));
    }

    @Override
    public void doParty(int partyParam) {
        final int emittingTime = 3000;
        final long particleLifeTime = 1500L;
        final int konfettiPerEmitterPerSecond = 5 + Math.max(partyParam, 0) * 3;
        final int konfettiPerEmitter = (int) (particleLifeTime *
                konfettiPerEmitterPerSecond /
                1000L);
        final ViewGroup parent = (ViewGroup) getView();
        final View emitAtView = mBtnRiddles;
        final int emitGravity = Gravity.BOTTOM;
        if (parent == null || emitAtView == null) {
            return;
        }
        new ParticleSystem(getResources(), konfettiPerEmitter, R.drawable.konfetti_long1,
                particleLifeTime, parent)
                .setScaleRange(0.7f, 1.3f)
                .setSpeedModuleAndAngleRange(0.07f, 0.12f, 0, 180)
                .setRotationSpeedRange(-90, 90)
                .setAcceleration(0.00013f, 90)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(emitAtView, emitGravity, konfettiPerEmitterPerSecond, emittingTime);
        new ParticleSystem(getResources(), konfettiPerEmitter, R.drawable.konfetti_long2,
                particleLifeTime, parent)
                .setScaleRange(0.7f, 1.3f)
                .setSpeedModuleAndAngleRange(0.07f, 0.12f, 0, 180)
                .setRotationSpeedRange(-90, 90)
                .setAcceleration(0.00013f, 90)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(emitAtView, emitGravity, konfettiPerEmitterPerSecond, emittingTime);
        new ParticleSystem(getResources(), konfettiPerEmitter, R.drawable.konfetti_small1,
                particleLifeTime, parent)
                .setSpeedModuleAndAngleRange(0.07f, 0.12f, 0, 180)
                .setAcceleration(0.00013f, 90)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(emitAtView, emitGravity, konfettiPerEmitterPerSecond, emittingTime);
        new ParticleSystem(getResources(), konfettiPerEmitter, R.drawable.konfetti_small2,
                particleLifeTime, parent)
                .setSpeedModuleAndAngleRange(0.07f, 0.12f, 0, 180)
                .setAcceleration(0.00013f, 90)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(emitAtView, emitGravity, konfettiPerEmitterPerSecond, emittingTime);
    }

    @Override
    public void showMoneyEarned(int earned) {
        final ViewGroup parent = (ViewGroup) getView();
        final View emitAtView = mRiddleView;
        final int emitGravity = Gravity.CENTER;
        if (parent == null || emitAtView == null || earned <= 0) {
            return;
        }
        new ParticleSystem(getResources(), earned, R.drawable.think_currency,
                3000L, parent)
                .setScaleRange(0.8f, 1.2f)
                .setSpeedModuleAndAngleRange(0.0005f, 0.001f, 0, 360)
                .setAccelerationModuleAndAndAngleRange(0.00009f, 0.0001f, 275, 310)
                .setFadeOut(200, new AccelerateInterpolator())
                .emitWithGravity(emitAtView, emitGravity, earned, 1000);
    }

    private void onRiddlesClick() {
        if (!isTotalMenuEnabled()) {
            mBtnRiddles.setEnabled(false);
            return;
        }
        showRiddlesDialog();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMainHandler = new Handler();
        mErrorHandlingAttempted = false;
        getLoaderManager().initLoader(0, null, this);
        updateScoreInfo();
        mScoreChangedListener = new Wallet.OnEntryChangedListener() {
            @Override
            public void onDataEvent(WalletEntry entry) {
                updateScoreInfo();
            }
            @Override
            public void onEntryRemoved(WalletEntry entry) {
            }
        };
        TestSubject.getInstance().registerScoreChangedListener(mScoreChangedListener);
        mUnclaimedChangedListener = new TestSubjectAchievementHolder.UnclaimedAchievementsCountListener() {
            @Override
            public void onDataEvent(Void nothing) {
                handleUnclaimedAchievementsCountChanged(TestSubject.getInstance()
                        .getAchievementHolder().getUnclaimedAchievementsCount());
            }
        };
        TestSubject.getInstance().getAchievementHolder().addUnclaimedAchievementsCountListener
                (mUnclaimedChangedListener);
        handleUnclaimedAchievementsCountChanged(TestSubject.getInstance().getAchievementHolder()
                .getUnclaimedAchievementsCount());
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
        updateMenuButtons();
    }

    // needs to be robust against multiple calls
    private void cleanUp() {
        AchievementManager.commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        mManager.cancelMakeRiddle();
        TestSubject.getInstance().removeScoreChangedListener(mScoreChangedListener);
        TestSubject.getInstance().getAchievementHolder().removeUnclaimedAchievementsCountListener
                (mUnclaimedChangedListener);
        if (mRiddleView != null) {
            mRiddleView.setVisibility(View.INVISIBLE); // else it is black when being reloaded
            // after having opened the shop
            clearRiddle();
        }
        cleanUp();
        Log.d("Riddle", "Stopping riddle fragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.riddle_home, null);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created
        return new CursorLoader(getActivity(), ImagesContentProvider.CONTENT_URI_IMAGE, ImageTable.ALL_COLUMNS, null, null, ImageTable.COLUMN_TIMESTAMP);
    }

    private volatile AsyncTask mLoadedImagesTask;
    public synchronized void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d("Image", "Loaded images with loader: " + data.getCount());
        if (mLoadedImagesTask != null) {
            mLoadedImagesTask.cancel(true);
        }
        mLoadedImagesTask = new AsyncTask<Cursor, Void, Map<String, Image>>() {

            @Override
            public Map<String, Image> doInBackground(Cursor... toLoads) {
                Cursor toLoad = toLoads[0];
                toLoad.moveToFirst();
                Map<String, Image> map = new HashMap<>(toLoad.getCount());
                while (!isCancelled() && !toLoad.isAfterLast()) {
                    Image curr = null;
                    synchronized (RiddleFragment.this) {
                        if (!isCancelled() && !toLoad.isClosed()) {
                            curr = Image.loadFromCursor(getActivity().getApplicationContext(), toLoad);
                        }
                    }
                    if (!isCancelled()) {
                        if (curr != null) {
                            map.put(curr.getHash(), curr);
                        }
                        toLoad.moveToNext();
                    }
                }
                if (isCancelled()) {
                    Log.e("Riddle", "Cancelled loaded images task, currently in map: " + map.size());
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
                updateMenuButtons();
                nextRiddleIfEmpty();
            }
        }.execute(data);

    }

    @Override
    public synchronized void onLoaderReset(Loader<Cursor> loader) {
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
        if (mBtnRiddles != null) {
            updateMenuButtons();
        }
    }

    private void handleUnclaimedAchievementsCountChanged(final int unclaimed) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (unclaimed == 0) {
                    mOpenStore.setImageResource(R.drawable.alien_menu_enter_notreasure);
                } else {
                    mOpenStore.setImageResource(R.drawable.alien_menu_enter);
                }
            }
        });
    }
}
