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

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by daniel on 08.08.15.
 */
public class Episode {
    public static final String EPISODE_KEY_SEPARATOR = "_";
    private final String[] mMessages;
    protected int mCurrMessageIndex;
    protected final Intro mIntro;
    private final String mEpisodeKey;
    private int mIcon;
    private int mNextChildIndex;
    private List<Episode> mChildren = new LinkedList<>();

    public Episode(@NonNull String episodeKey, Intro intro, String[] messages) {
        mEpisodeKey = episodeKey;
        mIntro = intro;
        if (intro == null) {
            throw new IllegalArgumentException("No intro given.");
        }
        if (mEpisodeKey == null) {
            throw new IllegalArgumentException("No episode key given.");
        }
        mMessages = messages;
    }

    public Episode(@NonNull String episodeKey, Intro intro, String message) {
        this(episodeKey, intro, new String[] {message});
    }

    public Episode setIcon(int icon) {
        mIcon = icon;
        return this;
    }

    protected boolean isDone() {
        return true;
    }

    protected boolean isMandatory() {
        return false;
    }

    protected void init(String key) {
        if (TextUtils.isEmpty(key)) {
            mCurrMessageIndex = 0;
            return;
        }
        String indexStr = key.substring(key.indexOf(EPISODE_KEY_SEPARATOR) + EPISODE_KEY_SEPARATOR
                .length());
        int index = 0;
        try {
            index = Integer.parseInt(indexStr);
        } catch (NumberFormatException nfe) {
            Log.e("HomeStuff", "Error parsing index of episode key: " + nfe);
        }
        mCurrMessageIndex = index;
        if (mMessages != null && mMessages.length > 0) {
            mCurrMessageIndex %= mMessages.length;
        }
    }

    protected void start() {
        if (mMessages != null && mCurrMessageIndex < mMessages.length) {
            mIntro.applyMessage(mMessages[mCurrMessageIndex]);
        } else {
            mIntro.applyMessage(0);
        }
        mIntro.applyIcon(mIcon);
    }

    public @NonNull
    String getEpisodeKey() {
        return mEpisodeKey;
    }


    public static String extractEpisodeKey(@NonNull String key) {
        return key.substring(0, key.indexOf(EPISODE_KEY_SEPARATOR));
    }

    public @NonNull String getKey() {
        return mEpisodeKey + EPISODE_KEY_SEPARATOR + mCurrMessageIndex;
    }

    @Override
    public int hashCode() {
        return getEpisodeKey().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Episode) {
            return getEpisodeKey().equals(((Episode) other).getEpisodeKey());
        }
        return super.equals(other);
    }

    public Episode getChild(int index) {
        return mChildren.get(index);
    }

    public int getChildrenCount() {
        return mChildren.size();
    }

    public void addChild(Episode child) {
        mChildren.add(child);
    }

    public boolean hasNextMessage() {
        return mCurrMessageIndex < mMessages.length - 1;
    }
    public Episode next(int childIndex) {
        if (hasNextMessage()) {
            mCurrMessageIndex++;
            return this;
        }
        if (mNextChildIndex >= mChildren.size()) {
            return null;
        }
        mCurrMessageIndex = 0; // reset current episode in case it returns in later cycle
        if (childIndex >= 0 && childIndex < mChildren.size()) {
            return mChildren.get(childIndex);
        }
        Episode next = mChildren.get(mNextChildIndex);
        mNextChildIndex++;
        mNextChildIndex %= mChildren.size(); // cycle
        return next;
    }
}
