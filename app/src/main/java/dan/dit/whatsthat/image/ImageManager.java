package dan.dit.whatsthat.image;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manager class for image related things.
 * <ul><li>Syncs new drawables or updated drawable information to the database on startup.
 * <li>Deletes any images from database that are no more accessible due to missing file or being removed from
 * drawables for some reasons.
 * </ul>
 * Created by daniel on 28.03.15.
 */
public class ImageManager {
    protected static final String PREFERENCES_KEY_IMAGE_MANAGER_VERSION = "dan.dit.whatsthat.prefkey_imagemanagerversion";

    private static final Set<Image> INVALID_IMAGES = new HashSet<>(); //TODO remove images from database when some async operation is started
    public static final int PROGRESS_COMPLETE = 100;
    public static final int ESTIMATED_BUNDLE_COUNT = 5; // some value for the progress bar, not too important, >= 1

    private ImageManager() {}

    private static SyncingTask SYNCING_TASK;

    private static void calculateImagedataDeveloper(Context context) {
        //Step1: Load new images from XML and calculate their hash and preferences
        ImageXmlParser parser = new ImageXmlParser();
        List<Image> loadedImages = null;
        try {
            loadedImages = parser.parseNewBundles(context);
            Log.d("Image", "Loaded images: " + loadedImages);
        } catch (IOException e) {
            Log.d("Image", "IOEXCEPTION: " + e);
        } catch (XmlPullParserException e) {
            Log.d("Image", "XML EXCEPTION " + e);
        }
        if (loadedImages != null) {
            //Step 2: Save the updated images to new xml for future use
            ImageXmlWriter xmlWriter = new ImageXmlWriter();
            xmlWriter.writeBundle(context, loadedImages, parser.getHighestReadBundleNumber());
        }
        throw new UnsupportedOperationException("WE ARE DONE BUILDING IMAGES; GTFO.");
    }

    public static void sync(Context context, SynchronizationListener listener) {
        //calculateImagedataDeveloper(context);//FOR DEVELOPER BUILDING ONLY!
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
            ImageXmlParser parser = new ImageXmlParser();
            try {
                parser.parseAndSyncBundles(mContext, new SynchronizationListener() {

                    @Override
                    public void onSyncProgress(int progress) {
                        publishProgress(progress);
                    }

                    @Override
                    public void onSyncComplete() {}

                    @Override
                    public boolean isSyncCancelled() {
                        return isCancelled();
                    }
                });
            } catch (IOException e) {
                Log.e("Image", "IO Error parsing bundles: " + e);
            } catch (XmlPullParserException e) {
                Log.e("Image", "XML Error parsing bundles: " + e);
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


    public static void markInvalidImage(Image image) {
        if (image != null) {
            INVALID_IMAGES.add(image);
        }
    }
}
