package dan.dit.whatsthat.preferences;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import dan.dit.whatsthat.image.ImageObfuscator;
import dan.dit.whatsthat.util.webPhotoSharing.CloudinaryController;
import dan.dit.whatsthat.util.webPhotoSharing.PhotoAlbumShareController;

/**
 * Created by daniel on 28.10.15.
 */
public class WebPhotoStorage {
    private static final String KEY_USER_ALBUM_HASH = "dan.dit.whatsthat.PhotoStorageAlbumHash";
    private static final String KEY_USER_UPLOADED_PHOTOS_HASHES = "dan.dit.whatsthat" +
            ".PhotoStorageUploadedPhotosHashes";
    private static final String TEMP_DOWNLOAD_FILE_NAME = "temp_download" + ImageObfuscator.FILE_EXTENSION;
    private static final int ERROR_CODE_NONE = 1;
    private static final int ERROR_CODE_DOWNLOAD_RESPONSE_NOT_OK = 2;
    private static final int ERROR_CODE_STORAGE_NOT_AVAILABLE = 3;
    private static final int ERROR_CODE_DOWNLOAD_FILE_ILLEGAL = 4;
    private static final int ERROR_CODE_DOWNLOAD_IOEXCEPTION = 5;
    private static final int ERROR_CODE_URI_ILLEGAL = 6;
    private static final int ERROR_CODE_UPLOAD_FAILED = 7;
    private final PhotoAlbumShareController mController;
    private String mAlbumHash;
    private boolean mIsWorking;

    public WebPhotoStorage(@NonNull SharedPreferences prefs) {
        mController = new CloudinaryController(); //DumpYourPhoto will probably shut down starting
        // with 2016, do not use it
        checkAlbum(prefs);
    }

    private boolean checkAlbum(@NonNull SharedPreferences prefs) {
        mAlbumHash = prefs.getString(KEY_USER_ALBUM_HASH, null);
        return hasAlbum();
    }

    public boolean hasAlbum() {
        return !TextUtils.isEmpty(mAlbumHash);
    }

    public synchronized boolean isWorking() {
        return mIsWorking;
    }

    public interface UploadListener {
        void onPhotoUploaded(String photoLink, URL photoShareLink);
        void onPhotoUploadFailed(int error);
    }

    public interface DownloadListener {
        void onPhotoDownloaded(File photo);
        void onPhotoDownloadError(int error);
    }

    public synchronized void downloadAsync(@NonNull final String downloadLink, final @Nullable
    DownloadListener listener) {
        if (TextUtils.isEmpty(downloadLink)) {
            throw new IllegalArgumentException("No download link given.");
        }
        if (isWorking()) {
            throw new IllegalStateException("Already working on something.");
        }
        URL url;
        try {
            url = new URL(downloadLink);
        } catch (MalformedURLException e) {
            Log.e("HomeStuff", "No valid download link: " + downloadLink + ": " + e);
            return;
        }
        mIsWorking = true;
        new AsyncTask<URL, Integer, Integer>() {

            @Override
            public void onCancelled(Integer result) {
                mIsWorking = false;
            }

            @Override
            protected Integer doInBackground(URL... params) {
                return executeDownload(params[0], this);
            }

            @Override
            public void onPostExecute(Integer result) {
                if (result != ERROR_CODE_NONE) {
                    if (listener != null) {
                        listener.onPhotoDownloadError(result);
                    }
                } else if (listener != null) {
                    listener.onPhotoDownloaded(getTempStorageFile());
                }
                mIsWorking = false;
            }
        }.execute(url);
    }

    public File download(URL url, DownloadListener listener) {
        Integer result = executeDownload(url, null);
        if (result == null || result != ERROR_CODE_NONE) {
            if (listener != null) {
                listener.onPhotoDownloadError(result == null ? ERROR_CODE_DOWNLOAD_IOEXCEPTION :
                        result);
            }
            return null;
        } else if (listener != null) {
            listener.onPhotoDownloaded(getTempStorageFile());
        }
        return getTempStorageFile();
    }

    private File getTempStorageFile() {
        File storageFile = User.getTempDirectory();
        if (storageFile == null) {
            Log.e("Image", "No storage directory or file available");
            return null;
        }
        storageFile = new File(storageFile, TEMP_DOWNLOAD_FILE_NAME);
        return storageFile;
    }

