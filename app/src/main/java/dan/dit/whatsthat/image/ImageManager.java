package dan.dit.whatsthat.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Manager class for image related things.
 * <ul><li>Syncs new drawables or updated drawable information to the database on startup.
 * <li>Deletes any images from database that are no more accessible due to missing file or being removed from
 * drawables for some reasons.
 * </ul>
 * Created by daniel on 28.03.15.
 */
public class ImageManager {
    private static final String PREFERENCES_KEY_IMAGE_MANAGER_VERSION = "dan.dit.whatsthat.prefkey_imagemanagerversion";
    private static boolean SYNCING_TASK_HAS_FINISHED_THIS_RUN;

    private ImageManager() {}

    private static SyncingTask SYNCING_TASK;

    public static boolean calculateImagedataDeveloperFromFile(Context context, String fileName) {
        if (!BuildConfig.DEBUG) {
            Log.e("HomeStuff", "Using developer method in non debug build: calculateImagedataDeveloperFromFile");
            return false;
        }
        //Step1: Load new images from XML and calculate their hash and preferences
        ImageXmlParser parser = null;
        File file = new File(ExternalStorage.getExternalStoragePathIfMounted(null), fileName);
        Log.d("Image", "Starting calculating imagedata from file: " + file);
        try {
            Log.d("Image", "Starting parsing uncompiled images and compiling them.");
            parser = ImageXmlParser.parseInput(context, new FileInputStream(file), Integer.MIN_VALUE, true);
            Log.d("Image", "Loaded bundles: " + parser.getReadBundlesCount());
        } catch (IOException e) {
            Log.d("Image", "IOEXCEPTION: " + e);
        } catch (XmlPullParserException e) {
            Log.d("Image", "XML EXCEPTION " + e);
        }
        if (parser != null && parser.getReadBundlesCount() > 0) {
            //Step 2: Save the updated images to new xml for future use
            Log.d("Image", "Loaded " + parser.getReadBundlesCount() + " bundles, now writing again to compiled file.");
            List<Integer> bundleNumers = new ArrayList<>(parser.getReadBundleNumbers());
            Collections.sort(bundleNumers);
            List<List<Image>> bundleImages = new ArrayList<>();
            List<String> bundleOrigins = new ArrayList<>();
            for (Integer bundleNumber : bundleNumers) {
                bundleImages.add(parser.getBundle(bundleNumber));
                bundleOrigins.add(parser.getOrigin(bundleNumber));
            }
            if (bundleImages.size() > 0) {
                ImageXmlWriter.writeBundle(context, bundleImages, bundleNumers, bundleOrigins.get(0), file);
                return true;
            }
        }
        return false;
    }
    /**
     * DEVELOPER METHOD; NOT FOR RELEASE.
     * <br>Parses the imagedata_uncompiled xml file, calculates hashs and preferences if required
     * and writes each read bundle to its own file. Will terminate the program with an Exception.
     * @param context A context.
     */
    public static boolean calculateImagedataDeveloper(Context context) {
        if (!BuildConfig.DEBUG) {
            Log.e("HomeStuff", "Using developer method in non debug build:  calculateImagedataDeveloper");
            return false; // do nothing, but better this should never be called
        }
        //Step1: Load new images from XML and calculate their hash and preferences
        ImageXmlParser parser = null;
        try {
            Log.d("Image", "Starting parsing uncompiled images and compiling them.");
            parser = ImageXmlParser.parseInput(context, context.getResources().openRawResource(R.raw.imagedata_uncompiled), 0, true);
            Log.d("Image", "Loaded bundles: " + parser.getReadBundlesCount());
        } catch (IOException e) {
            Log.d("Image", "IOEXCEPTION: " + e);
        } catch (XmlPullParserException e) {
            Log.d("Image", "XML EXCEPTION " + e);
        }
        if (parser != null && parser.getReadBundlesCount() > 0) {
            //Step 2: Save the updated images to new xml for future use
            Log.d("Image", "Loaded " + parser.getReadBundlesCount() + " bundles, now writing again to compiled file.");
            for (Integer bundleNumber : parser.getReadBundleNumbers()) {
                ImageXmlWriter.writeBundle(context, Collections.singletonList(parser.getBundle(bundleNumber)), Collections.singletonList(bundleNumber), parser.getOrigin(bundleNumber), null);
            }
            return true;
        }
        return false;
    }

    public static void sync(Context context, SynchronizationListener listener) {
        cancelSync();
        SYNCING_TASK = new SyncingTask(context, listener);
        SYNCING_TASK.execute();
    }

    public static void unregisterSynchronizationListener() {
        if (SYNCING_TASK != null) {
            SYNCING_TASK.mListener = null;
        }
    }

    public static void cancelSync() {
        if (SYNCING_TASK != null) {
            SYNCING_TASK.mListener = null;
            SYNCING_TASK.cancel(true);
            SYNCING_TASK = null;
        }
    }

    public static boolean isSyncing() {
        return SYNCING_TASK != null;
    }

    public static boolean isSynced() {
        return SYNCING_TASK_HAS_FINISHED_THIS_RUN;
    }

    private static class SyncingTask extends AsyncTask<Void, Integer, Void> {

        private SynchronizationListener mListener;
        private Context mContext;

        public SyncingTask(Context context, SynchronizationListener listener) {
            mContext = context;
            mListener = listener;
            if (context == null) {
                throw new IllegalArgumentException("Null context given.");
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SharedPreferences prefs = mContext.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
            int currBundleNumber = prefs.getInt(ImageManager.PREFERENCES_KEY_IMAGE_MANAGER_VERSION, 0);

            ImageXmlParser parser = null;
            try {
                parser = ImageXmlParser.parseInput(mContext, mContext.getResources().openRawResource(R.raw.imagedata), currBundleNumber + 1, false);
            } catch (IOException e) {
                Log.e("Image", "IO Error parsing bundles: " + e);
            } catch (XmlPullParserException e) {
                Log.e("Image", "XML Error parsing bundles: " + e);
            }
            if (parser != null) {

                SynchronizationListener listener = new SynchronizationListener() {

                    @Override
                    public void onSyncProgress(int progress) {
                        publishProgress(progress);
                    }

                    @Override
                    public void onSyncComplete() {}

                    @Override
                    public boolean isSyncCancelled() {
                        return isCancelled();
                    }};

                if (parser.syncToDatabase(listener)) {
                    int highestNumber = parser.getHighestReadBundleNumber();
                    if (highestNumber > currBundleNumber) {
                        prefs.edit().putInt(ImageManager.PREFERENCES_KEY_IMAGE_MANAGER_VERSION, highestNumber).apply();
                    }
                    Log.d("Image", "Parsed and synced bundles: Loaded images from XML with highest read number= " + highestNumber);
                } else {
                    Log.d("Image", "Parsing for image sync: no new bundles, cancelled or failed.");
                }
                if (!isCancelled()) {
                    SYNCING_TASK_HAS_FINISHED_THIS_RUN = true;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (mListener != null && progress != null && progress.length > 0) {
                mListener.onSyncProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void nothing) {
            Log.d("Image", "ImageManager on sync done.");
            SYNCING_TASK = null;
            if (mListener != null) {
                mListener.onSyncComplete();
                mListener = null;
            }
        }
    }

    public interface SynchronizationListener {
        void onSyncProgress( int progress); // image progress in percent
        void onSyncComplete();
        boolean isSyncCancelled();
    }


    public static void removeInvalidImageImmediately(Context context, Image image) {
        if (image != null) {
            image.deleteFromDatabase(context);
        }
    }
}
