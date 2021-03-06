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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperCardToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.plattysoft.leonids.ParticleSystem;

import java.io.File;
import java.net.URL;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageAuthor;
import dan.dit.whatsthat.image.ImageObfuscator;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.preferences.WebPhotoStorage;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.general.VersionSafe;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.ui.GlasDialog;

/**
 * Created by daniel on 06.05.15.
 */
public class NoPanicDialog extends DialogFragment {
    public static final String KEY_IMAGE = "key_image_hash";
    public static final String KEY_TYPE = "key_type_full_name";
    public static final String KEY_SOLUTION_INPUT_DATA = "key_solution_input_data";
    private static final String KEY_NO_SECRETS = "key_no_secrets";
    private PracticalRiddleType mType;
    private String mSolutionInputData;
    private Image mImage;
    private Callback mCallback;
    private ViewGroup mAskTypeAnswer;
    private TextView mAskTypeAnswerText;
    private int mFunCounter;
    private ViewGroup mAuthorContainer;
    private Button mShareExperiment;
    private URL mToShareLink;
    private Bitmap mToShare;
    private AsyncTask<Void, Void, File> mShareBuildTask;
    private ViewGroup mTotalContainer;

    public interface Callback {
        boolean canSkip();
        void onSkip();
        void onComplain(Image image);
        void onRetryWithDifferentRiddle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ImageUtil.CACHE.makeReusable(mToShare);
        mToShare = null;
        User.clearTempDirectory();
        mToShareLink = null;
        if (mShareBuildTask != null) {
            mShareBuildTask.cancel(true);
            mShareBuildTask = null;
        }
        this.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    }

    @Override
    public void onDetach() {
        if (mShareBuildTask != null) {
            mShareBuildTask.cancel(true);
            mShareBuildTask = null;
        }
        super.onDetach();
    }
    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.NoPanicDialogAnimation;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View baseView = getActivity().getLayoutInflater().inflate(R.layout.panic_dialog, null);
        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        String imageHash = null;
        String typeName = null;
        boolean noSecrets = false;
        String solutionInputData = null;
        Bundle args = getArguments();
        if (savedInstanceState != null) {
            imageHash = savedInstanceState.getString(KEY_IMAGE);
            typeName = savedInstanceState.getString(KEY_TYPE);
            noSecrets = savedInstanceState.getBoolean(KEY_NO_SECRETS);
            solutionInputData = savedInstanceState.getString(KEY_SOLUTION_INPUT_DATA);
        } else if (args != null) {
            imageHash = args.getString(KEY_IMAGE);
            typeName = args.getString(KEY_TYPE);
            noSecrets = args.getBoolean(KEY_NO_SECRETS);
            solutionInputData = args.getString(KEY_SOLUTION_INPUT_DATA);
        }
        if (typeName != null) {
            mType = PracticalRiddleType.getInstance(typeName);
        }
        if (imageHash != null) {
            mImage = RiddleFragment.ALL_IMAGES.get(imageHash);
        }
        mSolutionInputData = solutionInputData;

