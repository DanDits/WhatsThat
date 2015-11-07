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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import java.util.Collection;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.TypeChooser;
import dan.dit.whatsthat.riddle.UnsolvedRiddlesChooser;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.ui.GlasDialog;

/**
 * Created by daniel on 05.05.15.
 */
public class RiddlePickerDialog extends DialogFragment {

    private UnsolvedRiddlesChooser mChooser;
    private TypeChooser mTypeChooser;
    private long mIdToHide;
    private UnsolvedRiddlesChooser.Callback mCallback;
    private ViewGroup mContainer;
    private Button mConfirm;

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.RiddlePickerDialogAnimation;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View baseView = getActivity().getLayoutInflater().inflate(R.layout.riddle_picker, null);

        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        mIdToHide = getArguments().getLong(Riddle.LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, Riddle.NO_ID);
        Log.d("Riddle", "Showing riddle picker dialog, hiding id: " + mIdToHide);

        mChooser = new UnsolvedRiddlesChooser();
        mTypeChooser = new TypeChooser();
        baseView.findViewById(R.id.subtitle).setVisibility(TestSubject.getInstance().canChooseNewRiddle() ? View.VISIBLE : View.GONE);
        mContainer = (ViewGroup) baseView.findViewById(R.id.unsolved_and_types_container);
        mTypeChooser.makeView(getActivity(), mContainer);
        if (RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddleCount() > (mIdToHide == Riddle.NO_ID ? 0 : 1)) {
            mContainer.addView(mChooser.makeView(getActivity(), mIdToHide, new UnsolvedRiddlesChooser.UnsolvedRiddleSelectionChangeListener() {
                @Override
                public void onUnsolvedRiddleSelectionChanged() {
                    updateConfirmButton();
                }
            }));
        }
        mConfirm = (Button) baseView.findViewById(R.id.confirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirmation();
            }
        });
        updateConfirmButton();
        return new GlasDialog(getActivity(), baseView);
    }

    private void updateConfirmButton() {
        int unsolvedSelected = mChooser != null ? mChooser.getSelectedRiddlesCount() : 0;
        if (unsolvedSelected > 0) {
            mConfirm.setText(getResources().getQuantityString(R.plurals.riddle_dialog_confirm_unsolveds, unsolvedSelected, unsolvedSelected));
        } else {
            mConfirm.setText(R.string.riddle_dialog_confirm);
        }
    }

    private void onConfirmation() {
        if (mChooser.getSelectedRiddlesCount() > 0) {
            Collection<Long> riddleIds = mChooser.getSelectedRiddles();
            if (riddleIds != null && !riddleIds.isEmpty()) {
                mCallback.openUnsolvedRiddle(riddleIds);
            }
        }
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = ((UnsolvedRiddlesChooser.Callback) activity);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Activity needs to implement UnsolvedRiddlesChooser callback: " + activity);
        }
    }

}