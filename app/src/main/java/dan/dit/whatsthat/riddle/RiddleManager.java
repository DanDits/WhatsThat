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

package dan.dit.whatsthat.riddle;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.control.RiddleGame;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.general.ObserverController;
import dan.dit.whatsthat.util.image.Dimension;

/**
 * Class to keep track of all unsolved riddles and making new riddles by using the RiddleMaker class. Retrieve
 * an instance of the RiddleInitializer which needs to be initialized beforehand!
 * Created by daniel on 31.03.15.
 */
public class RiddleManager {
    private final List<Riddle> mAllUnsolvedRiddles = new LinkedList<>();
    private static LruCache<Riddle, Bitmap> mMemoryCache;

    private ObserverController<UnsolvedRiddleListener, Void> mUnsolvedRiddleListenerController
            = new ObserverController<>();
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
                mUnsolvedRiddleListenerController.notifyObservers(null);
            }
        }
    }

    /**
     * There was a change (not further specified) with the unsolved riddles.
     * This informs listeners to recheck the information they need from the manager. No data is
     * passed for the event, information needs to be pulled from observer.
     */
    public interface UnsolvedRiddleListener extends ObserverController.Observer<Void> {

    }

    /**
     * Initializes the manager with a list of unsolved riddles.
     * @param loadedUnsolvedRiddles A non null list of riddles.
     */
    RiddleManager(@NonNull List<Riddle> loadedUnsolvedRiddles) {
        mAllUnsolvedRiddles.addAll(loadedUnsolvedRiddles);
    }

    /**
     * Notifies the manager that the given riddle was solved, removing
     * it from unsolved riddles list.
     * @param riddle The riddle that was solved which must not be null.
     */
    public void onRiddleSolved(@NonNull Riddle riddle) {
        //noinspection ConstantConditions
        if (riddle == null) {
            throw new IllegalArgumentException("On solved riddle with null riddle.");
        }
        mSolvedRiddlesCount++;
        TestSubject.getInstance().addSolvedRiddleScore(riddle.getScore());
        RiddleInitializer.INSTANCE.registerUsedRiddleImage(riddle);
        if (mAllUnsolvedRiddles.remove(riddle)) {
            mUnsolvedRiddleListenerController.notifyObservers(null);
        }
        if (mMemoryCache != null) {
            mMemoryCache.remove(riddle);
        }
        Log.d("Riddle", "Solved riddles: " + mSolvedRiddlesCount + ", unsolved riddles " + mAllUnsolvedRiddles.size());
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
        //noinspection ConstantConditions
        if (riddle == null) {
            throw new IllegalArgumentException("On unsolved riddle with null riddle.");
        }
        RiddleInitializer.INSTANCE.registerUsedRiddleImage(riddle);
        if (!mAllUnsolvedRiddles.contains(riddle)) {
            mAllUnsolvedRiddles.add(riddle);
            mUnsolvedRiddleListenerController.notifyObservers(null);
        }
    }

    /**
     * Registers a listener that is informed about changes to the unsolved riddles.
     * A listener cannot register multiple times.
     * @param listener The listener to register.
     */
    public void registerUnsolvedRiddleListener(UnsolvedRiddleListener listener) {
        mUnsolvedRiddleListenerController.addObserver(listener);
    }

    /**
     * Removes the listener that previously registered to the manager.
     * @param listener The listener to remove.
     * @return True if the given listener was registered and removed.
     */
    public boolean unregisterUnsolvedRiddleListener(UnsolvedRiddleListener listener) {
        return mUnsolvedRiddleListenerController.removeObserver(listener);
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

    public void remakeCurrentWithNewType(final Context context,
                                         final Riddle current,
                                         PracticalRiddleType newType,
                                         Dimension maxCanvasDimension,
                                         int densityDpi,
                                         final RiddleMaker.RiddleMakerListener listener) {
        cancelMakeRiddle();
        mMaker = new RiddleMaker();
        mMaker.remakeCurrentWithNewType(context, current, newType, maxCanvasDimension,
                densityDpi, new RiddleMaker.RiddleMakerListener() {
                    @Override
                    public void onRiddleReady(RiddleGame riddle) {
                        if (Riddle.deleteFromDatabase(context, current.getId())) {
                            onRiddleInvalidated(current);
                        }
                        listener.onRiddleReady(riddle);
                    }

                    @Override
                    public void onError(Image image, Riddle riddle) {
                        listener.onError(image, riddle);
                    }

                    @Override
                    public void onProgressUpdate(int progress) {
                        listener.onProgressUpdate(progress);
                    }
                });
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
