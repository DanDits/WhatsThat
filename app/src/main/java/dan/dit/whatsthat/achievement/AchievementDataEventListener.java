package dan.dit.whatsthat.achievement;

/**
 * An interface for listening to AchievementDataEvents.
 * Created by daniel on 12.05.15.
 */
public interface AchievementDataEventListener {
    /**
     * The data associated with the event changed. The type of the event and
     * changed keys and the changed data can be retrieved through the event.
     * @param event The event that happened.
     */
    void onDataEvent(AchievementDataEvent event);
}
