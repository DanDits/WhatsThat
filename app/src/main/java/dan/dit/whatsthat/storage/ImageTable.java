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

/**
 * Table that stores the reference to the image, metadata to the image like the author,
 * solution words or preferred riddle types. Also saves a hash and if the image is obfuscated.
 * Created by daniel on 24.03.15.
 */
public class ImageTable {

    /**
     * Table images. Created for database 'imageData' on database version 1.
     */
    public static final String TABLE_IMAGES = "Images";


    /**
     * The hash of the image data.
     */
    public static final String COLUMN_HASH = "hash";

    /**
     * Time of creation
     */
    public static final String COLUMN_TIMESTAMP = "timestamp";

    /**
     * The name (filename) of the image.
     */
    public static final String COLUMN_NAME = "name";

    /**
     * The name to the drawable resource if it is an intern image.
     */
    public static final String COLUMN_RESNAME = "resname";


    /**
     * The savelocation if it is an extern image. A relative path relative to Image.EXTERN_IMAGES_PATH,
     * inside a directory named after the origin.
     */
    public static final String COLUMN_SAVELOC = "saveloc";


    /**
     * Solution words to the image in multiple languages.
     */
    public static final String COLUMN_SOLUTIONS = "solutions";


    /**
     * Preferred riddle types for this image.
     */
    public static final String COLUMN_RIDDLEPREFTYPES = "riddleprefs";

    /**
     * Refused riddle types for this image.
     */
    public static final String COLUMN_RIDDLEREFUSEDTYPES = "riddlerefused";

    /**
     * Legal notices to the author or source of the image including name, source, license, title
     * and modifications (see wiki.creativecommons.org).
     */
    public static final String COLUMN_AUTHOR = "author";

    /**
     * The origin like who sent this, did the user add this himself or by the app itself.
     * Has to be a file system compatible name that suits a directory name.
     */
    public static final String COLUMN_ORIGIN = "origin";

    /**
     * The version number of the obfuscation or anything else if not obfuscated.
     */
    public static final String COLUMN_OBFUSCATION = "obf";

    /**
     * The average (a)rgb color of the image's bitmap.
     */
    public static final String COLUMN_AVERAGE_COLOR = "avcolor";

    public static final String[] ALL_COLUMNS = new String[] {COLUMN_HASH, COLUMN_TIMESTAMP, COLUMN_OBFUSCATION, COLUMN_AUTHOR,
            COLUMN_NAME, COLUMN_ORIGIN, COLUMN_RESNAME, COLUMN_SAVELOC, COLUMN_SOLUTIONS,  COLUMN_RIDDLEPREFTYPES, COLUMN_RIDDLEREFUSEDTYPES, COLUMN_AVERAGE_COLOR};

    //Database creation SQL statement
    private static final String DATABASE_CREATE =
            "create table "
            + TABLE_IMAGES
            + "("
            + COLUMN_HASH + " text not null primary key, "
            + COLUMN_TIMESTAMP + " integer, "
            + COLUMN_NAME + " text not null, "
            + COLUMN_RESNAME + " text, "
            + COLUMN_SAVELOC + " text, "
            + COLUMN_SOLUTIONS + " text, "
            + COLUMN_RIDDLEPREFTYPES + " text, "
            + COLUMN_RIDDLEREFUSEDTYPES + " text, "
            + COLUMN_AUTHOR + " text, "
            + COLUMN_ORIGIN + " text, "
            + COLUMN_OBFUSCATION + " integer, "
            + COLUMN_AVERAGE_COLOR + " integer"
            + ");";

    //private constructor to make sure it is never instantiated
    private ImageTable() {}

    /**
     * Creates the images table.
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
