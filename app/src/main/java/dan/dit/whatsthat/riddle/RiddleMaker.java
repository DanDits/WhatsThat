package dan.dit.whatsthat.riddle;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.games.RiddleGame;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.system.RiddleFragment;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.Dimension;

/**
 * Class responsible for producing a RiddleGame. Works asynchronously, can be used to create specific riddles
 * and specific riddles for an image or just plain creation of a new RiddleGame for a given PracticalRiddleType.
 * Created by daniel on 29.04.15.
 */
public class RiddleMaker {
    private static final int PROGRESS_FOUND_IMAGE_AND_MADE_BASIC_RIDDLE = 25;
    private static final int PROGRESS_LOADED_IMAGE_BITMAP = 40;
    private static final int PROGRESS_INITIALIZED_RIDDLE_BITMAP = 100;

    private static final double PICK_DISTRIBUTION_EXP_LAMBDA = 0.3; // lambda of the exponential distribution http://de.wikipedia.org/wiki/Exponentialverteilung


    private final Random mRandom = new Random();
    private RiddleConfig mMakeRiddleConfig;
    private Worker mWorker;
    private RiddleMakerListener mListener;
    private Context mContext;
    private PracticalRiddleType mType;
    private List<Image> mAllImages;
    private Map<RiddleType, Set<String>> mUsedImagesForTypes;
    private Dimension mBitmapDimension;

    /**
     * The listener used the invoker of the RiddleMaker to get informed
     * about progress, results and failure.
     */
    public interface RiddleMakerListener extends PercentProgressListener {

        /**
         * The making of the riddle is done and successful. The Maker is not considered
         * making anymore at this point.
         * @param riddle The non null riddle that was newly made.
         */
        void onRiddleReady(RiddleGame riddle);

        /**
         * The making failed for some (serious) reason. No future riddle will be delivered.
         */
        void onError(Image image, Riddle riddle);
    }

    /**
     * Cancels the running RiddleMaker, if not running does nothing.
     */
    public void cancel() {
        if (isRunning()) {
            mWorker.cancel(true);
            onClose();
        }
    }

    /**
     * Checks if this RiddleMaker is currently running and making a riddle.
     * If yes, it is not allowed to make any riddles.
     * @return If this maker is running.
     */
    public boolean isRunning() {
        return mWorker != null;
    }

    private void onClose() {
        mWorker = null;
        mListener = null;
        mContext = null;
        mUsedImagesForTypes = null;
        mAllImages = null;
    }

    /**
     * Remakes an old riddle. No parameter must be null, this RiddleMaker must not be running or
     * an exception will be thrown in either case.
     * @param context A context.
     * @param suggestedId Attempts to search the unsolved riddles and to use the riddle that matches this id.
     * @param maxCanvasDim The maximum dimension of the riddle's canvas.
     * @param screenDensity The density of the device.
     * @param listener A listener that is notified about progress, failure and success.
     */
    public void remakeOld(Context context, long suggestedId, Dimension maxCanvasDim, int screenDensity, RiddleMakerListener listener) {
        if (context == null || maxCanvasDim == null || listener == null) {
            throw new IllegalArgumentException("No context, riddle type or listener or canvas dim given.");
        }
        if (isRunning()) {
            throw new IllegalStateException("Already making riddle.");
        }
        List<Riddle> allUnsolvedRiddles = RiddleInitializer.INSTANCE.getRiddleManager().getUnsolvedRiddles();
        Log.d("Riddle", "Suggested id of unsolved riddles: " + suggestedId + " of " + allUnsolvedRiddles);

        // attempt finding the riddle the user suggested
        Riddle riddle = null;
        int riddleIndex = 0;
        for (Riddle rid : allUnsolvedRiddles) {
            if (rid.mCore.getId() == suggestedId) {
                riddle = rid;
                break;
            }
            riddleIndex++;
        }
        if (riddle == null && !allUnsolvedRiddles.isEmpty()) {
            riddleIndex = 0;
            riddle = allUnsolvedRiddles.get(riddleIndex); // get the front (newest)
        }
        if (riddle == null) {
            listener.onError(null, null); // no unsolved riddles
            Log.e("Riddle", "No unsolved riddles during remakeOld");
            return;
        }

        // we need to find the image of the riddle
        Image image = RiddleFragment.ALL_IMAGES.get(riddle.mCore.getImageHash());
        if (image == null) {
            Log.e("Riddle", "Image not available for riddle " + riddle);
            listener.onError(null, riddle); // image not available, this can happen if an extern image is deleted while this riddle is still unsolved
            // we need to get rid of this riddle
            return;
        }

        allUnsolvedRiddles.remove(riddleIndex);
        allUnsolvedRiddles.add(riddle); // add to the end (list changes are backed by riddle manager's list)

        Dimension canvasDim = new Dimension(maxCanvasDim);
        canvasDim.fitInsideWithRatio(riddle.getType().getSuggestedCanvasAspectRatio());
        Dimension bitmapDim = new Dimension(canvasDim);
        bitmapDim.fitInsideWithRatio(riddle.getType().getSuggestedBitmapAspectRatio()); // and then the bitmap inside the canvas

        mListener = listener;
        mContext = context;
        mType = riddle.getType();
        mBitmapDimension = bitmapDim;
        mMakeRiddleConfig = new RiddleConfig(canvasDim.getWidth(), canvasDim.getHeight());
        mMakeRiddleConfig.mScreenDensity = screenDensity;
        mMakeRiddleConfig.setAchievementData(riddle.getType());
        Log.d("Image", "Remaking old riddle " + riddle + " and image " + image + " and bitmapDim " + bitmapDim);
        mWorker = new Worker(riddle, image);
        mWorker.execute();
    }

