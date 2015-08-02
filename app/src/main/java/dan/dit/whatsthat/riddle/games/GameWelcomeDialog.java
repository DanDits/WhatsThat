package dan.dit.whatsthat.riddle.games;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;

/**
 * Created by daniel on 04.07.15.
 */
public class GameWelcomeDialog extends DialogFragment {
    public static final String KEY_TYPE = "key_type_full_name";
    private PracticalRiddleType mType;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mType = PracticalRiddleType.getInstance(getArguments().getString(KEY_TYPE));
            if (mType == null && savedInstanceState != null) {
                mType = PracticalRiddleType.getInstance(savedInstanceState.getString(KEY_TYPE));
            }
        }
        if (mType == null || !TestSubject.isInitialized()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Error").setMessage("Illegal state.");
            Dialog dialog = builder.create();
            dialog.dismiss();
            return dialog;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = mType.getGameWelcomeView(getActivity());
        if (view != null) {
            builder.setView(view);
        } else {
            CharSequence hint = mType.getRiddleHint(getResources(), TestSubject.getInstance().getCurrentRiddleHintNumber(mType));
            if (!TextUtils.isEmpty(hint)) {
                builder.setTitle(R.string.game_welcome_default_title).setMessage(hint);
            } else {
                builder.setTitle("Hier könnte auch ihr Werbung stehen").setMessage("Das sollte wirklich niemand sehen...");
            }
            builder.setIcon(R.drawable.alien_achieved);
        }
        builder.setPositiveButton(R.string.game_welcome_positive_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TestSubject.getInstance().increaseRiddleHintsDisplayed(mType);
                    }
                }
            )
            .setNegativeButton(R.string.game_welcome_postpone, null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mType != null) {
            outState.putString(KEY_TYPE, mType.getFullName());
        }
    }

    public static GameWelcomeDialog makeInstance(PracticalRiddleType type) {
        GameWelcomeDialog dialog = new GameWelcomeDialog();
        Bundle args = new Bundle();
        args.putString(KEY_TYPE, type.getFullName());
        dialog.setArguments(args);
        return dialog;
    }
}
