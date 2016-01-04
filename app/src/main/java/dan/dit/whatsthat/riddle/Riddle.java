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

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.storage.RiddleTable;
import dan.dit.whatsthat.util.general.BuildException;
import dan.dit.whatsthat.util.general.PercentProgressListener;

/**
 * Basic riddle class that describes an unsolved riddle instance. Can be decorated by a RiddleGame
 * to create a game that is playable by the user.<br>
 *     As a Riddle keeps the state of input and of any previously played RiddleGame and this is kept in
 *     the database, you should not keep too many instances, some RiddleGames produce an awful lot of data to
 *     accurately reproduce their state.
 */
public class Riddle {
    /**
     * The key to identify a parameter that describes the last visible unsolved riddle that was closed
     * and saved when the app shut down or stopped. Useful on restart to pretent nothing happened.
     */
    public static final String LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY = "dan.dit.whatsthat.unsolved_riddle_id_key";

    /**
     * The key to identify a parameter that describes the last visible unsolved riddle type
     * saved when the riddle fragment shut down or stopped.
     */
    private static final String LAST_VISIBLE_RIDDLE_TYPE_FULL_NAME_ID_KEY = "dan.dit.whatsthat.last_visible_riddle_type_fullname_key";


    /**
     * A constant that is to be used when an id parameter or result value is around that is not a valid id of any riddle.
     */
    public static final long NO_ID = -1L;
    public static final String ORIGIN_REMADE_TO_NEW_TYPE = "REMADE_TYPE";
    private String mSolutionData;
    private int mSolved; // if solved only the core needs to be saved, if not yet started solving no need to save to database
    Core mCore;
    private String mCurrentState; // the current state after closing
    private String mAchievementData; // the achievement data after closing

    public Riddle(String hash, PracticalRiddleType type, String origin) {
        mCore = new Core(origin, hash, type);
        mSolved = Solution.SOLVED_NOTHING;
    }

    public Riddle(String hash, PracticalRiddleType type, String origin, String solutionData) {
        this(hash, type, origin);
        mSolutionData = solutionData;
    }
    private Riddle(Core core, Cursor cursor) throws BuildException {
        buildFromCursor(cursor, core);
    }

    public long getId() {
        return mCore.mId;
    }

    public PracticalRiddleType getType() {
        return mCore.mRiddleType;
    }

    /**
     * Closes this riddle keeping (compacted) state that can be written to persistant memory.
     * @param solvedValue The solved value of the SolutionInput.
     * @param score The score.
     * @param currentState The current state of the RiddleGame.
     * @param achievementData The data that describes the AchievementData used by the RiddleGame.
     * @param solutionData The current state of the SolutionInput.
     */
    public void onClose(int solvedValue, int score, String currentState, String achievementData, String solutionData) {
        mSolved = solvedValue;
        mCore.mScore = score;
        mAchievementData = achievementData;
        mCurrentState = currentState;
        mSolutionData = solutionData;
    }

    /**
     * Returns a drawable to describe this riddle for easier recognition. If possible
     * (that is, the riddle produced and saved a snapshot of itself and this snapshot is still in cache)
     * this is an accurate snapshot of the RiddleGame, else the icon of the riddle type.
     * @param res The resources used to retrieve the snapshot. If null the result might be null.
     * @return A snapshot of the riddle. Might be null (especially if res is null).
     */
    public Drawable getSnapshot(Resources res) {
        Bitmap snapshot = RiddleManager.getFromCache(this);
        if (snapshot != null && res != null) {
            return new BitmapDrawable(res, snapshot);
        } else {
            return mCore.mRiddleType.getIcon(res);
        }
    }

    public String getCurrentState() {
        return mCurrentState;
    }

    public String getSolutionData() {
        return mSolutionData;
    }

    public String getImageHash() {
        return mCore.getImageHash();
    }

    public static void saveLastVisibleRiddleId(Context context, long id) {
        context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                .putLong(LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, id).apply();
    }