    /**
     * Opens a specific image riddle bypassing the selection system. No parameter must be null, this RiddleMaker must not be running or
     * an exception will be thrown in either case.
     * @param context A context.
     * @param image The image to use.
     * @param type The type of the riddle
     * @param maxCanvasDim The maximum dimension of the riddle's canvas.
     * @param screenDensity The density of the device.
     * @param listener A listener that is notified about progress, failure and success.
     */
    public void makeSpecific(Context context, Image image, PracticalRiddleType type, Dimension maxCanvasDim, int screenDensity, RiddleMakerListener listener) {
        if (context == null || maxCanvasDim == null || listener == null || image == null || type == null) {
            throw new IllegalArgumentException("No context, riddle type or listener or canvas dim or image given.");
        }
        if (isRunning()) {
            throw new IllegalStateException("Already making riddle.");
        }

        Dimension canvasDim = new Dimension(maxCanvasDim);
        canvasDim.fitInsideWithRatio(type.getSuggestedCanvasAspectRatio());
        Dimension bitmapDim = new Dimension(canvasDim);
        bitmapDim.fitInsideWithRatio(type.getSuggestedBitmapAspectRatio()); // and then the bitmap inside the canvas

        mListener = listener;
        mContext = context;
        mType = type;
        mBitmapDimension = bitmapDim;
        mMakeRiddleConfig = new RiddleConfig(canvasDim.getWidth(), canvasDim.getHeight());
        mMakeRiddleConfig.mScreenDensity = screenDensity;
        mMakeRiddleConfig.setAchievementData(type);
        Log.d("Image", "Cheating a riddle for image " + image + " and bitmapDim " + bitmapDim);
        mWorker = new Worker(null, image);
        mWorker.execute();
    }

    /**
     * Starts making a new riddle. No parameter must be null, this RiddleMaker must not be running or
     * an exception will be thrown in either case.
     * @param context A context.
     * @param type The type a new InitializedRiddle is wanted for.
     * @param maxCanvasDim The maximum dimension of the riddle's canvas.
     * @param screenDensity The density of the device.
     * @param listener A listener that is notified about progress, failure and success.
     */
    public void makeNew(Context context, PracticalRiddleType type, Dimension maxCanvasDim, int screenDensity, RiddleMakerListener listener) {
        if (listener == null || context == null || type == null || maxCanvasDim == null) {
            throw new IllegalArgumentException("No context, riddle type or listener or canvas dim given.");
        }
        if (isRunning()) {
            throw new IllegalStateException("Already making riddle.");
        }
        Dimension canvasDim = new Dimension(maxCanvasDim);
        canvasDim.fitInsideWithRatio(type.getSuggestedCanvasAspectRatio()); // the canvas needs to fit in
        Dimension bitmapDim = new Dimension(canvasDim);
        bitmapDim.fitInsideWithRatio(type.getSuggestedBitmapAspectRatio()); // and then the bitmap inside the canvas

        Log.d("Image", "Making riddle for type " + type + " and bitmapDim " + bitmapDim);
        mAllImages = new LinkedList<>(RiddleFragment.ALL_IMAGES.values());
        mUsedImagesForTypes = RiddleInitializer.INSTANCE.makeUsedImagesCopy();
        mBitmapDimension = bitmapDim;
        mMakeRiddleConfig = new RiddleConfig(canvasDim.getWidth(), canvasDim.getHeight());
        mMakeRiddleConfig.mScreenDensity = screenDensity;
        mMakeRiddleConfig.setAchievementData(type);
        mContext = context;
        mType = type;
        mListener = listener;
        mWorker = new Worker();
        mWorker.execute();
    }

