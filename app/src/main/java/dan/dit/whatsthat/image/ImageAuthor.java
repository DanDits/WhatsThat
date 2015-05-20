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

    public ImageAuthor(String name, String source, String license, String title, String extras) {
        mName = name;
        mSource = source == null ? "" : source;
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Empty author name.");
        }
        setLicense(license);
        setTitle(title);
        setExtras(extras);
    }

    public ImageAuthor(Compacter compactedData) throws CompactedDataCorruptException {
        unloadData(compactedData);
    }

    public void setLicense(String license) {
        mLicense = license == null ?  "" : license;
    }

    public void setTitle(String title) {
        mTitle = title == null ? "" : title;
    }

    public void setExtras(String extras) {
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

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData.getSize() < 5) {
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
}
