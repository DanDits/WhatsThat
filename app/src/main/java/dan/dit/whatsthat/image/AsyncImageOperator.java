package dan.dit.whatsthat.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

/**
 * Created by daniel on 30.03.15.
 */
public class AsyncImageOperator {
    public static final int OPERATION_LOAD_IMAGE = 1; // progress from 0 to PROGRESS_COMPLETE
    public static final int OPERATION_DELETE_IMAGE = 2; // progress from 0 to PROGRESS_COMPLETE
    public static final int OPERATION_SAVE_IMAGE = 3;// progress from 0 to PROGRESS_COMPLETE
    public static final int OPERATION_LOAD_HASHES = 4; // doesn't have any progress updates
    public static final int OPERATION_LOAD_BITMAP = 5; // progress from 0 to PROGRESS_COMPLETE
    public static final int PROGRESS_COMPLETE = 100;
    private static final int STATE_STARTED = 1;

    private Callback mCallback;
    private Context mContext;
    private String[] mHashes;
    private Image[] mImages;
    private int mResult;
    private int mOperation;
    private int mProgressIndex;
    private int mStateStarted;
    private AsyncTask mTask;
    private Bitmap[] mBitmaps;

    public Bitmap[] getBitmaps() {
        return mBitmaps;
    }

    /**
     * Returns the result of the operation. This is the amount of successfully deleted or saved images
     * or 0 for other operations.
     * @return The result of the operation.
     */
    public int getResult() {
        return mResult;
    }

    /**
     * Checks if this operator is not yet started with an operation. Once started
     * it will not accept a request until it is explicitly reset.
     * @return If the operator is in a valid state to do an async operation.
     */
    public boolean isAcceptingRequest() {
        return mStateStarted == 0;
    }

