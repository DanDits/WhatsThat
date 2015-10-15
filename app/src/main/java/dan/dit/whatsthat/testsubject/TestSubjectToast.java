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

package dan.dit.whatsthat.testsubject;

import android.content.Context;

import com.github.johnpersano.supertoasts.SuperToast;

/**
 * A simple record holding parameters to create a SuperToast from.
 * The main purpose to create this class is to create multiple same or similar SuperToasts
 * or to get over the requirement to hold onto a Context object for creation.
 * Created by daniel on 11.05.15.
 */
public class TestSubjectToast {
    private int mTextResId;
    private int mIconResId;
    public SuperToast.IconPosition mIconPosition;
    private int mGravity;
    private int mOffsetX;
    private int mOffsetY;
    public long mDuration;
    public CharSequence mText;
    public int mTextSize;
    public SuperToast.Animations mAnimations;
    public int mBackground;
    public int mTextColor;
    public int mBackgroundColor;

    public TestSubjectToast(int textId) {
        mTextResId = textId;
    }

    public TestSubjectToast(CharSequence text) {
        mText = text;
    }

    public TestSubjectToast(int gravity, int offsetX, int offsetY, int iconId, int textId, int duration) {
        mTextResId = textId;
        mIconResId = iconId;
        mGravity = gravity;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mDuration = duration;
    }

    public SuperToast makeSuperToast(Context context) {
        if (context == null) {
            return null;
        }
        SuperToast superToast = new SuperToast(context);
        if (mTextResId != 0) {
            superToast.setText(context.getResources().getText(mTextResId));
        } else if (mText != null) {
            superToast.setText(mText);
        }
        if (mTextSize != 0) {
            superToast.setTextSize(mTextSize);
        }
        if (mAnimations != null) {
            superToast.setAnimations(mAnimations);
        }
        if (mBackground != 0) {
            superToast.setBackground(mBackground);
        }
        if (mBackgroundColor != 0) {
            superToast.setBackgroundColor(mBackgroundColor);
        }
        if (mIconResId != 0) {
            superToast.setIcon(mIconResId, mIconPosition == null ? SuperToast.IconPosition.LEFT : mIconPosition);
        }
        if (mTextColor != 0) {
            superToast.setTextColor(mTextColor);
        }
        superToast.setGravity(mGravity, mOffsetX, mOffsetY);
        superToast.setDuration((int) mDuration);
        return superToast;
    }

    @Override
    public String toString() {
        return "Text: " + mText + " text id " + mTextResId;
    }
}
