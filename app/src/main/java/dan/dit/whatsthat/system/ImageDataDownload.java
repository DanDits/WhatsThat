package dan.dit.whatsthat.system;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.BundleCreator;
import dan.dit.whatsthat.image.BundleManager;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.image.ImageXmlParser;
import dan.dit.whatsthat.util.IOUtil;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Created by daniel on 06.07.15.
 */
public class ImageDataDownload {
    private static final int ERROR_CODE_NONE = 0;
    private static final int ERROR_CODE_DOWNLOAD_RESPONSE_NOT_OK = 1001;
    private static final int ERROR_CODE_DOWNLOAD_FILE_ILLEGAL = 1002;
    private static final int ERROR_CODE_DOWNLOAD_IOEXCEPTION = 1003;
    private static final int ERROR_CODE_STORAGE_NOT_AVAILABLE = 2000;
    private static final int ERROR_CODE_SYNC_NOT_DOWNLOADED = 3001;
    private static final int ERROR_CODE_SYNC_UNZIP_FAILED = 3002;
    private static final int ERROR_CODE_SYNC_NO_DATA_FILE = 3003;
    private static final int ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_FNF = 3004;
    private static final int ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_PARSER = 3005;
    private static final int ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_IOE = 3006;
    private static final int ERROR_CODE_SYNC_TO_DATABASE_FAILED = 3007;
    private static final double PROGRESS_WEIGHT_FOR_DOWNLOAD = 0.5;
    private final Feedback mListener;
    private int mEstimatedSizeMB;
    private final String mURL;
    private final String mDataName;
    private final String mOrigin;
    private DownloadTask mDownloadTask;
    private Context mContext;
    private boolean mIsDownloaded;
    private SyncTask mSyncTask;
    private boolean mKeepBundleAfterSync;

    public boolean isWorking() {
        return (mDownloadTask != null && !mDownloadTask.isCancelled())
            || (mSyncTask != null && !mSyncTask.isCancelled());
    }

    public void setKeepBundleAfterSync() {
        mKeepBundleAfterSync = true;
    }

    public boolean isDownloaded() {
        return mIsDownloaded;
    }

    public String getOrigin() {
        return mOrigin;
    }

    public String getDataName() {
        return mDataName;
    }

    public String getUrl() {
        return mURL;
    }

    public String getURLHost() {
        if (TextUtils.isEmpty(mURL)) {
            return null;
        }
        try {
            return new URL(mURL).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public int getEstimatedSize() {
        return mEstimatedSizeMB;
    }

    public void cancel() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        //only allow to cancel download, not syncing
        //if (mSyncTask != null) {
        //    mSyncTask.cancel(true);
        //    mSyncTask = null;
        //}
    }

    public int getEstimatedImages() {
        return -1;
    }

    public interface Feedback extends PercentProgressListener {
        void setIndeterminate(boolean indeterminate);
        void onError(int messageResId, int errorCode);
        void onDownloadComplete();
        void onComplete();
    }

    /**
     * Creates a new ImageDataDownloader that waits for execution to either download
     * a zip containing the imagedata.xml and the actual images or parse and sync the already downloaded
     * file to the database.
     * @param context The application context.
     * @param origin The origin name, usually Image.ORIGIN_IS_THE_APP if from official source or some other name, used as directory name.
     * @param dataName A (better) unique name for all image data available that can be used to create a file name.
     * @param estimatedSizeMB The estimated size of the data in mb in case the data is not available from the connection
     *                        or to show the size before downloading even started.
     * @param url The url to the zip. Can only be null if you are sure that the file to use is already downloaded and existent! Else this
     *            class does nothing.
     * @param feedback The progress of the downloading, parsing and syncing. Reference will be held forever by this object.
     */
    public ImageDataDownload(Context context, String origin, String dataName, int estimatedSizeMB, String url, Feedback feedback) {
        if (context == null || TextUtils.isEmpty(origin) || TextUtils.isEmpty(dataName) || feedback == null) {
            throw new IllegalArgumentException("Null or empty parameter given for " + dataName + " in " + origin + " and url " + url);
        }
        mContext = context.getApplicationContext();
        mEstimatedSizeMB = estimatedSizeMB;
        mOrigin = origin;
        mDataName = dataName;
        mURL = url;
        mListener = feedback;
        File downloaded = checkIfIsDownloaded();
        Log.d("Image", "Data download is downloaded file: " + downloaded);
    }

    public void start() {
        if (isWorking()) {
            Log.e("Image", "Trying to start already working image data process.");
            return;
        }
        if (!mIsDownloaded) {
            // Step 0: download
            Log.d("Image", "Is not downloaded, download required first of url " + mURL);
            startDownload(mURL);
        } else {
            // Step 1: sync
            Log.d("Image", "Is downloaded, directly start syncing.");
            startSync();
        }
    }

    private void startSync() {
        mListener.setIndeterminate(true);
        mSyncTask = new SyncTask();
        mSyncTask.execute();
    }

    public void stop() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        if (mSyncTask != null) {
            mSyncTask.cancel(true);
            mSyncTask = null;
        }
    }