    private Integer executeDownload(URL url, AsyncTask<URL, Integer, Integer> task) {
        if (url == null) {
            return ERROR_CODE_URI_ILLEGAL;
        }
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        File storageFile;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e("HomeStuff", "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage() + " for url " + url);
                return ERROR_CODE_DOWNLOAD_RESPONSE_NOT_OK;
            }

            int fileLength = connection.getContentLength();

            if (task != null && task.isCancelled()) {
                return null;
            }
            storageFile = getTempStorageFile();
            if (storageFile == null) {
                return ERROR_CODE_STORAGE_NOT_AVAILABLE;
            }
            // download the file, 10kb buffer
            input = new BufferedInputStream(connection.getInputStream(), 1024 * 10);

            try {
                output = new FileOutputStream(storageFile, false);
            } catch (FileNotFoundException fnf) {
                Log.e("HomeStuff", "Target file on sd card not found: " + fnf + " for name " + storageFile);
                return ERROR_CODE_DOWNLOAD_FILE_ILLEGAL;
            }
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) > 0) {
                if (task != null && task.isCancelled()) {
                    input.close();
                    output.close();
                    return null;
                }
                total += count;
                // publishing the progress.... this would require overwriting publishprogress in
                // a custom AsyncTask class as this is a proteted final method
                //if (fileLength > 0 && task != null) // only if total length is known
                    //task.publishProgress((int) (total * 100 / (double) fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            Log.e("HomeStuff", "Exception during download: " + e);
            return ERROR_CODE_DOWNLOAD_IOEXCEPTION;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return ERROR_CODE_NONE;
    }

    public synchronized void uploadPhoto(@NonNull final SharedPreferences prefs, @NonNull final
            File photo, final @Nullable UploadListener listener) {
        if (prefs == null) {
            throw new NullPointerException("No shared preferences given.");
        }
        if (photo == null) {
            throw new IllegalArgumentException("No photo to upload.");
        }
        if (isWorking()) {
            throw new IllegalStateException("Already working on something.");
        }
        mIsWorking = true;
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                if (!hasAlbum()) {
                    String originName = User.getInstance().getOriginName();
                    Log.d("HomeStuff", "Does not have an album, creating new one for " + originName);
                    makeOrUpdateAlbumExecute(prefs, originName == null ? String.valueOf(System
                            .currentTimeMillis
                                    ()) : originName);
                }
                return mController.uploadPhotoToAlbum(mAlbumHash, photo);
            }

            @Override
            public void onPostExecute(String result) {
                if (result != null) {
                    Set<String> photoLinks = new HashSet<>(prefs.getStringSet
                            (KEY_USER_UPLOADED_PHOTOS_HASHES, new
                            HashSet<String>()));
                    photoLinks.add(result);
                    prefs.edit().putStringSet(KEY_USER_UPLOADED_PHOTOS_HASHES, photoLinks).apply();
                    if (listener != null) {
                        listener.onPhotoUploaded(result, mController.makeShareLink
                                (result));
                    }
                } else if (listener != null) {
                    listener.onPhotoUploadFailed(ERROR_CODE_UPLOAD_FAILED);
                }
                mIsWorking = false;
            }
        }.execute();
    }

    public synchronized void makeOrUpdateAlbum(@NonNull final SharedPreferences prefs,
                                       @NonNull final String name) {
        if (prefs == null) {
            throw new NullPointerException("No shared preferences given.");
        }
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("No name given for new album.");
        }
        if (isWorking()) {
            throw new IllegalStateException("Already working on something.");
        }
        mIsWorking = true;
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                makeOrUpdateAlbumExecute(prefs, name);
                return mAlbumHash;
            }

            @Override
            public void onPostExecute(String result) {
                mIsWorking = false;
            }
        }.execute();
    }

    private void makeOrUpdateAlbumExecute(SharedPreferences prefs, String name) {
        if (hasAlbum()) {
            mAlbumHash = mController.updateAlbum(mAlbumHash, name,
                    PhotoAlbumShareController.IS_ALBUM_PUBLIC_DEFAULT);
        } else {
            mAlbumHash = mController.makeAlbum(name);
        }
        if (hasAlbum()) {
            prefs.edit().putString(KEY_USER_ALBUM_HASH, mAlbumHash).apply();
        }
    }


    public @Nullable URL makeDownloadLink(@NonNull Uri shared) {
        return mController.makeDownloadLink(shared);
    }
}
