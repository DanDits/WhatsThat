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

package dan.dit.whatsthat.testsubject.intro;

/**
 * Created by daniel on 08.08.15.
 */
public class Episode {
    private final int mMessageResId;
    private final String mMessage;
    protected final Intro mIntro;
    private int mIcon;

    public Episode(Intro intro) {
        this(intro, null);
    }

    public Episode(Intro intro, String message) {
        mIntro = intro;
        mMessageResId = 0;
        mMessage = message;
        if (intro == null) {
            throw new IllegalArgumentException("No intro given.");
        }
    }

    public Episode(Intro intro, int messageResId) {
        mIntro = intro;
        mMessage = null;
        mMessageResId = messageResId;
        if (intro == null) {
            throw new IllegalArgumentException("No intro given.");
        }
    }

    public void setIcon(int icon) {
        mIcon = icon;
    }

    protected boolean isDone() {
        return true;
    }

    protected boolean isMandatory() {
        return false;
    }

    protected void start() {
        if (mMessage != null) {
            mIntro.applyMessage(mMessage);
        } else if (mMessageResId != 0) {
            mIntro.applyMessage(mMessageResId);
        } else {
            mIntro.applyMessage(0);
        }
        mIntro.applyIcon(mIcon);
    }

}