    /**
     * Check if the storage file is available. We cannot be sure that the file is intact
     * and downloaded completely, but this will be found out when trying to unzip and parse.
     */
    private File checkIfIsDownloaded() {
        File storageFile = getTempDownloadFile();
        mIsDownloaded = storageFile != null && storageFile.exists();
        return storageFile;
    }

    private void startDownload(String url) {
        if (url == null) {
            return;
        }
        mListener.setIndeterminate(true);
        mDownloadTask = new DownloadTask();
        mDownloadTask.execute(url);
    }

    private File getStorageDirectory() {
        String storageDirectoryPath = ExternalStorage.getExternalStoragePathIfMounted(Image.IMAGES_DIRECTORY_NAME);
        if (storageDirectoryPath == null) {
            Log.e("Image", "External storage unavailable? " + ExternalStorage.isMounted() + " for " + Image.IMAGES_DIRECTORY_NAME);
            return null;
        }
        storageDirectoryPath += "/" + mOrigin;
        File storageDirectory = new File(storageDirectoryPath);
        if (!storageDirectory.isDirectory() && !storageDirectory.mkdirs()) {
            Log.e("Image", "No storage directory: " + storageDirectory + " storageDirectoryPath: " + storageDirectoryPath);
            return null;
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            new File(storageDirectory, ".nomedia").createNewFile();
        } catch (IOException e) {
            Log.e("Image", "Failed creating nomedia file in directory " + storageDirectory + ": " + e);
        }
        return storageDirectory;
    }

    private File getTempDownloadFile() {
        File storageDirectory = BundleManager.ensureBundleDirectory();
        if (storageDirectory == null) {
            Log.e("Image", "External storage unavailable? " + ExternalStorage.isMounted() + " for " + BundleManager.BUNDLES_DIRECTORY_NAME);
            return null;
        }

        return BundleManager.makeBundleFile(storageDirectory, mOrigin, mDataName);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Integer> {

        private PowerManager.WakeLock mWakeLock;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mWakeLock != null) {
                mWakeLock.release();
            }
            mDownloadTask = null;
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            mDownloadTask = null;
            if (mWakeLock != null) {
                mWakeLock.release();
            }
            if (errorCode != ERROR_CODE_NONE) {
                mListener.onError(R.string.download_article_toast_error_download, errorCode);
            } else {
                mListener.onDownloadComplete();
                startSync();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values != null && values.length > 0) {
                mListener.setIndeterminate(false);
                mListener.onProgressUpdate((int) (values[0] * PROGRESS_WEIGHT_FOR_DOWNLOAD));
            }
        }

        @Override
        protected Integer doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("Image", "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return ERROR_CODE_DOWNLOAD_RESPONSE_NOT_OK;
                }

                int fileLength = connection.getContentLength();
                if (fileLength > 0) {
                    mEstimatedSizeMB = fileLength / (1024 * 1024);
                }
                fileLength = fileLength > 0 ? fileLength : mEstimatedSizeMB * 1024 * 1024;

