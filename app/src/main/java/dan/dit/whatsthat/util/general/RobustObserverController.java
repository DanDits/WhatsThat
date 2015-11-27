package dan.dit.whatsthat.util.general;

import java.util.ArrayList;
import java.util.List;

/**
 * A robust implementation of a controller of a list of observers. The observers can be added and
 * removed at any time (event during event processing). Processing an event can trigger other
 * events. Adding and removing observers will only take place after all processing is done. So
 * removing an observer during processing an event will not prevent it from being called in case
 * another event is triggered during processing.
 * Created by daniel on 26.11.15.
 */
public class RobustObserverController<Listener extends RobustObserverController.Observer<?
        super Event>, Event> {

    private final List<Listener> mListeners = new ArrayList<>();
    private List<Listener> mAddedListeners = new ArrayList<>(4);
    private List<Listener> mRemovedListeners = new ArrayList<>(4);
    private int mIsProcessingEventDepth;

    /**
     * The listeners controlled by this class need to implement this interface and react to the
     * given data event.
     * @param <Event> The event that happened and needs to be processed.
     */
    public interface Observer<Event> {
        void onDataEvent(Event event);
    }

    /**
     * Adds a listener to be notified on future data events.
     * @param listener The listener to add if not yet added.
     */
    public void addListener(Listener listener) {
        if (!mListeners.contains(listener) && !mAddedListeners.contains(listener)) {
            mAddedListeners.add(listener);
        }
    }

    /**
     * Removes a listener that will no longer be notified on future data events.
     * @param listener The listener to remove if not yet removed.
     * @return If the listener will be removed before next notification.
     */
    public boolean removeListener(Listener listener) {
        if (mListeners.contains(listener) && !mRemovedListeners.contains(listener)) {
            mRemovedListeners.add(listener);
            return true;
        }
        return false;
    }

    /**
     * Notifies all associated listeners of the given event.
     * @param event The event to notify listeners of.
     */
    public synchronized void notifyListeners(Event event) {
        if (mIsProcessingEventDepth == 0) {
            for (int i = 0; i < mAddedListeners.size(); i++) {
                mListeners.add(mAddedListeners.get(i));
            }
            for (int i = 0; i < mRemovedListeners.size(); i++) {
                mListeners.remove(mRemovedListeners.get(i));
            }
            mAddedListeners.clear();
            mRemovedListeners.clear();
        }
        mIsProcessingEventDepth++;
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDataEvent(event);
        }
        mIsProcessingEventDepth--;
    }

}
