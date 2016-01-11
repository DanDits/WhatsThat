package dan.dit.whatsthat.util.general;

import java.util.ArrayList;
import java.util.List;

/**
 * A robust implementation of a controller of a list of observers. The observers can be added and
 * removed at any time (event during event processing). Processing an event can trigger other
 * events. Adding and removing observers will only take place after all processing is done. So
 * removing an observer during processing an event will not prevent it from being called in case
 * another event is triggered during processing. Processed events that result in nested
 * notifications must do this in the original thread of the first notification, this also
 * includes adding and removing of observers as these operations are synchronized.
 * Created by daniel on 26.11.15.
 */
public class RobustObserverController<Observer extends ObserverController.Observer<?
        super Event>, Event> extends ObserverController<Observer, Event> {

    private List<Observer> mAddedObservers = new ArrayList<>(4);
    private List<Observer> mRemovedObservers = new ArrayList<>(4);
    private int mIsProcessingEventDepth;

    @Override
    public synchronized void addObserver(Observer observer) {
        if (observer != null && !mObservers.contains(observer)
                && !mAddedObservers.contains(observer)) {
            mAddedObservers.add(observer);
        }
    }


    @Override
    public synchronized boolean removeObserver(Observer observer) {
        if (mObservers.contains(observer) && !mRemovedObservers.contains(observer)) {
            mRemovedObservers.add(observer);
            return true;
        }
        return false;
    }
    
    @Override
    public synchronized void notifyObservers(Event event) {
        if (mIsProcessingEventDepth == 0) {
            for (int i = 0; i < mAddedObservers.size(); i++) {
                mObservers.add(mAddedObservers.get(i));
            }
            for (int i = 0; i < mRemovedObservers.size(); i++) {
                mObservers.remove(mRemovedObservers.get(i));
            }
            mAddedObservers.clear();
            mRemovedObservers.clear();
        }
        mIsProcessingEventDepth++;
        super.notifyObservers(event);
        mIsProcessingEventDepth--;
    }

}