                if (isCancelled()) {
                    return null;
                }

                File storageFile = getTempDownloadFile();
                if (storageFile == null) {
                    Log.e("Image", "No storage directory or file available");
                    return ERROR_CODE_STORAGE_NOT_AVAILABLE;
                }

                // download the file
                input = new BufferedInputStream(connection.getInputStream(), 1024 * 20); // 20 kb buffer

                try {
                    output = new FileOutputStream(storageFile, false);
                } catch (FileNotFoundException fnf) {
                    Log.e("Image", "Target file on sd card not found: " + fnf + " for name " + storageFile);
                    return ERROR_CODE_DOWNLOAD_FILE_ILLEGAL;
                }
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) > 0) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / (double) fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                Log.e("Image", "Exception during download: " + e);
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
    }

    private class SyncTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mSyncTask = null;
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            mSyncTask = null;
            File storageFile = getTempDownloadFile();
            if (storageFile != null && !mKeepBundleAfterSync && storageFile.delete()) {
                mIsDownloaded = false;
            }
            if (errorCode != ERROR_CODE_NONE) {
                mListener.onError(R.string.download_article_toast_error_syncing, errorCode);
            } else {
                mListener.onComplete();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values != null && values.length > 0) {
                mListener.setIndeterminate(false);
                mListener.onProgressUpdate((int) (PercentProgressListener.PROGRESS_COMPLETE * PROGRESS_WEIGHT_FOR_DOWNLOAD + values[0] * ( 1 - PROGRESS_WEIGHT_FOR_DOWNLOAD)));
            }
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            // Step 0: check if zip is downloaded and existant.. yes, we are so paranoid and think it disappeared
            File storage = checkIfIsDownloaded();
            if (!mIsDownloaded || storage == null) {
                return ERROR_CODE_SYNC_NOT_DOWNLOADED;
            }

            // Step 1: unzip into storage directory
            File storageDirectory = getStorageDirectory();
            if (storageDirectory != null && IOUtil.unzip(storage, storageDirectory)) {
                publishProgress(25);
                // Step 2: find the xml holding required information to parse data
                File dataHolder = null;
                for (File subFile : storageDirectory.listFiles()) {
                    if (subFile != null && subFile.getName().equalsIgnoreCase(mDataName + ".xml")) {
                        dataHolder = subFile;
                        break;
                    }
                }
                if (dataHolder != null) {
                    ImageXmlParser parser;
                    try {
                        parser = ImageXmlParser.parseInput(mContext, new FileInputStream(dataHolder), Integer.MIN_VALUE, false);
                        publishProgress(35);
                    } catch (FileNotFoundException fnf) {
                        return ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_FNF;
                    } catch (IOException ioe) {
                        return ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_IOE;
                    } catch (XmlPullParserException e) {
                        Log.e("Image", "Image data download failed: exception with data file parser exception: " + e);
                        return ERROR_CODE_SYNC_NO_DATA_FILE_EXCEPTION_PARSER;
                    }
                    if (parser != null && parser.syncToDatabase(new ImageManager.SynchronizationListener() {
                            @Override
                            public void onSyncProgress(int progress) {
                                publishProgress((int) (35. + progress * (65./100.)));
                            }

                            @Override
                            public void onSyncComplete() {
                                publishProgress(PercentProgressListener.PROGRESS_COMPLETE);
                            }

                            @Override
                            public boolean isSyncCancelled() {
                                return isCancelled();
                            }
                        })) {
                            if (!dataHolder.delete()) {
                                Log.d("Image", "Failed deleting data holder after successfully syncing. Might leak game data, but whatever.");
                            }
                            Log.d("Image", "Successfully synced bundle. Finally!");
                        } else {
                            return ERROR_CODE_SYNC_TO_DATABASE_FAILED;
                        }
                } else {
                    return ERROR_CODE_SYNC_NO_DATA_FILE;
                }
            } else {
                return ERROR_CODE_SYNC_UNZIP_FAILED;
            }

            return ERROR_CODE_NONE;
        }
    }
}
