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

package dan.dit.whatsthat.testsubject.shopping.sortiment;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.shopping.SubProduct;

/**
 * Created by daniel on 30.07.15.
 */
public class HintProduct extends SubProduct {
    private static final int STATE_SHORT_LENGTH = 15;
    private boolean mStateFullyVisible;
    private PracticalRiddleType mType;
    private int mHintNumber;
    private boolean mAlreadyRead;
    private int mDefaultTextColor;

    public HintProduct(PracticalRiddleType type, int hintNumber, boolean alreadyRead) {
        super(R.layout.hint_product);
        mType = type;
        mHintNumber = hintNumber;
        mAlreadyRead = alreadyRead;
    }

    public void setAlreadyRead(boolean alreadyRead) {
        mAlreadyRead = alreadyRead;
        setText();
    }

    @Override
    public void inflateView(LayoutInflater inflater) {
        super.inflateView(inflater);
        mDefaultTextColor = ((TextView) mView.findViewById(R.id.hint_text)).getCurrentTextColor();
        setText();
    }

    private void setText() {
        TextView view = ((TextView) mView.findViewById(R.id.hint_text));
        CharSequence text = mType.getRiddleHint(mView.getResources(), mHintNumber);
        if (!mAlreadyRead) {
            view.setTextColor(view.getResources().getColor(R.color.important_on_main_background));
            view.setText(R.string.article_hint_not_yet_read);
            return;
        } else if (TextUtils.isEmpty(text)) {
            view.setTextColor(Color.YELLOW);
            view.setText(R.string.article_hint_no_translation);
            return;
        }
        view.setTextColor(mDefaultTextColor);
        if (mStateFullyVisible || text.length() <= STATE_SHORT_LENGTH) {
            view.setText(text);
        } else {
            view.setText(text.subSequence(0, STATE_SHORT_LENGTH) + "...");
        }
    }

    @Override
    public void onClick() {
        if (hasNoView()) {
            return;
        }
        mStateFullyVisible = !mStateFullyVisible;
        setText();
    }
}
