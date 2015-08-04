package dan.dit.whatsthat.system;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.achievement.AchievementProperties;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageAuthor;
import dan.dit.whatsthat.riddle.achievement.holders.MiscAchievementHolder;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 06.05.15.
 */
public class NoPanicDialog extends DialogFragment {
    public static final String KEY_IMAGE = "key_image_hash";
    public static final String KEY_TYPE = "key_type_full_name";
    public static final String KEY_NO_SECRETS = "key_no_secrets";
    private PracticalRiddleType mType;
    private Image mImage;
    private Callback mCallback;
    private TextView mAskTypeAnswer;

    public interface Callback {
        boolean canSkip();
        void onSkip();
        void onComplain(Image image);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View baseView = getActivity().getLayoutInflater().inflate(R.layout.panic_dialog, null);
        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        String imageHash = null;
        String typeName = null;
        boolean noSecrets = false;
        Bundle args = getArguments();
        if (savedInstanceState != null) {
            imageHash = savedInstanceState.getString(KEY_IMAGE);
            typeName = savedInstanceState.getString(KEY_TYPE);
            noSecrets = savedInstanceState.getBoolean(KEY_NO_SECRETS);
        } else if (args != null) {
            imageHash = args.getString(KEY_IMAGE);
            typeName = args.getString(KEY_TYPE);
            noSecrets = args.getBoolean(KEY_NO_SECRETS);
        }
        if (typeName != null) {
            mType = PracticalRiddleType.getInstance(typeName);
        }
        if (imageHash != null) {
            mImage = RiddleFragment.ALL_IMAGES.get(imageHash);
        }

        if (mImage != null) {
            String[] headings = getResources().getStringArray(R.array.panic_author_credit_title);
            View authorContainer = baseView.findViewById(R.id.author_container);
            authorContainer.setVisibility(View.VISIBLE);
            ((TextView) baseView.findViewById(R.id.credits_heading)).setText(headings[(int) (Math.random() * headings.length)]);

            ImageAuthor author = mImage.getAuthor();
            TextView nameView = (TextView) baseView.findViewById(R.id.author_name);
            setTextIfAvailable(nameView, R.string.image_author_name, author.getName());
            nameView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
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
        View askType = baseView.findViewById(R.id.panic_ask_type);
        mAskTypeAnswer = (TextView) baseView.findViewById(R.id.panic_ask_type_answer);
        if (mType == null) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.no_panic_title).setIcon(R.drawable.dontpanic)
                .setView(baseView)
                .setPositiveButton(R.string.panic_dialog_positive, null);
        return builder.create();
    }

    private void onAskType() {
        if (mAskTypeAnswer.getVisibility() == View.GONE) {
            mAskTypeAnswer.setVisibility(View.VISIBLE);
            mAskTypeAnswer.setText(mType.getExplanationResId());
        } else {
            mAskTypeAnswer.setVisibility(View.GONE);
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
