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

package dan.dit.whatsthat.riddle.control;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.HintsView;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.ui.GlasDialog;

/**
 * Created by daniel on 04.07.15.
 */
public class GameWelcomeDialog extends DialogFragment {
    private static final String KEY_TYPE = "key_type_full_name";
    public static final String GAME_WELCOME_DIALOG_TAG = "dan.dit.whatsthat.GameWelcomeDialog";
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
        View baseView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.hints_dialog, null);
        ((HintsView) baseView.findViewById(R.id.hints_view)).setType(mType);
        baseView.findViewById(R.id.hints_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Dialog dialog = new GlasDialog(getActivity(), baseView);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
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
