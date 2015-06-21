package dan.dit.whatsthat.system;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TabHost;

import java.util.Collection;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.TypeChooser;
import dan.dit.whatsthat.riddle.UnsolvedRiddlesChooser;

/**
 * Created by daniel on 05.05.15.
 */
public class RiddlePickerDialog extends DialogFragment {
    private static final String TAB_UNSOLVED = "TAB_UNSOLVED";
    private static final String TAB_TYPES = "TAB_TYPES";

    private TabHost mTabHost;
    private UnsolvedRiddlesChooser mChooser;
    private TypeChooser mTypeChooser;
    private long mIdToHide;
    private UnsolvedRiddlesChooser.Callback mCallback;

    private class TabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        public TabFactory(Context context) {
            mContext = context;
        }

        @Override
        public View createTabContent(String tag) {
            if (tag != null && tag.equals(TAB_UNSOLVED)) {
                return mChooser.makeView(mContext, mIdToHide);
            } else {
                return mTypeChooser.makeView(mContext);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View baseView = getActivity().getLayoutInflater().inflate(R.layout.riddle_picker, null);
        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        mIdToHide = getArguments().getLong(Riddle.LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, Riddle.NO_ID);
        Log.d("Riddle", "Showing riddle picker dialog, hiding id: " + mIdToHide);

        mChooser = new UnsolvedRiddlesChooser();
        mTypeChooser = new TypeChooser();
        mTabHost = (TabHost) baseView.findViewById(android.R.id.tabhost);
        initializeTabHost();
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setView(baseView)
                .setPositiveButton(R.string.riddles_dialog_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onConfirmation();
                    }
                })
                .setNegativeButton(android.R.string.no, null);
        return builder.create();
    }

    private void onConfirmation() {
        if (mTabHost.getCurrentTabTag().equals(TAB_UNSOLVED)) {
            Collection<Long> riddleIds = mChooser.getSelectedRiddles();
            if (riddleIds != null && !riddleIds.isEmpty()) {
                mCallback.openUnsolvedRiddle(riddleIds);
            }
        }
    }

    private void initializeTabHost() {
        mTabHost.setup();
        addTab(getActivity(), this.mTabHost, this.mTabHost.newTabSpec(TAB_TYPES).setIndicator(getResources().getString(R.string.riddle_dialog_tab_types)));
        if (RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddleCount() > 1) {
            addTab(getActivity(), this.mTabHost, this.mTabHost.newTabSpec(TAB_UNSOLVED).setIndicator(getResources().getString(R.string.riddle_dialog_tab_unsolved)));
        }

    }

    private void addTab(Context context, TabHost tabHost, TabHost.TabSpec tabSpec) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(new TabFactory(context));
        tabHost.addTab(tabSpec);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
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