        if (mImage != null) {
            String[] headings = getResources().getStringArray(R.array.panic_author_credit_title);
            Button authorQuestion = (Button) baseView.findViewById(R.id.author_ask);
            authorQuestion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAskAuthor();
                }
            });
            mAuthorContainer = (ViewGroup) baseView.findViewById(R.id.author_container);
            authorQuestion.setVisibility(View.VISIBLE);
            mAuthorContainer.setVisibility(View.GONE);
            mTotalContainer = (ViewGroup) baseView.findViewById(R.id.panic_container);
            ((TextView) baseView.findViewById(R.id.credits_heading)).setText(headings[(int) (Math.random() * headings.length)]);

            ImageAuthor author = mImage.getAuthor();
            TextView nameView = (TextView) baseView.findViewById(R.id.author_name);
            setTextIfAvailable(nameView, R.string.image_author_name, author.getName());
            nameView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        new ParticleSystem(getResources(), 10, R.drawable.heart, 1000L,
                                mTotalContainer)
                                .setScaleRange(0.7f, 1.3f)
                                .setSpeedModuleAndAngleRange(0.07f, 0.16f, 180, 360)
                                .setRotationSpeedRange(-90, 90)
                                .setAcceleration(0.00013f, 90)
                                .setFadeOut(200, new AccelerateInterpolator())
                                .emitWithGravity(view, Gravity.CENTER, 3, 3000);
                        AchievementProperties data = TestSubject.getInstance().getAchievementHolder().getMiscData();
                        if (data != null) {
                            data.increment(MiscAchievementHolder.KEY_ADMIRED_IMAGE_AUTHOR, 1L, 0L);
                        }
                    }
                    return false;
                }
            });
            setTextIfAvailable(((TextView) baseView.findViewById(R.id.author_license)), R.string.image_author_license, author.getLicense());
            if (noSecrets) {
                setTextIfAvailable(((TextView) baseView.findViewById(R.id.author_source)), R.string.image_author_source, author.getSource());
                setTextIfAvailable(((TextView) baseView.findViewById(R.id.author_details)), R.string.image_author_details, author.getTitle() + "; " + author.getExtras());
            } else {
                setTextIfAvailable(((TextView) baseView.findViewById(R.id.author_source)), R.string.image_author_source, author.sourceExtractWebsite());
                baseView.findViewById(R.id.author_details).setVisibility(View.GONE);
            }
        } else {
            baseView.findViewById(R.id.author_ask).setVisibility(View.GONE);
            baseView.findViewById(R.id.author_container).setVisibility(View.GONE);
        }
        Button skip = (Button) baseView.findViewById(R.id.panic_skip);
        if (!mCallback.canSkip()) {
            skip.setEnabled(false);
            skip.setText(R.string.panic_cannot_skip);
        } else {
            skip.setEnabled(true);
            skip.setText(R.string.panic_skip);
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                    mCallback.onSkip();
                }
            });
        }

        Button retry = (Button) baseView.findViewById(R.id.panic_retry);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SuperCardToast toast = new SuperCardToast(getActivity(), SuperToast.Type.BUTTON);
                toast.setText(getResources().getString(R.string.panic_retry_notification));
                toast.setDuration(SuperToast.Duration.EXTRA_LONG);
                toast.setButtonIcon(R.drawable.panic_retry, getResources().getString(R.string
                        .panic_retry_confirm));
                toast.setButtonTextSize(15);
                toast.setOnClickWrapper(new OnClickWrapper("PANIC_RETRY", new SuperToast.OnClickListener() {
                    @Override
                    public void onClick(View view, Parcelable token) {
                        mCallback.onRetryWithDifferentRiddle();
                    }
                }));
                dismiss();
                toast.show();
            }
        });

        View askType = baseView.findViewById(R.id.panic_ask_type);
        mAskTypeAnswer = (ViewGroup) baseView.findViewById(R.id.panic_ask_type_answer);
        mAskTypeAnswerText = (TextView) baseView.findViewById(R.id.panic_ask_type_answer_text);
        if (mType == null || mType.getExplanationResId() == 0) {
            askType.setVisibility(View.GONE);
            mAskTypeAnswer.setVisibility(View.GONE);
        } else {
            askType.setVisibility(View.VISIBLE);
            mAskTypeAnswer.setVisibility(View.GONE);
            askType.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onAskType();
                }
            });
        }
        View complainView = baseView.findViewById(R.id.panic_complain);
        complainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                mCallback.onComplain(mImage);
            }
        });
        mShareExperiment = (Button) baseView.findViewById(R.id.panic_share);
        if (mImage == null) {
            mShareExperiment.setVisibility(View.GONE);
        } else {
            mShareExperiment.setVisibility(View.VISIBLE);
            mShareExperiment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareCurrentExperiment();
                }
            });
        }

        final String dontPanicAnims = getResources().getString(R.string.no_panic_anims);
        View dontPanic = baseView.findViewById(R.id.panic_dontpanic);
        dontPanic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    ImageView image = (ImageView) v;
                    Rect bounds = image.getDrawable().getBounds();
                    if (!bounds.contains((int) (event.getX() - v.getWidth() / 2 + bounds.width() / 2), (int) (event.getY() - v.getHeight() / 2 + bounds.height() / 2))) {
                        dismiss();
                    } else {
                        v.clearAnimation();
                        int animNumber = 0;
                        try {
                            animNumber = Integer.parseInt(String.valueOf(dontPanicAnims.charAt
                                    (mFunCounter)));
                        } catch (NumberFormatException nfe) {
                            Log.e("Riddle", "Illegal dontpanicanims string: " + dontPanicAnims);
                        }
                        int anim;
                        switch (animNumber) {
                            case 0:
                                anim = R.anim.panic_fun_zoom;
                                break;
                            case 1:
                                anim = R.anim.panic_fun_roll;
                                break;
                            default:
                                anim = R.anim.panic_fun_bounce;
                                break;
                        }
                        mFunCounter++;
                        mFunCounter %= dontPanicAnims.length();
                        v.startAnimation(AnimationUtils.loadAnimation(getActivity(), anim));
                    }
                }
                return true;
            }
        });
        return new GlasDialog(getActivity(), baseView);
    }

    private void shareCurrentExperiment() {
        mShareExperiment.setEnabled(false);
        mShareExperiment.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.panic_share_progress));
        if (mToShareLink != null) {
            executeShare();
            return;
        }
        if (mImage != null) {
            mShareBuildTask = new AsyncTask<Void, Void, File>() {

                @Override
                protected File doInBackground(Void... params) {

                    mToShare = ImageObfuscator.makeHidden(getResources(), mImage, mType, User
                            .getInstance().getLogo(getResources()), mSolutionInputData);
                    boolean success = mToShare != null;
                    File toShare = null;
                    if (success && !isCancelled()) {
                        success = false;
                        String origin = User.getInstance().getOriginName();
                        String name = (origin == null ? "-" : origin)
                                + "_"
                                + System.currentTimeMillis();
                        if (!name.endsWith(ImageObfuscator.FILE_EXTENSION)) {
                            if (name.endsWith(".png")) {
                                name = name.substring(0, name.length() - 4) + ImageObfuscator.FILE_EXTENSION;
                            } else {
                                name = name + ImageObfuscator.FILE_EXTENSION;
                            }
                        }
                        File tempDir = User.getTempDirectory();
                        Log.d("Image", "Made hidden success, now attempting to save in dir " + tempDir + " with name " + name);
                        if (tempDir != null) {
                            File target = new File(tempDir, name);
                            if (ImageUtil.saveToFile(mToShare, target, Bitmap.CompressFormat.PNG, 100)) {
                                toShare = target;
                                success = true;
                            }
                        }
                    }
                    if (!success && !isCancelled()) {
                        Toast.makeText(getActivity(), R.string.panic_share_failed, Toast.LENGTH_SHORT).show();
                    }
                    return toShare;
                }

                @Override
                public void onPostExecute(File result) {
                    if (result != null) {
                        User.getInstance().uploadPhoto(result, new WebPhotoStorage.UploadListener() {
                            @Override
                            public void onPhotoUploaded(String photoLink, URL photoShareLink) {
                                mToShareLink = photoShareLink;
                                Log.d("Riddle", "On Photo uploaded: " + photoLink + " and " +
                                        photoShareLink);
                                if (VersionSafe.isDetached(NoPanicDialog.this)) {
                                    Log.d("Riddle", "On Photo uploaded and dialog not visible");
                                    return;
                                }
                                executeShare();
                            }

                            @Override
                            public void onPhotoUploadFailed(int error) {
                                if (VersionSafe.isDetached(NoPanicDialog.this)) {
                                    return;
                                }
                                clearShareButton();
                                Toast.makeText(getActivity(), getResources().getString(R.string
                                                .share_failed_upload, error),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }.execute();
        } else {
            clearShareButton();
        }
    }

    private void clearShareButton() {
        mShareExperiment.setEnabled(true);
        mShareExperiment.clearAnimation();
    }

    private void executeShare() {
        clearShareButton();
        if (mToShareLink == null) {
            return;
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, mToShareLink.toString());
        share.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string
                .share_experiment_link_subject));
        getActivity().startActivity(Intent.createChooser(share, getActivity().getResources().getString(R.string.panic_share_dialog)));
    }

    private void onAskType() {
        int explanationResId = mType.getExplanationResId();
        if (mAskTypeAnswer.getVisibility() == View.GONE && explanationResId != 0) {
            mAskTypeAnswer.setVisibility(View.VISIBLE);
            mAskTypeAnswerText.setText(explanationResId);
        } else {
            mAskTypeAnswer.setVisibility(View.GONE);
        }
    }

    private void onAskAuthor() {
        if (mAuthorContainer.getVisibility() == View.GONE) {
            mAuthorContainer.setVisibility(View.VISIBLE);
        } else {
            mAuthorContainer.setVisibility(View.GONE);
        }
    }

    private void setTextIfAvailable(TextView view, int resId, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(getResources().getString(resId, text));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mType != null) {
            outState.putString(KEY_TYPE, mType.getFullName());
        }
        if (mImage != null) {
            outState.putString(KEY_IMAGE, mImage.getHash());
        }
        outState.putString(KEY_SOLUTION_INPUT_DATA, mSolutionInputData);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Activity needs to implement callback!");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }
    }
}
