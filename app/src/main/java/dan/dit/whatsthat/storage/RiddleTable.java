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

package dan.dit.whatsthat.storage;

import android.database.sqlite.SQLiteDatabase;

import dan.dit.whatsthat.solution.Solution;

/**
 * Table that stores riddles that already done or still in progress by the user.
 * Created by daniel on 24.03.15.
 */
public class RiddleTable {

    /**
     * Table riddles. Created for database 'imageData' on database version 1.
     */
    public static final String TABLE_RIDDLES = "Riddles";

    /**
     * The current SolutionInput or if solved the final solution entered for the riddle.
     */
    public static final String COLUMN_SOLUTION = "solution";

    /**
     * Time of creation
     */
    public static final String COLUMN_TIMESTAMP = "timestamp";

    /**
     * The id of the riddle in the table.
     */
    public static final String COLUMN_ID = "_id";

    /**
     * If the riddle is already solved.
     */
    public static final String COLUMN_SOLVED = "solved";

    /**
     * The image the riddle is about, identified by the image hash, not the image id!
     */
    public static final String COLUMN_IMAGEHASH = "imagehash";

    /**
     * The riddle type that this riddle will use or has used.
     */
    public static final String COLUMN_RIDDLETYPE = "riddletype";

    /**
     * Metadata of this riddle that is used by achievements. Information can be empty
     */
    public static final String COLUMN_ACHIEVEMENTDATA = "achievementdata";

    /**
     * Is this riddle created by the app, sent by another user or a riddle of the user himself.
     */
    public static final String COLUMN_ORIGIN = "origin";

    /**
     * The current state if the riddle type supports saving its state and starting from there.
     */
    public static final String COLUMN_CURRENTSTATE = "currState";

    /**
     * The score gained for solving the riddle.
     */
    public static final String COLUMN_SCORE = "score";

    public static final String SELECTION_UNSOLVED = COLUMN_SOLVED + " < " + Solution.SOLVED_COMPLETELY;
    public static final String SELECTION_SOLVED = COLUMN_SOLVED + " = " + Solution.SOLVED_COMPLETELY;

    //Database creation SQL statement
    private static final String DATABASE_CREATE =
            "create table "
                    + TABLE_RIDDLES
                    + "("
                    + COLUMN_ID + " integer primary key, "
                    + COLUMN_TIMESTAMP + " integer, "
                    + COLUMN_SOLUTION + " text, " // not core
                    + COLUMN_SOLVED + " integer, " // not core
                    + COLUMN_ORIGIN + " text, "
                    + COLUMN_IMAGEHASH + " text not null, "
                    + COLUMN_RIDDLETYPE + " text, "
                    + COLUMN_ACHIEVEMENTDATA + " text, " // not core
                    + COLUMN_CURRENTSTATE + " text, " // not core
                    + COLUMN_SCORE + " integer"
                    + ");";

    public static final String[] ALL_COLUMNS = new String[] {COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_SOLUTION, COLUMN_ORIGIN,
            COLUMN_IMAGEHASH, COLUMN_RIDDLETYPE, COLUMN_ACHIEVEMENTDATA, COLUMN_SCORE, COLUMN_SOLVED, COLUMN_CURRENTSTATE};

    //private constructor to make sure it is never instantiated
    private RiddleTable() {}

    /**
     * Creates the riddle table.
     * @param database The database which the table is created in.
     */
    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    /**
     * If database is getting a version upgrade the table can adapt to fill required data.
     * @param database The upgrading database.
     * @param oldVersion The old version number.
     * @param newVersion The new version number.
     */
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }
}
