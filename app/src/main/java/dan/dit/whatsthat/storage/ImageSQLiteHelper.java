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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper that can be used to get a writable database object for the imageData database.
 * The database holds a table for the images {@see ImageTable} and a table for already solved or
 * current riddles {@see RiddleTable}.
 * Created by daniel on 24.03.15.
 */
class ImageSQLiteHelper extends SQLiteOpenHelper{
    //Initial version number = 1
    private static final int DATABASE_VERSION=1;

    //Database name will not change
    private static final String DATABASE_NAME="imageData";

    /**
     * Creates an instance of the SQLiteOpenHelper required to use the imageData database.
     * @param context Context object.
     */
    ImageSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        ImageTable.onCreate(database);
        RiddleTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Any table that requires onUpgrade must be invoked here
        ImageTable.onUpgrade(db, oldVersion, newVersion);
        RiddleTable.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        // Any table that requires onDowngrade must be invoked here
    }

}

