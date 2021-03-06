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

package dan.dit.whatsthat.achievement;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.general.ObserverController;

/**
 * An achievement is managed by this manager class which saves relevant achievement's data
 * to permanent storage and loads previously saved content. The manager must be initialized
 * with the application context before being able to retrieve the singleton. The manager must
 * be told to commit changed data before the application context is closed or saving is not disturbing.
 * Created by daniel on 12.05.15.
 */
public class AchievementManager implements AchievementDataEventListener {

    private static final String ACHIEVEMENT_PREFERENCES_FILE = "dan.dit.whatsthat.achievement_data";
    public static final int CHANGED_PROGRESS = 0;
    public static final int CHANGED_TO_COVERED = 1;
    public static final int CHANGED_TO_DISCOVERED = 2;
    public static final int CHANGED_TO_ACHIEVED_AND_UNCLAIMED = 3;
    public static final int CHANGED_GOT_CLAIMED = 4;
    public static final int CHANGED_TO_RESET = 5;
    public static final int CHANGED_OTHER = 6;

    private static AchievementManager INSTANCE = null;
    private final SharedPreferences mPrefs;
    private Set<AchievementData> mManagedChangedData;
    private Set<Achievement> mChangedAchievements;
    private final ObserverController<OnAchievementChangedListener, AchievementChangeEvent>
            mAchievementChangedEventController = new ObserverController<>();

    /**
     * Event class for the observers of achievement state.
     */
    public static class AchievementChangeEvent {
        private final Achievement mAchievement;
        private int mChangedHint;

        AchievementChangeEvent(Achievement achievement) {
            mAchievement = achievement;
            if (mAchievement == null) {
                throw new IllegalArgumentException("Null achievement givenf or change event.");
            }
        }

        private void setChangedHint(int changedHint) {
            mChangedHint = changedHint;
        }

        public Achievement getAchievement() {
            return mAchievement;
        }

        public int getChangedHint() {
            return mChangedHint;
        }
    }

    public interface OnAchievementChangedListener extends ObserverController.Observer<AchievementChangeEvent> {

    }
    private AchievementManager(Context applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("No context given.");
        }
        applicationContext = applicationContext.getApplicationContext();
        mPrefs = applicationContext.getSharedPreferences(ACHIEVEMENT_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mManagedChangedData = new HashSet<>();
        mChangedAchievements = new HashSet<>();
    }

    /**
     * Initializes the manager by the given application context. Future invocations to getInstance()
     * will be valid and return the singleton.
     * @param applicationContext The application context.
     */
    public static void initialize(Context applicationContext) {
        INSTANCE = new AchievementManager(applicationContext);
    }

    /**
     * Returns the singleton if it was previously initialized. Else the state is illegal.
     * @return The singleton.
     */
    public static AchievementManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AchievementManager not yet initialized!");
        }
        return INSTANCE;
    }

    /**
     * Closes manager at the end to commit changes to data and achievements. Does nothing
     * if no instance initialized.
     */
    public static synchronized void commit() {
        if (INSTANCE != null && (!INSTANCE.mManagedChangedData.isEmpty() || !INSTANCE.mChangedAchievements.isEmpty())) {
            SharedPreferences.Editor editor = INSTANCE.mPrefs.edit();
            for (AchievementData data : INSTANCE.mManagedChangedData) {
                editor.putString(data.mName, data.compact());
            }
            for (Achievement achievement : INSTANCE.mChangedAchievements) {
                achievement.addData(editor);
            }
            editor.apply();
            Log.d("Achievement", "Commiting achievement manager"
                    + " , saving " + INSTANCE.mChangedAchievements.size() + " changed " +
                            "achievements.");
            INSTANCE.mManagedChangedData.clear();
            INSTANCE.mChangedAchievements.clear();
        }
    }

    public void addAchievementChangedListener(OnAchievementChangedListener listener) {
        mAchievementChangedEventController.addObserver(listener);
    }

    public void removeAchievementChangedListener(OnAchievementChangedListener listener) {
        mAchievementChangedEventController.removeObserver(listener);
    }

    /**
     * An achievement notifies its manager that it changed its state or data.
     * @param achievement The achievement that changed.
     * @param changedHint The hint of what kind of change happened to the achievement.
     */
    public void onChanged(final Achievement achievement, final int changedHint) {
        if (achievement != null) {
            mChangedAchievements.add(achievement);
            AchievementChangeEvent event = achievement.getStateChangeEvent();
            event.setChangedHint(changedHint);
            mAchievementChangedEventController.notifyObservers(event);
        }
    }

    /**
     * The given AchievementData will in future be managed by this manager. This is handy
     * for the AchievementData that cannot or does not want to manage its own permanent data.
     * @param data The not null data to be managed.
     */
    public void manageAchievementData(AchievementData data) {
        // for achievement data that is not managed by the using instance but rather left alone in its existence somewhere
        // this offers a general interface for saving and loading
        data.addListener(this);
    }

    @Override
    public void onDataEvent(AchievementDataEvent event) {
        mManagedChangedData.add(event.getChangedData());
    }

    /**
     * Loads the data event with the given name from permanent storage.
     * @param name The name identifying the data.
     * @return The compacted data to reconstruct the data event.
     */
    public Compacter loadDataEvent(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String data = mPrefs.getString(name, null);
        if (data == null) {
            return null;
        }
        return new Compacter(data);
    }

    /**
     * Returns the shared preferences used to save and load managed data.
     * @return The shared preferences.
     */
    SharedPreferences getSharedPreferences() {
        return mPrefs;
    }

}
