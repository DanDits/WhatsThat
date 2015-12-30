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

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * Created by daniel on 08.08.15.
 */
public class Episode implements Compactable {
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

    protected void init(String data) throws CompactedDataCorruptException {
        unloadData(TextUtils.isEmpty(data) ? null : new Compacter(data));
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


    public static String extractEpisodeKey(@NonNull String data) {
        return new Compacter(data).getData(0);
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

    @Override
    public String compact() {
        Compacter cmp = new Compacter(5);
        cmp.appendData(getEpisodeKey());
        cmp.appendData(mCurrMessageIndex);
        cmp.appendData(mNextChildIndex);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 1) {
            return;
        }
        if (!compactedData.getData(0).equals(getEpisodeKey())) {
            throw new CompactedDataCorruptException("Wrong episode for compacted data!" +
                    getEpisodeKey()).setCorruptData(compactedData);
        }
        if (compactedData.getSize() > 1) {
            mCurrMessageIndex = compactedData.getInt(1);
            if (mMessages != null && mMessages.length > 0) {
                mCurrMessageIndex %= mMessages.length;
            }
        } else {
            mCurrMessageIndex = 0;
        }
        if (compactedData.getSize() > 2) {
            mNextChildIndex = compactedData.getInt(2);
            if (mChildren != null && mChildren.size() > 0) {
                mNextChildIndex %= mChildren.size();
            }
        } else {
            mNextChildIndex = 0;
        }
    }
}