    /**
     * The task that is doing all the work assembling and loading all required data. Steps
     * of building an InitializedRiddle is described within the doInBackground method.
     * Cancelling will not invoke the listener's onError method.
     */
    private class Worker extends AsyncTask<Void, Integer, RiddleGame> implements PercentProgressListener {
        private Riddle mUseRiddle;
        private Image mUseImage;

        /**
         * A worker than searches for an image for the given type. This only fails if the the bitmap
         * for the image got deleted (external image).
         */
        public Worker() {}

        /**
         * Creates a worker that attempts to use the given image and riddle. If a parameter
         * is null then an image/riddle is trying to be found and loaded for the set type.
         * No checks are done if the image and riddle match.
         * @param riddle A riddle to use for the image. Its core should belong to the given image.
         * @param image The image to use.
         */
        public Worker(Riddle riddle, Image image) {
            mUseRiddle = riddle;
            mUseImage = image;
        }

        @Override
        public void onCancelled(RiddleGame nothing) {
            onClose();
        }

        @Override
        protected RiddleGame doInBackground(Void... voids) {

            // Step0: See if we already know an image and if yes use it.
            if (mUseImage == null) {
                // Step1: Find an image for the type.
                mUseImage = findImage(mType, mAllImages, mUsedImagesForTypes);
            }

            // Step2: Check if everything is fine and if yes make the basic riddle for the image.
            if (isCancelled()) {
                return null;
            } else if (mUseImage == null) {
                // we didn't find an image, no images available?!
                Log.e("Riddle", "Making riddle did not find an image for type " + mType + " and all images: " + mAllImages);
                return null;
            }

            if (mUseRiddle == null) {
                mUseRiddle = new Riddle(mUseImage.getHash(), mType, null);
                publishProgress(PROGRESS_FOUND_IMAGE_AND_MADE_BASIC_RIDDLE);
            }

            // Step3: Check if everything is fine and if yes load the image's bitmap.
            if (isCancelled()) {
                return null;
            }
            Bitmap bitmap = mUseImage.loadBitmap(mContext, mBitmapDimension, mType.enforcesBitmapAspectRatio());
            publishProgress(PROGRESS_LOADED_IMAGE_BITMAP);

            // Step4: Check if everything is fine and if yes create and initialize the final riddle.
            if (isCancelled()) {
                return null;
            } else if (bitmap == null) {
                // we didn't find the bitmap for the image?!
                return null;
            }

            RiddleGame riddleGame = mType.makeRiddle(mUseRiddle, mUseImage, bitmap, mContext.getResources(), mMakeRiddleConfig, Worker.this);
            publishProgress(PROGRESS_INITIALIZED_RIDDLE_BITMAP);
            if (isCancelled()) {
                riddleGame.onClose();
                return null;
            }
            return riddleGame;
        }

        @Override
        public void onProgressUpdate(Integer... progress) {
            if (mListener != null) {
                mListener.onProgressUpdate(progress[0]);
            }
        }

        @Override
        public void onProgressUpdate(int progress) {
            publishProgress((PROGRESS_LOADED_IMAGE_BITMAP
                    + (PROGRESS_INITIALIZED_RIDDLE_BITMAP - PROGRESS_LOADED_IMAGE_BITMAP) * progress / PercentProgressListener.PROGRESS_COMPLETE));
        }

        @Override
        public void onPostExecute(RiddleGame riddle) {
            RiddleMakerListener listener = mListener;
            onClose(); //closing removes listener, so keep a temp reference, close before calling ready
            if (riddle != null) {
                listener.onProgressUpdate(PercentProgressListener.PROGRESS_COMPLETE);
                listener.onRiddleReady(riddle);
            } else {
                listener.onError(mUseImage, mUseRiddle);
            }
        }
    }

