package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.ui.ViewFlipperManager;

/**
 * Created by daniel on 03.05.15.
 */
public class UnsolvedRiddlesChooser {
    private List<Riddle> mAllRiddlesList;
    private List<Long> mSelectedIds;
    private View mView;

    private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
    private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
    private long mIdToHide;
    private UnsolvedFlipper mUnsolvedFlipper;
    private String[] mTimesOfDay;
    private UnsolvedRiddleSelectionChangeListener mUnsolvedRiddleSelectionListener;
    private TextView mUnsolvedRiddleDate;
    private Resources mResources;

    public int getSelectedRiddlesCount() {
        return mSelectedIds == null ? 0 : mSelectedIds.contains(mIdToHide) ? mSelectedIds.size() - 1 : mSelectedIds.size();
    }

    public interface Callback {
        void openUnsolvedRiddle(Collection<Long> toOpenIds);
    }

    public interface UnsolvedRiddleSelectionChangeListener {
        void onUnsolvedRiddleSelectionChanged();
    }

    public Collection<Long> getSelectedRiddles() {
        if (mIdToHide != Riddle.NO_ID && mSelectedIds.size() > 0) {
            mSelectedIds.add(mIdToHide);
        }
        return mSelectedIds;
    }

    public View makeView(Context context, long idToHide, UnsolvedRiddleSelectionChangeListener listener) {
        if (mView != null) {
            return mView;
        }
        mResources = context.getResources();
        mTimesOfDay = mResources.getStringArray(R.array.date_time_of_day);
        mUnsolvedRiddleSelectionListener = listener;
        mIdToHide = idToHide;
        mAllRiddlesList = new ArrayList<>(RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddles());
        for (int i = 0; i < mAllRiddlesList.size(); i++) {
            if (mAllRiddlesList.get(i).getId() == idToHide) {
                mAllRiddlesList.remove(i);
                break;
            }
        }
        View baseView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.unsolved_riddles, null);
        mView = baseView;
        ((TextView) baseView.findViewById(R.id.unsolved_riddles_title))
                .setText(mResources.getQuantityString(R.plurals.unsolved_riddles_title, mAllRiddlesList.size(), mAllRiddlesList.size()));
        mUnsolvedRiddleDate = (TextView) baseView.findViewById(R.id.unsolved_riddle_date);
        mUnsolvedFlipper = (UnsolvedFlipper) baseView.findViewById(R.id.unsolved_flipper);
        mSelectedIds = new LinkedList<>();
        mUnsolvedFlipper.init(this);
        return baseView;
    }

    public static class UnsolvedFlipper extends ViewFlipperManager {

        private UnsolvedRiddlesChooser mChooser;

        public UnsolvedFlipper(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
        public UnsolvedFlipper(Context context) {
            super(context);
        }
        public UnsolvedFlipper(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void addViews(LayoutInflater inflater, ViewFlipper flipper) {
            for (int i = 0; i < mChooser.mAllRiddlesList.size(); i++) {
                Riddle curr = mChooser.mAllRiddlesList.get(i);
                ImageView icon = (ImageView) inflater.inflate(R.layout.unsolved_riddle, null);
                icon.setImageDrawable(curr.getSnapshot(getResources()));
                flipper.addView(icon);
            }
        }

        @Override
        public void onDisplayedChildChanged(View displayed) {
            Riddle current = mChooser.mAllRiddlesList.get(getDisplayedChild());
            if (current == null) {
                return;
            }
            mChooser.mUnsolvedRiddleDate.setText(mChooser.getDate(new Date(current.getTimestamp())));
            if (mChooser.mSelectedIds.contains(current.getId())) {
                mChooser.mUnsolvedRiddleDate.setTextColor(getResources().getColor(R.color.riddle_type_selected));
                ((ImageView) displayed.findViewById(R.id.riddle_icon)).setImageResource(R.drawable.accept);
            } else {
                mChooser.mUnsolvedRiddleDate.setTextColor(getResources().getColor(R.color.riddle_type_unselected));
                ((ImageView) displayed.findViewById(R.id.riddle_icon)).setImageDrawable(current.getSnapshot(mChooser.mResources));
            }
        }

        private void init(UnsolvedRiddlesChooser chooser) {
            mChooser = chooser;
            init(0);
        }

        @Override
        protected boolean onContentTouched(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                int displayedChild = getDisplayedChild();
                if (displayedChild >= 0 && displayedChild < mChooser.mAllRiddlesList.size()) {
                    Riddle riddle = mChooser.mAllRiddlesList.get(displayedChild);
                    long id = riddle == null ? Riddle.NO_ID : riddle.getId();
                    if (!mChooser.mSelectedIds.remove(id)) {
                        mChooser.mSelectedIds.add(id);
                    }
                    mChooser.mUnsolvedRiddleSelectionListener.onUnsolvedRiddleSelectionChanged();
                    onDisplayedChildChanged(getDisplayedChildView());
                    return true;
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                return true;
            }
            return false;
        }

    }

    private static int getDaysBeforeToday(Date toCheck, Date today) {
        CALENDAR_CHECKER1.setTime(toCheck);
        CALENDAR_CHECKER2.setTime(today);
        return CALENDAR_CHECKER2.get(Calendar.DAY_OF_YEAR) - CALENDAR_CHECKER1.get(Calendar.DAY_OF_YEAR)
                + (CALENDAR_CHECKER2.get(Calendar.YEAR) - CALENDAR_CHECKER1.get(Calendar.YEAR)) * 365;
    }

    private String getDate(Date startDate) {
        Date today = new Date();
        CALENDAR_CHECKER1.setTime(startDate);
        int hour = CALENDAR_CHECKER1.get(Calendar.HOUR_OF_DAY);
        int timeOfDay;
        if (hour < 6 || hour > 22) {
            timeOfDay = 0; // night from 23 to 5
        } else if (hour > 18) {
            timeOfDay = 1; // evening from 19 to 22
        } else if (hour > 14) {
            timeOfDay = 2; // afternoon from 15 to 18
        } else if (hour > 11) {
            timeOfDay = 3; // midday from 12 to 14
        } else if (hour > 5) {
            timeOfDay = 4; // morning from 6 to 11
        } else {
            timeOfDay = 0; // if not sure, its night..
        }
        String time = mTimesOfDay != null && timeOfDay < mTimesOfDay.length ? mTimesOfDay[timeOfDay] : String.valueOf(hour + ":" + CALENDAR_CHECKER1.get(Calendar.MINUTE));

        int dayDiff = getDaysBeforeToday(startDate, today);
        if (dayDiff == 0) {
            return (mResources.getString(R.string.date_today, time));
        } else if (dayDiff == 1) {
            return (mResources.getString(R.string.date_yesterday, time));
        } else {
            return (mResources.getQuantityString(R.plurals.riddle_dialog_unsolved_days_ago, dayDiff, dayDiff, time));
        }
    }
}