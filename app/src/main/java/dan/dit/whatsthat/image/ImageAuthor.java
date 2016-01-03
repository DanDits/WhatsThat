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

package dan.dit.whatsthat.image;

import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;

import dan.dit.whatsthat.util.compaction.Compactable;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;

/**
 * The author of an image needs to be mentioned and sufficiently recognized.
 * This is the information as specified by wiki.creativecommons.org.
 * Created by daniel on 28.03.15.
 */
public class ImageAuthor implements Compactable {
    private String mName; // name of the author, username, pseudonym,..
    private String mSource; // like website, magazine,... if any
    private String mLicense; // under which license is the free image published if any
    private String mTitle; // title of the image if any
    private String mExtras; // like modifications or any additions and remarks if any

    protected ImageAuthor() {
        this(null, null, null, null, null);
    }

    public ImageAuthor(String name, String source, String license, String title, String extras) {
        setName(name);
        setSource(source);
        setLicense(license);
        setTitle(title);
        setExtras(extras);
    }

    public ImageAuthor(Compacter compactedData) throws CompactedDataCorruptException {
        unloadData(compactedData);
    }

    protected void setSource(String source) {
        mSource = source == null ? "" : source;
    }

    protected void setLicense(String license) {
        mLicense = license == null ?  "" : license;
    }

    protected void setTitle(String title) {
        mTitle = title == null ? "" : title;
    }

    protected void setExtras(String extras) {
        mExtras = extras == null ? "" : extras;
    }

    @Override
    public String toString() {
        return "ImageAuthor: " + mName + ", " + mSource + ", " + mLicense + ", " + mTitle + ", " + mExtras;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mName);
        cmp.appendData(mSource);
        cmp.appendData(mLicense);
        cmp.appendData(mTitle);
        cmp.appendData(mExtras);
        return cmp.compact();
    }

    public static final int REQUIRED_DATA_FIELDS_COUNT = 5;
    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData.getSize() < REQUIRED_DATA_FIELDS_COUNT) {
            throw new CompactedDataCorruptException().setCorruptData(compactedData);
        }
        mName = compactedData.getData(0);
        mSource = compactedData.getData(1);
        mLicense = compactedData.getData(2);
        mTitle = compactedData.getData(3);
        mExtras = compactedData.getData(4);
    }

    public String getName() {
        return mName;
    }

    public String getSource() {
        return mSource;
    }

    public String getLicense() {
        return mLicense;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getExtras() {
        return mExtras;
    }

    public String sourceExtractWebsite() {
        if (TextUtils.isEmpty(mSource)) {
            return null;
        }
        try {
            URL url = new URL(mSource);
            return url.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    protected void setName(String name) {
        mName = name == null ? "" : name;
    }
}
