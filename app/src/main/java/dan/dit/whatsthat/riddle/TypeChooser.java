package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.testsubject.TestSubjectRiddleType;

/**
 * Created by daniel on 06.05.15.
 */
public class TypeChooser {
    private View mView;
    private TypesAdapter mAdapter;
    private List<TestSubjectRiddleType> mTestSubjectTypes;


    public View makeView(Context context) {
        if (mView != null) {
            return mView;
        }

        mTestSubjectTypes = TestSubject.getInstance().getAvailableTypes();
        mView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.riddle_types, null);
        GridView grid = (GridView) mView.findViewById(R.id.riddle_types_grid);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                TestSubjectRiddleType type = mTestSubjectTypes.get(position);
                type.setSelected(!type.isSelected());
                mAdapter.notifyDataSetChanged();
            }
        });
        mAdapter = new TypesAdapter(context, R.layout.riddle_type);
        grid.setAdapter(mAdapter);
        return mView;
    }

    private class TypesAdapter extends ArrayAdapter<TestSubjectRiddleType> {
        private int mLayoutResourceId;
        private LayoutInflater mInflater;
        private ColorFilter mColorFilter;
        private int mDefaultColor;

        public TypesAdapter(Context context, int layoutResourceId) {
            super(context, layoutResourceId, mTestSubjectTypes);
            mLayoutResourceId = layoutResourceId;
            mColorFilter = new LightingColorFilter(0xff888888, 0x000000);
            mInflater = ((Activity) context).getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(mLayoutResourceId, parent, false);
            }
            TestSubjectRiddleType type = mTestSubjectTypes.get(position);
            ImageView typeIcon = (ImageView) row.findViewById(R.id.imageView);
            typeIcon.setImageResource(type.getIconResId());
            if (type.isSelected()) {
                row.setBackgroundColor(0);
                typeIcon.clearColorFilter();
            } else {
                row.setBackgroundColor(Color.LTGRAY);
                typeIcon.setColorFilter(mColorFilter);
            }
            TextView typeName = (TextView) row.findViewById(R.id.type_name);
            if (type.isSelected()) {
                if (mDefaultColor == 0) {
                    mDefaultColor = typeName.getCurrentTextColor();
                }
                typeName.setTextColor(getContext().getResources().getColor(R.color.positive_text));
            } else if (mDefaultColor != 0) {
                typeName.setTextColor(mDefaultColor);
            }
            typeName.setText(type.getNameResId());
            return row;
        }
    }
}
