package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 03.05.15.
 */
public class UnsolvedRiddlesChooser {
    private RiddlesAdapter mRiddlesAdapter;
    private List<Riddle> mAllRiddlesList;
    private List<Long> mSelectedIds;
    private int mColor;
    private View mView;

    /**
     * A helper DateFormat for subclasses that display the time, e.g. the starttime of a game.
     * Use for a consistent look.
     */
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);

    /**
     * A helper DateFormat for subclasses that display a date, e.g. the startdate of a game.
     * Use for a consistent look.
     */
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
    private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
    private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
    private String mToday;
    private String mYesterday;
    private long mIdToHide;

    public interface Callback {
        void openUnsolvedRiddle(Collection<Long> toOpenIds);
    }

    public Collection<Long> getSelectedRiddles() {
        if (mIdToHide != Riddle.NO_ID && mSelectedIds.size() > 0) {
            mSelectedIds.add(mIdToHide);
        }
        return mSelectedIds;
    }

    public View makeView(Context context, long idToHide) {
        if (mView != null) {
            return mView;
        }
        mToday = context.getResources().getString(R.string.date_today);
        mYesterday = context.getResources().getString(R.string.date_yesterday);
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
        ListView mListView = (ListView) baseView.findViewById(R.id.riddles_list);
        mRiddlesAdapter = new RiddlesAdapter(context, R.layout.unsolved_riddle);
        mListView.setAdapter(mRiddlesAdapter);
        mColor = context.getResources().getColor(R.color.important);
        mSelectedIds = new LinkedList<>();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
                if (!mSelectedIds.remove(id)) {
                    mSelectedIds.add(id);
                }
                mRiddlesAdapter.notifyDataSetChanged();
            }

        });
        return baseView;
    }

    private class RiddlesAdapter extends ArrayAdapter<Riddle> {
        private int mLayoutResourceId;
        private LayoutInflater mInflater;
        public RiddlesAdapter(Context context, int layoutResourceId) {
            super(context, layoutResourceId, mAllRiddlesList);
            mLayoutResourceId = layoutResourceId;
            mInflater = ((Activity)context).getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(mLayoutResourceId, parent, false);
            }

            Riddle riddle = mAllRiddlesList.get(position);
            if (mSelectedIds.contains(riddle.getId())) {
                row.setBackgroundColor(mColor);
            } else {
                row.setBackgroundResource(0);
            }
            ImageView icon = (ImageView) row.findViewById(R.id.riddle_icon);
            icon.setImageDrawable(riddle.getSnapshot(getContext().getResources()));
            TextView createdData = (TextView) row.findViewById(R.id.riddle_date);
            Date date = new Date(riddle.getTimestamp());
            StringBuilder builder = new StringBuilder();
            appendDate(builder, date).append(" - ");
            appendTime(builder, date);
            createdData.setText(builder.toString());
            return row;
        }

        @Override
        public long getItemId(int position) {
            return mAllRiddlesList.get(position).getId();
        }

    }

    /**
     * Helper method to find out if the given dates are on the same day.
     * @param first The first Date.
     * @param second The second Date.
     * @return If <code>true</code> then the dates are on the same day of the year in the same year.
     */
    private static boolean isSameDate(Date first, Date second) {
        CALENDAR_CHECKER1.setTime(first);
        CALENDAR_CHECKER2.setTime(second);
        return CALENDAR_CHECKER1.get(Calendar.DAY_OF_YEAR) == CALENDAR_CHECKER2.get(Calendar.DAY_OF_YEAR)
                && CALENDAR_CHECKER1.get(Calendar.YEAR) == CALENDAR_CHECKER2.get(Calendar.YEAR);
    }

    /**
     * Helper method for the usual format: When the given date is at the current
     * day, then 'today' is displayed, or 'yesterday' accordingly, else the date according
     * to the DATE_FORMAT.
     * @param builder To append the text to.
     * @param startDate The start date of the game.
     */
    private StringBuilder appendDate(StringBuilder builder, Date startDate) {
        Date today = new Date();
        if (isSameDate(today, startDate)) {
            builder.append(mToday);
        } else {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            if (isSameDate(yesterday.getTime(), startDate)) {
                builder.append(mYesterday);
            } else {
                builder.append(DATE_FORMAT.format(startDate));
            }
        }
        return builder;
    }

    private StringBuilder appendTime(StringBuilder builder, Date startDate) {
        builder.append(TIME_FORMAT.format(startDate));
        return builder;
    }

}