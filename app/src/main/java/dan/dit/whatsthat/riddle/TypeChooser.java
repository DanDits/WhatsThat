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

package dan.dit.whatsthat.riddle;

import android.app.Activity;
import android.content.Context;
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


    public View makeView(Context context, ViewGroup parent) {
        if (mView != null) {
            return mView;
        }

        mTestSubjectTypes = TestSubject.getInstance().getAvailableTypes();
        mView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.riddle_types, parent);
        GridView grid = (GridView) mView.findViewById(R.id.riddle_types_grid);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
            row.setTag(type);
            ImageView typeIcon = (ImageView) row.findViewById(R.id.imageView);
            typeIcon.setImageResource(type.getIconResId());
            if (type.isSelected()) {
                typeIcon.clearColorFilter();
                row.setBackgroundResource(R.drawable.riddle_type_selected);
            } else {
                typeIcon.setColorFilter(mColorFilter);
                row.setBackgroundResource(0);
            }
            TextView typeName = (TextView) row.findViewById(R.id.type_name);
            if (type.isSelected()) {
                typeName.setTextColor(getContext().getResources().getColor(R.color.riddle_type_selected));
            } else {
                typeName.setTextColor(getContext().getResources().getColor(R.color.riddle_type_unselected));
            }
            typeName.setText(type.getNameResId());
            return row;
        }
    }
}