    /**
     * Searches for an image for the given type in the collection of given images. The used images for type
     * mapping is used to sort the collection by putting a focus on offering new images that fit the riddle type.
     * @param type The type.
     * @param allImages The image collection to search.
     * @param usedImagesForType A mapping of already used images for all riddle types.
     * @return An image. Will only be null if the given collection of images was empty.
     */
    private Image findImage(PracticalRiddleType type, Collection<Image> allImages, Map<RiddleType, Set<String>> usedImagesForType) {
        //assume all available images already loaded and used images for riddles loaded
        /*
         * Attributes for all images:
         * I = interest in image, R = refusal to image
         * 1: I>0, R=0  2: I=0, R=0  3: R>0
         * A: Not yet used, B: Used but not for type, C: already used for type
         * Sort by categories like 1A|2A|1B|2B|1C|2C|3A|3B|3C
         * Sort each category descending by I-R
         * Pick first available
         */

        //ABC defined by usedImagesForType

        //calculate INTEREST I
        final Map<String, Integer> IMAGE_INTEREST = new HashMap<>(allImages.size());
        for (Image image : allImages) {
            IMAGE_INTEREST.put(image.getHash(), type.calculateInterest(image));
        }

        //calculate REFUSAL R
        final Map<String, Integer> IMAGE_REFUSAL = new HashMap<>(allImages.size());
        for (Image image : allImages) {
            IMAGE_REFUSAL.put(image.getHash(), type.calculateRefusal(image));
        }

        // init categories
        final int categoriesCount = 9;
        List<List<Image>> categories = new ArrayList<>(categoriesCount);
        for (int i = 0; i < categoriesCount; i++) {
            categories.add(new ArrayList<Image>());
        }

        final int CategoryAIndex1 = 0;
        final int CategoryAIndex2 = 1;
        final int CategoryBIndex1 = 2;
        final int CategoryBIndex2 = 3;
        final int CategoryCIndex1 = 4;
        final int CategoryCIndex2 = 5;
        final int CategoryAIndex3 = 6;
        final int CategoryBIndex3 = 7;
        final int CategoryCIndex3 = 8;

        // fill categories
        for (Image image : allImages) {
            String key = image.getHash();
            int interest = IMAGE_INTEREST.get(key);
            int refusal = IMAGE_REFUSAL.get(key);

            // calc 1,2,3,A,B,C for current image, easy from this point here
            boolean isOne = interest > 0 && refusal == 0;
            boolean isTwo = interest == 0 && refusal == 0;
            boolean isThree = refusal > 0;
            boolean isA = true;
            boolean isB = false;
            //boolean isC = false;
            for (RiddleType otherType : usedImagesForType.keySet()) {
                if (usedImagesForType.get(otherType).contains(key)) {
                    isA = false; // some type used this image
                    isB = true; // we assume only other types used the image
                    if (otherType.equals(type)) {
                        isB = false; // assumption wrong, this type already used this image
                        break;
                    }
                }
            }
            //isC = !isB

            // put into category, 1A|2A|1B|2B|1C|2C|3A|3B|3C
            int catIndex;
            if (isOne && isA) {
                catIndex = CategoryAIndex1;
            } else if (isTwo && isA) {
                catIndex = CategoryAIndex2;
            } else if (isOne && isB) {
                catIndex = CategoryBIndex1;
            } else if (isTwo && isB) {
                catIndex = CategoryBIndex2;
            } else if (isOne) { // and isC
                catIndex = CategoryCIndex1;
            } else if (isTwo) { // and isC
                catIndex = CategoryCIndex2;
            } else if (isThree && isA) {
                catIndex = CategoryAIndex3;
            } else if (isThree && isB) {
                catIndex = CategoryBIndex3;
            } else {
                catIndex = CategoryCIndex3;
            }
            categories.get(catIndex).add(image);
        }

        // sort categories
        for (int i = 0; i < categoriesCount; i++) {
            Collections.sort(categories.get(i), new Comparator<Image>() {

                @Override
                public int compare(Image image1, Image image2) {
                    int interestRefusal1 = IMAGE_INTEREST.get(image1.getHash()) - IMAGE_REFUSAL.get(image1.getHash());
                    int interestRefusal2 = IMAGE_INTEREST.get(image2.getHash()) - IMAGE_REFUSAL.get(image2.getHash());
                    // the image with the higher value is considered to be better fitting and needs to get to the front of the list
                    return interestRefusal2 - interestRefusal1;
                }
            });
        }

        for (int i = 0; i < categoriesCount; i++) {
            Log.d("Riddle", "Category " + i + " for type " + type + " = " + categories.get(i));
        }
        // find the image
        for (int i = 0; i < categoriesCount; i++) {
            // pick one at random, but make it more likely to pick first one available since everything is sorted
            if (!categories.get(i).isEmpty()) {
                if (i == CategoryAIndex3 || i == CategoryBIndex3 || i == CategoryCIndex3) {
                    mMakeRiddleConfig.mHintImageRefused = true;
                }
                if (i == CategoryBIndex1 || i == CategoryBIndex2 || i == CategoryBIndex3) {
                    mMakeRiddleConfig.mHintImageReusedOtherType = true;
                }
                if (i == CategoryCIndex1 || i == CategoryCIndex2 || i == CategoryCIndex3) {
                    // is C, use uniform distribution to get an image
                    mMakeRiddleConfig.mHintImageReused = true;
                    return categories.get(i).get(mRandom.nextInt(categories.get(i).size()));
                } else {
                    // use inversion method and an exponentially distributed pseudorandom number
                    double rand = mRandom.nextDouble();
                    rand = (-Math.log(1.0 - rand) / PICK_DISTRIBUTION_EXP_LAMBDA);
                    int number = Math.max(0, Math.min((int) rand, categories.get(i).size() - 1));
                    return categories.get(i).get(number);
                }
            }
        }
        return null; // no images given!
    }

}