    /**
     * Aborts the (running) operation as soon as possible. Does nothing if nothing started or already
     * completed.
     * This is required to be invoked before running another operation on the same operator that has already been used,
     * no matter if it finished yet or not.
     * This is done to make sure an operator doesn't accidentally start again, potentially losing data
     * gained from the first run.
     */
    public void abortOperation() {
        if (mTask != null) {
            mTask.cancel(true);
            mStateStarted = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        abortOperation();
        super.finalize();
    }

    private void checkState() {
        if (!isAcceptingRequest()) {
            throw new IllegalStateException("Already started an operation on operator " + mOperation);
        }
    }

    private void startOperation(int op) {
        checkState();
        mOperation = op;
        mContext = null;
        mCallback = null;
        mStateStarted = STATE_STARTED;
        mHashes = null;
        mImages = null;
        mProgressIndex = 0;
        mResult = 0;
        mTask = null;
        mBitmaps = null;
    }

    /**
     * Loads the images' bitmap from memory if it is no longer available or simply retrieves it from the image
     * if the images' dimension fit the required dimension. The returned images will be guaranteed to be smaller
     * in height than the required height and smaller in width than the required width and attempt to scale
     * exactly to the required dimension if this would not totally destroy the aspect ratio.<br>
     *     If either reqWidth or reqHeight is smaller than or equal to 0 the required dimension is ignored and
     *     the image loaded as is.
     * @param context The application context.
     * @param reqWidth The required width of the loaded bitmaps.
     * @param reqHeight The required height of the loaded bitmaps.
     * @param images The images whose bitmaps are needed.
     * @param cb The callback for progress updates and results, invoked on ui thread.
     */
    public void loadBitmapFromImage(Context context, final int reqWidth, final int reqHeight, Image[] images, Callback cb) {
        startOperation(OPERATION_LOAD_BITMAP);
        mImages = images;
        mCallback = cb;
        mContext = context;
        mTask = new AsyncTask<Image, Integer, Bitmap[]>() {
            @Override
            public Bitmap[] doInBackground(Image... images) {
                mBitmaps = new Bitmap[images.length];
                for (Image image: images) {
                    if (isCancelled()) {
                        break;
                    }
                    mBitmaps[mProgressIndex]=image.getOrLoadBitmap(mContext, reqWidth, reqHeight);
                    mProgressIndex++;
                    publishProgress(mProgressIndex * PROGRESS_COMPLETE / mImages.length);
                }

                return mBitmaps;
            }
            @Override
            public void onProgressUpdate(Integer... progress) {
                if (wantsCallback()) {
                    // use one index lower to ensure to be in bounds, background operation could be
                    // already further and some hash values may not appear and others multiple times
                    mCallback.onProgressUpdate(mImages[mProgressIndex - 1].getHash(), mOperation, progress[0]);
                }
            }

            @Override
            public void onPostExecute(Bitmap[] bitmaps) {
                if (wantsCallback()) {
                    mCallback.onProgressComplete(null, mOperation, mImages);
                }
            }
        }.execute(mImages);
    }

    private void loadImageFromDatabase(Context context, String[] hashes, Callback cb) {
        startOperation(OPERATION_LOAD_IMAGE);
        mContext = context;
        mHashes = hashes;
        mCallback = cb;
        mTask = new AsyncTask<String, Integer, Image[]>() {
            @Override
            public Image[] doInBackground(String... hashes) {
                mImages = new Image[hashes.length];
                for (String hash : hashes) {
                    if (isCancelled()) {
                        break;
                    }
                    mImages[mProgressIndex] = Image.loadFromDatabase(mContext, hash);
                    mProgressIndex++;
                    publishProgress(mProgressIndex * PROGRESS_COMPLETE / hashes.length);
                }
                return mImages;
            }
            @Override
            public void onProgressUpdate(Integer... progress) {
                if (wantsCallback()) {
                    // use one index lower to ensure to be in bounds, background operation could be
                    // already further and some hash values may not appear and others multiple times
                    mCallback.onProgressUpdate(mHashes[mProgressIndex - 1], mOperation, progress[0]);
                }
            }

            @Override
            public void onPostExecute(Image... images) {
                if (wantsCallback()) {
                    mCallback.onProgressComplete(mHashes, mOperation, images);
                }
            }
        }.execute(mHashes);
    }

    public void deleteImageFromDatabase(Context context, String[] hashes, Callback cb) {
        startOperation(OPERATION_DELETE_IMAGE);
        mContext = context;
        mHashes = hashes;
        mCallback = cb;
        mTask = new AsyncTask<String, Integer, Void>() {
            @Override
            public Void doInBackground(String... hashes) {
                for (String hash : hashes) {
                    if (isCancelled()) {
                        break;
                    }
                    if (Image.deleteFromDatabase(mContext, hash)) {
                        mResult++;
                    }
                    mProgressIndex++;
                    publishProgress(mProgressIndex * PROGRESS_COMPLETE / hashes.length);
                }

                return null;
            }
            @Override
            public void onProgressUpdate(Integer... progress) {
                if (wantsCallback()) {
                    // use one index lower to ensure to be in bounds, background operation could be
                    // already further and some hash values may not appear and others multiple times
                    mCallback.onProgressUpdate(mHashes[mProgressIndex - 1], mOperation, progress[0]);
                }
            }

            @Override
            public void onPostExecute(Void nothing) {
                if (wantsCallback()) {
                    mCallback.onProgressComplete(mHashes, mOperation, null);
                }
            }
        }.execute(mHashes);
    }

    public void saveImageToDatabase(Context context, Image[] images, Callback cb) {
        startOperation(OPERATION_SAVE_IMAGE);
        mContext = context;
        mImages = images;
        mCallback = cb;
        mTask = new AsyncTask<Image, Integer, Void>() {
            @Override
            public Void doInBackground(Image... hashes) {
                for (Image image: mImages) {
                    if (isCancelled()) {
                        break;
                    }
                    if (image.saveToDatabase(mContext)) {
                        mResult++;
                    }
                    mProgressIndex++;
                    publishProgress(mProgressIndex * PROGRESS_COMPLETE / mImages.length);
                }

                return null;
            }
            @Override
            public void onProgressUpdate(Integer... progress) {
                if (wantsCallback()) {
                    // use one index lower to ensure to be in bounds, background operation could be
                    // already further and some hash values may not appear and others multiple times
                    mCallback.onProgressUpdate(mImages[mProgressIndex - 1].getHash(), mOperation, progress[0]);
                }
            }

            @Override
            public void onPostExecute(Void nothing) {
                if (wantsCallback()) {
                    mCallback.onProgressComplete(null, mOperation, mImages);
                }
            }
        }.execute(mImages);
    }

    protected void loadAvailableHashes(Context context, Callback cb) {
        startOperation(OPERATION_LOAD_HASHES);
        mContext = context;
        mCallback = cb;
        mTask = new AsyncTask<Void, Void, String[]>() {
            @Override
            public String[] doInBackground(Void... nothings) {
                mHashes = Image.loadAvailableHashes(mContext);
                return mHashes;
            }

            @Override
            public void onPostExecute(String[] hashes) {
                if (wantsCallback()) {
                    mCallback.onProgressComplete(hashes, mOperation, null);
                }
            }
        }.execute();
    }

    private boolean wantsCallback() {
        return mCallback != null;
    }

    public Image[] getImages() {
        return mImages;
    }

    public interface Callback {
        void onProgressUpdate(String imageHash, int operation, int progress);
        void onProgressComplete(String[] imageHashes, int operation, Image[] images);
    }
}