    public static long getLastVisibleRiddleId(Context context) {
        return context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).
                getLong(LAST_VISIBLE_UNSOLVED_RIDDLE_ID_KEY, Riddle.NO_ID);
    }

    public static void saveLastVisibleRiddleType(Context context, PracticalRiddleType type) {
        context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).edit()
                .putString(LAST_VISIBLE_RIDDLE_TYPE_FULL_NAME_ID_KEY, type == null ? null : type.getFullName()).apply();
    }

    public static PracticalRiddleType getLastVisibleRiddleType(Context context) {
        String typeFullName =  context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE).
                getString(LAST_VISIBLE_RIDDLE_TYPE_FULL_NAME_ID_KEY, null);
        if (typeFullName != null) {
            return PracticalRiddleType.getInstance(typeFullName);
        }
        return null;
    }

    public int getScore() {
        return mCore.mScore;
    }

    public static int[] loadSolvedRiddlesCountAndScore(Context context, RiddleInitializer.InitTask commandingTask) {
        Cursor cursor = context.getContentResolver().query(ImagesContentProvider.CONTENT_URI_RIDDLE_SOLVED,
                new String[] {RiddleTable.COLUMN_SCORE}, null, null, null);
        final int PROGRESS_FOR_LOADING_CURSOR = 15;
        if (commandingTask.isCancelled()) {return null;}
        commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR);
        cursor.moveToFirst();
        int colScore = cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_SCORE);
        int score = 0;
        int solvedCount = cursor.getCount();
        while (!cursor.isAfterLast()) {
            score += cursor.getInt(colScore);
            cursor.moveToNext();
            if (commandingTask.isCancelled()) {return null;}
            commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR + (PercentProgressListener.PROGRESS_COMPLETE - PROGRESS_FOR_LOADING_CURSOR) * (cursor.getPosition() + 1) / cursor.getCount());
        }
        Log.d("Riddle", "Loaded solved riddles count " + solvedCount + " loaded score " + score);
        cursor.close();
        commandingTask.onInitComplete();
        return new int[]{solvedCount, score};
    }

    public long getTimestamp() {
        return mCore.mTimestamp;
    }

    public String getAchievementData() {
        return mAchievementData;
    }

    public String getOrigin() {
        return mCore.mOrigin;
    }

    /****************** CORE ****************************/
    public static class Core {
        private long mId = NO_ID;
        private long mTimestamp;
        private String mOrigin;
        private String mImageHash;
        private PracticalRiddleType mRiddleType;
        private int mScore;

        /**
         * Creates a new Riddle core for a new riddle.
         * @param origin The origin of the riddle. If empty then origin is the app.
         * @param imageHash The hash of the image used.
         * @param type The type of the riddle.
         */
        private Core(@Nullable String origin,@NonNull String imageHash,@NonNull PracticalRiddleType type) {
            mId = RiddleInitializer.INSTANCE.nextId();
            mTimestamp = System.currentTimeMillis();
            mOrigin = TextUtils.isEmpty(origin) ? Image.ORIGIN_IS_THE_APP : origin;
            mImageHash = imageHash;
            mRiddleType = type;
        }

        private Core(Cursor cursor) throws BuildException {
            buildFromCursor(cursor);
        }

        public String getImageHash() {
            return mImageHash;
        }

        ContentValues makeContentValues() {
            ContentValues cv = new ContentValues();
            cv.put(RiddleTable.COLUMN_ID, mId);
            cv.put(RiddleTable.COLUMN_TIMESTAMP, mTimestamp);
            cv.put(RiddleTable.COLUMN_ORIGIN, mOrigin);
            cv.put(RiddleTable.COLUMN_IMAGEHASH, mImageHash);
            cv.put(RiddleTable.COLUMN_SCORE, mScore);
            cv.put(RiddleTable.COLUMN_RIDDLETYPE, mRiddleType.getFullName());
            return cv;
        }

        private void buildFromCursor(Cursor cursor) throws BuildException {
            mId = cursor.getLong(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_ID));
            mTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_TIMESTAMP));
            mOrigin = cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_ORIGIN));
            mImageHash = cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_IMAGEHASH));
            mScore = cursor.getInt(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_SCORE));
            mRiddleType = PracticalRiddleType.getInstance(cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_RIDDLETYPE)));
            if (!isId(mId) || TextUtils.isEmpty(mImageHash) || mRiddleType == null) {
                throw new BuildException().setMissingData("RiddleCore", "ID " + mId + " riddle type " + mRiddleType + " hash " + mImageHash);
            }
        }

        private static boolean isId(long id) {
            return id != NO_ID;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Core) {
                return this == other || mId == ((Core) other).mId;
            } else {
                return super.equals(other);
            }
        }

        @Override
        public int hashCode() {
            return (int) mId;
        }

        @Override
        public String toString() {
            return "Core " + mId + " of type " + mRiddleType;
        }

        public long getId() {
            return mId;
        }
    }

    @Override
    public String toString() {
        return "Riddle, solved=" + mSolved + ", " + mCore;
    }

    private void buildFromCursor(Cursor cursor, Core core) throws BuildException {
        if (cursor.isAfterLast()) {
            throw new BuildException().setMissingData("Riddle", "No cursor data.");
        }
        mCore = core;
        mSolved = cursor.getInt(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_SOLVED));
        mCurrentState = cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_CURRENTSTATE));
        mAchievementData = cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_ACHIEVEMENTDATA));
        mSolutionData = cursor.getString(cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_SOLUTION));
    }

    private ContentValues makeContentValues() {
        ContentValues cv = mCore.makeContentValues();
        cv.put(RiddleTable.COLUMN_SOLVED, mSolved);
        cv.put(RiddleTable.COLUMN_CURRENTSTATE, mCurrentState);
        cv.put(RiddleTable.COLUMN_ACHIEVEMENTDATA, mAchievementData);
        cv.put(RiddleTable.COLUMN_SOLUTION, mSolutionData);
        return cv;
    }

    private static long saveToDatabase(Context context, ContentValues cv) {
        cv.put(ImagesContentProvider.SQL_INSERT_OR_REPLACE, true);
        Uri uri = context.getContentResolver().insert(ImagesContentProvider.CONTENT_URI_RIDDLE, cv);
        if (uri != null) {
            Log.d("Riddle", "Saved riddle " + cv.get(RiddleTable.COLUMN_IMAGEHASH) + " to database: " + uri.getLastPathSegment());
            return Long.parseLong(uri.getLastPathSegment());
        } else {
            return NO_ID;
        }
    }

    public boolean isSolved() {
        return mSolved == Solution.SOLVED_COMPLETELY;
    }

    public boolean saveToDatabase(Context context) {
        long id = saveToDatabase(context, makeContentValues());
        if (Core.isId(id)) {
            // successfully replaced or newly added
            mCore.mId = id; // set id anyways to prevent accidentally saving riddle in two rows
            return true;
        }
        return false;
    }

    public static boolean deleteFromDatabase(Context context, long id) {
        return Core.isId(id) && context.getContentResolver().delete(ImagesContentProvider.CONTENT_URI_RIDDLE, RiddleTable.COLUMN_ID + "=?", new String[]{Long.toString(id)}) > 0;
    }

    static Map<RiddleType, Set<String>> loadUsedImagesForTypes(@NonNull Context context, @NonNull RiddleInitializer.InitTask commandingTask) {
        Cursor cursor = context.getContentResolver().query(ImagesContentProvider.CONTENT_URI_RIDDLE,
                new String[] {RiddleTable.COLUMN_RIDDLETYPE, RiddleTable.COLUMN_IMAGEHASH}, null, null, RiddleTable.COLUMN_TIMESTAMP + " DESC");
        final int PROGRESS_FOR_LOADING_CURSOR = 15;
        Map<RiddleType, Set<String>> used = new HashMap<>();
        if (commandingTask.isCancelled()) {return used;}
        commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR);
        cursor.moveToFirst();
        int colRiddleType = cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_RIDDLETYPE);
        int colImageHash = cursor.getColumnIndexOrThrow(RiddleTable.COLUMN_IMAGEHASH);
        while (!cursor.isAfterLast()) {
            RiddleType type = PracticalRiddleType.getInstance(cursor.getString(colRiddleType));
            if (type != null) {
                Set<String> typeSet = used.get(type);
                if (typeSet == null) {
                    typeSet = new HashSet<>();
                    used.put(type, typeSet);
                }
                typeSet.add(cursor.getString(colImageHash));
            }
            cursor.moveToNext();
            if (commandingTask.isCancelled()) {return used;}
            commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR + (PercentProgressListener.PROGRESS_COMPLETE - PROGRESS_FOR_LOADING_CURSOR) * (cursor.getPosition() + 1) / cursor.getCount());
        }
        Log.d("Riddle", "Loaded used images for types: " + used);
        cursor.close();
        commandingTask.onInitComplete();
        return used;
    }


    static List<Riddle> loadUnsolvedRiddles(Context context, RiddleInitializer.InitTask commandingTask) {
        Cursor cursor = context.getContentResolver().query(ImagesContentProvider.CONTENT_URI_RIDDLE_UNSOLVED,
                RiddleTable.ALL_COLUMNS, null, null, RiddleTable.COLUMN_TIMESTAMP + " DESC");
        final int PROGRESS_FOR_LOADING_CURSOR = 25;
        List<Riddle> riddles = new ArrayList<>(cursor.getCount());
        if (commandingTask.isCancelled()) {return riddles;}
        commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Core currCore = null;
            try {
                currCore = new Core(cursor);
            } catch (BuildException exp) {
                Log.e("Riddle", "Error building riddle core: " + exp.getMessage());
            }
            if (currCore != null) {
                Riddle curr;
                try {
                    curr = new Riddle(currCore, cursor);
                } catch (BuildException exp) {
                    Log.e("Riddle", "Error building unsolved riddle: " + exp.getMessage());
                    curr = null;
                }
                if (curr != null) {
                    riddles.add(curr);
                }
            }
            cursor.moveToNext();
            if (commandingTask.isCancelled()) {return riddles;}
            commandingTask.onProgressUpdate(PROGRESS_FOR_LOADING_CURSOR + (PercentProgressListener.PROGRESS_COMPLETE - PROGRESS_FOR_LOADING_CURSOR) * (cursor.getPosition() + 1)/ cursor.getCount());

        }
        cursor.close();
        Log.d("Riddle", "Loaded unsolved riddles " + riddles);
        commandingTask.onInitComplete();
        return riddles;
    }

}
