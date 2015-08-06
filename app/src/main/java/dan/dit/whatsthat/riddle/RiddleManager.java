package dan.dit.whatsthat.riddle;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.image.Dimension;

/**
 * Class to keep track of all unsolved riddles and making new riddles by using the RiddleMaker class. Retrieve
 * an instance of the RiddleInitializer which needs to be initialized beforehand!
 * Created by daniel on 31.03.15.
 */
public class RiddleManager {
    private final List<Riddle> mAllUnsolvedRiddles = new LinkedList<>();
    private static LruCache<Riddle, Bitmap> mMemoryCache;
    private int mLoadedScore;

    private List<UnsolvedRiddleListener> mUnsolvedRiddleListeners = new LinkedList<>();
    private RiddleMaker mMaker;
    private int mSolvedRiddlesCount;

    public static void addToCache(Riddle riddle, Bitmap image) {
        if (riddle == null || image == null) {
            return;
        }
        if (mMemoryCache == null) {
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            final int cacheSize = maxMemory / 20;

            mMemoryCache = new LruCache<Riddle, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(Riddle key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
        mMemoryCache.put(riddle, image);
    }

    public static Bitmap getFromCache(Riddle riddle) {
        if (riddle == null || mMemoryCache == null) {
            return null;
        }
        return mMemoryCache.get(riddle);
    }

    public void onRiddleInvalidated(Riddle riddle) {
        if (riddle != null) {
            if (mMemoryCache != null) {
                mMemoryCache.remove(riddle);
            }
            if (mAllUnsolvedRiddles.remove(riddle)) {
                notifyUnsolvedRiddleListeners();
            }
        }
    }

    public int getLoadedScore() {
        return mLoadedScore;
    }

    /**
     * There was a change (not further specified) with the unsolved riddles.
     * This informs listeners to recheck the information they need from the manager.
     */
    public interface UnsolvedRiddleListener {
        void onUnsolvedRiddlesChanged();
    }

    /**
     * Initializes the manager with a list of unsolved riddles.
     * @param loadedUnsolvedRiddles A non null list of riddles.
     * @param solvedRiddlesCount The amount of solved riddles.
     * @param loadedScore The score earned by all solved riddles.
     */
    RiddleManager(@NonNull List<Riddle> loadedUnsolvedRiddles, int solvedRiddlesCount, int loadedScore) {
        mAllUnsolvedRiddles.addAll(loadedUnsolvedRiddles);
        mSolvedRiddlesCount = solvedRiddlesCount;
        mLoadedScore = loadedScore;
    }

    /**
     * Notifies the manager that the given riddle was solved, removing
     * it from unsolved riddles list.
     * @param riddle The riddle that was solved which must not be null.
     */
    public void onRiddleSolved(@NonNull Riddle riddle) {
        if (riddle == null) {
            throw new IllegalArgumentException("On solved riddle with null riddle.");
        }
        mSolvedRiddlesCount++;
        TestSubject.getInstance().addSolvedRiddleScore(riddle.getScore());
        RiddleInitializer.INSTANCE.registerUsedRiddleImage(riddle);
        if (mAllUnsolvedRiddles.remove(riddle)) {
            notifyUnsolvedRiddleListeners();
        }
        if (mMemoryCache != null) {
            mMemoryCache.remove(riddle);
        }
        Log.d("Riddle", "Solved riddles: " + mSolvedRiddlesCount + ", unsolved riddles " + mAllUnsolvedRiddles.size() + " loaded score " + mLoadedScore);
    }

    /**
     * Retrieves the exact list of unsolved riddles. Changes are possible,
     * no listeners will be notified about them though!
     * @return The list of unsolved riddles.
     */
    List<Riddle> getUnsolvedRiddles() {
        return mAllUnsolvedRiddles;
    }

    /**
     * Gets the amount of unsolved riddles. A riddle is also considered unsolved
     * if it is currently displayed as an InitializedRiddle.
     * @return The amount of unsolved riddles.
     */
    public int getUnsolvedRiddleCount() {
        return mAllUnsolvedRiddles.size();
    }

    /**
     * Notifies the manager about a new unsolved riddle and adds
     * it to the list of unsolved riddles.
     * @param riddle The unsolved riddle which must not be null.
     */
    public void onUnsolvedRiddle(@NonNull Riddle riddle) {
        if (riddle == null) {
            throw new IllegalArgumentException("On unsolved riddle with null riddle.");
        }
        RiddleInitializer.INSTANCE.registerUsedRiddleImage(riddle);
        if (!mAllUnsolvedRiddles.contains(riddle)) {
            mAllUnsolvedRiddles.add(riddle);
            notifyUnsolvedRiddleListeners();
        }
    }

    private void notifyUnsolvedRiddleListeners() {
        for (UnsolvedRiddleListener listener : mUnsolvedRiddleListeners) {
            listener.onUnsolvedRiddlesChanged();
        }
    }

    /**
     * Registers a listener that is informed about changes to the unsolved riddles.
     * A listener cannot register multiple times.
     * @param listener The listener to register.
     */
    public void registerUnsolvedRiddleListener(UnsolvedRiddleListener listener) {
        if (listener != null && !mUnsolvedRiddleListeners.contains(listener)) {
            mUnsolvedRiddleListeners.add(listener);
        }
    }

    /**
     * Removes the listener that previously registered to the manager.
     * @param listener The listener to remove.
     * @return True if the given listener was registered and removed.
     */
    public boolean unregisterUnsolvedRiddleListener(UnsolvedRiddleListener listener) {
        return mUnsolvedRiddleListeners.remove(listener);
    }

    // ************ MAKING RIDDLES; DELEGATING AND MANAGING A RIDDLE MAKER ************************

    /**
     * Checks if a RiddleMaker is currently running.
     * @return If a RiddleMaker is running.
     */
    public boolean isMakingRiddle() {
        return mMaker != null && mMaker.isRunning();
    }

    /**
     * Cancels the running RiddleMaker, does nothing if none is running.
     */
    public void cancelMakeRiddle() {
        if (isMakingRiddle()) {
            mMaker.cancel();
        }
        mMaker = null;
    }

    /**
     * Creates and starts a new RiddleMaker. Cancels previously running makers.
     * @see RiddleMaker#makeNew(android.content.Context, dan.dit.whatsthat.riddle.types.PracticalRiddleType, dan.dit.whatsthat.util.image.Dimension, int, dan.dit.whatsthat.riddle.RiddleMaker.RiddleMakerListener)
     */
    public void makeRiddle(Context context, PracticalRiddleType type, Dimension maxCanvasDimension, int densityDpi, RiddleMaker.RiddleMakerListener listener) {
        cancelMakeRiddle();
        mMaker = new RiddleMaker();
        mMaker.makeNew(context, type, maxCanvasDimension, densityDpi, listener);
    }

    /**
     * Creates and starts a new RiddleMaker. Cancels previously running makers.
     * @see RiddleMaker#remakeOld(android.content.Context, long, dan.dit.whatsthat.util.image.Dimension, int, dan.dit.whatsthat.riddle.RiddleMaker.RiddleMakerListener)
     */
    public void remakeOld(Context context, long suggestedId, Dimension maxCanvasDim, int screenDensity, RiddleMaker.RiddleMakerListener listener) {
        cancelMakeRiddle();
        mMaker = new RiddleMaker();
        mMaker.remakeOld(context, suggestedId, maxCanvasDim, screenDensity, listener);
    }

    /**
     * Creates and starts a new RiddleMaker. Cancels previously running makers.
     * @see RiddleMaker#makeSpecific(android.content.Context, dan.dit.whatsthat.image.Image, dan.dit.whatsthat.riddle.types.PracticalRiddleType, dan.dit.whatsthat.util.image.Dimension, int, dan.dit.whatsthat.riddle.RiddleMaker.RiddleMakerListener)
     */
    public void makeSpecific(Context context, Image image, PracticalRiddleType type, Dimension maxCanvasDim, int screenDensity, RiddleMaker.RiddleMakerListener listener) {
        cancelMakeRiddle();
        mMaker = new RiddleMaker();
        mMaker.makeSpecific(context, image, type, maxCanvasDim, screenDensity, listener);
    }
}
