package dan.dit.whatsthat.util.general;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A basic framework work the observer pattern allowing the Observer of something be notified 
 * about certain data events. The event type is variable, if it can be null when given as a 
 * parameter is up to the implementation.<br>
 *     An observer can be added to this controller once and removed at any time. 
 *     Notification goes out to all added observers. This is a lightweight class, no 
 *     synchronization is done. Notification of data events must not trigger new notifications or
 *     modify the list of observers. For a robust implementation with synchronization and some
 *     overhead see {@link RobustObserverController}.<br>
 * Created by daniel on 11.01.16.
 */
public class ObserverController<Observer extends ObserverController.Observer<? super Event>, 
        Event> implements Iterable<Observer> {

    public ObserverController() {
        mObservers = new ArrayList<>();
    }

    public ObserverController(int defaultCapacity) {
        mObservers = new ArrayList<>(defaultCapacity);
    }

    @Override
    public Iterator<Observer> iterator() {
        return mObservers.iterator();
    }

    /**
     * The observers controlled by this class need to implement this interface and react to the
     * given data event.
     * @param <Event> The event that happened and needs to be processed.
     */
    public interface Observer<Event> {
        void onDataEvent(Event event);
    }
    
    protected final List<Observer> mObservers;
    
    /**
     * Adds an observer to be notified on future data events.
     * @param observer The observer to add if not yet added and not null.
     */
    public synchronized void addObserver(Observer observer) {
        if (observer != null && !mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }
    
    /**
     * Removes an observer that will no longer be notified on future data events.
     * @param observer The observer to remove if not yet removed.
     * @return If the observer will be removed.
     */
    public boolean removeObserver(Observer observer) {
        return mObservers.remove(observer);
    }
    
    /**
     * Notifies all associated observers of the given event.
     * @param event The event to notify observers of.
     */
    public void notifyObservers(@Nullable Event event) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onDataEvent(event);
        }
    }
    
}
