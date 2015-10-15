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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Content provider to query, delete, update or insert data of the ImagesSQLiteHelper's database
 * for the ImageTable and RiddleTable. The insert method can and should be used with the SQL_INSERT_OR_REPLACE
 * value set to true in the content values to replace any existing entry.
 */
public class ImagesContentProvider extends ContentProvider {

    private static final String AUTHORITY =
            "dan.dit.whatsthat.provider.images";

    /**
     * Add this to content values with boolean true to replace instead of insert data when using
     * the content provider's insert() method.
     */
    public static final String SQL_INSERT_OR_REPLACE = "sql_insert_or_replace";

    /**
     * Content URI to insert (or replace) an image, query/delete/update all images.
     */
    public static final Uri CONTENT_URI_IMAGE =
            Uri.parse("content://" + AUTHORITY + "/" + ImageTable.TABLE_IMAGES);

    /**
     * Content URI to insert (or replace) a riddle, query/delete/update all riddles.
     */
    public static final Uri CONTENT_URI_RIDDLE =
            Uri.parse("content://" + AUTHORITY + "/" + RiddleTable.TABLE_RIDDLES);

    /**
     * Content URI to delete, query or update a solved riddle (selection with RiddleTable.SELECTION_SOLVED).
     * Should not be used to update or delete, as solved riddles are considered immutable data.
     */
    public static final Uri CONTENT_URI_RIDDLE_SOLVED =
            Uri.parse("content://" + AUTHORITY + "/" + RiddleTable.TABLE_RIDDLES + "/s");

    /**
     * Content URI to delete, query or update an unsolved riddle (selection with RiddleTable.SELECTION_UNSOLVED).
     * Should mainly be used for querying, as the insert with replace enabled method does the required
     * decision making if the riddle is new or should only be updated.
     */
    public static final Uri CONTENT_URI_RIDDLE_UNSOLVED =
            Uri.parse("content://" + AUTHORITY + "/" + RiddleTable.TABLE_RIDDLES + "/u");


    private static final int IMAGE_ALL= 1;
    private static final int IMAGE_BY_HASH = 2;
    private static final int RIDDLE_ALL = 3;
    private static final int RIDDLE_SOLVED = 4;
    private static final int RIDDLE_UNSOLVED = 5;
    private static final UriMatcher sURIMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, ImageTable.TABLE_IMAGES, IMAGE_ALL);
        sURIMatcher.addURI(AUTHORITY, ImageTable.TABLE_IMAGES + "/*",
                IMAGE_BY_HASH);
        sURIMatcher.addURI(AUTHORITY, RiddleTable.TABLE_RIDDLES, RIDDLE_ALL);
        sURIMatcher.addURI(AUTHORITY, RiddleTable.TABLE_RIDDLES + "/s", RIDDLE_SOLVED);
        sURIMatcher.addURI(AUTHORITY, RiddleTable.TABLE_RIDDLES + "/u", RIDDLE_UNSOLVED);
    }

    private ImageSQLiteHelper mDatabase;

    /**
     * Creates the uri the access a single image of the ImageTable, which is identified by the image' hash.
     * @param imageHash The image hash.
     * @return The content uri to access the image through this content provider.
     */
    public static Uri buildImageUri(String imageHash) {
        return Uri.parse("content://" + AUTHORITY + "/" + ImageTable.TABLE_IMAGES + "/" + imageHash);
    }

    @Override
    public boolean onCreate() {
        mDatabase = new ImageSQLiteHelper(getContext());
        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
        int rowsDeleted;

        switch (uriType) {
            case IMAGE_ALL:
                rowsDeleted = sqlDB.delete(ImageTable.TABLE_IMAGES,
                        selection,
                        selectionArgs);
                break;

            case IMAGE_BY_HASH:
                String hash = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(ImageTable.TABLE_IMAGES,
                            appendSelection(ImageTable.COLUMN_HASH + "=?", selection),
                            appendSelectionArgs(hash, selectionArgs));
                break;
            case RIDDLE_ALL:
                rowsDeleted = sqlDB.delete(RiddleTable.TABLE_RIDDLES,
                        selection, selectionArgs);
                break;
            case RIDDLE_SOLVED:
                rowsDeleted = sqlDB.delete(RiddleTable.TABLE_RIDDLES, appendSelection(RiddleTable.SELECTION_SOLVED, selection), selectionArgs);
                break;
            case RIDDLE_UNSOLVED:
                rowsDeleted = sqlDB.delete(RiddleTable.TABLE_RIDDLES, appendSelection(RiddleTable.SELECTION_UNSOLVED, selection), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);

        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();

        boolean replace = false;
        if ( values.containsKey( SQL_INSERT_OR_REPLACE )) {
            replace = values.getAsBoolean( SQL_INSERT_OR_REPLACE );

            // Clone the values object, so we don't modify the original.
            // This is not strictly necessary, but depends on your needs
            values = new ContentValues( values );

            // Remove the key, so we don't pass that on to db.insert() or db.replace()
            values.remove( SQL_INSERT_OR_REPLACE );
        }

        long id;
        switch (uriType) {
            case IMAGE_ALL:
                if (replace) {
                    id = sqlDB.replace(ImageTable.TABLE_IMAGES, null, values);
                } else {
                    id = sqlDB.insert(ImageTable.TABLE_IMAGES,
                            null, values);
                }
                if (id != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.parse(ImageTable.TABLE_IMAGES + "/" + values.getAsString(ImageTable.COLUMN_HASH));
                }
                break;
            case RIDDLE_ALL:
                if (replace) {
                    id = sqlDB.replace(RiddleTable.TABLE_RIDDLES, null, values);
                } else {
                    id = sqlDB.insert(RiddleTable.TABLE_RIDDLES, null, values);
                }
                if (id != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.parse(RiddleTable.TABLE_RIDDLES + "/" + id);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: "
                        + uri);
        }

        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        int uriType = sURIMatcher.match(uri);
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Cursor cursor;
        switch (uriType) {
            case IMAGE_BY_HASH:
                queryBuilder.setTables(ImageTable.TABLE_IMAGES);
                String hash = uri.getLastPathSegment();
                cursor = queryBuilder.query(mDatabase.getReadableDatabase(),
                            projection, appendSelection(ImageTable.COLUMN_HASH + "=?", selection),
                            appendSelectionArgs(hash, selectionArgs), null, null,
                            sortOrder);
                break;
            case IMAGE_ALL:
                queryBuilder.setTables(ImageTable.TABLE_IMAGES);
                cursor = queryBuilder.query(mDatabase.getReadableDatabase(),
                        projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case RIDDLE_ALL:
                queryBuilder.setTables(RiddleTable.TABLE_RIDDLES);
                cursor = queryBuilder.query(mDatabase.getReadableDatabase(),
                        projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case RIDDLE_SOLVED:
                queryBuilder.setTables(RiddleTable.TABLE_RIDDLES);
                cursor = queryBuilder.query(mDatabase.getReadableDatabase(),
                        projection, appendSelection(RiddleTable.SELECTION_SOLVED, selection), selectionArgs, null, null,
                        sortOrder);
                break;
            case RIDDLE_UNSOLVED:
                queryBuilder.setTables(RiddleTable.TABLE_RIDDLES);
                cursor = queryBuilder.query(mDatabase.getReadableDatabase(),
                        projection, appendSelection(RiddleTable.SELECTION_UNSOLVED, selection), selectionArgs, null, null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        cursor.setNotificationUri(getContext().getContentResolver(),
                uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
        int rowsUpdated;

        switch (uriType) {
            case IMAGE_ALL:
                rowsUpdated =
                        sqlDB.update(ImageTable.TABLE_IMAGES,
                                values,
                                selection,
                                selectionArgs);
                break;
            case IMAGE_BY_HASH:
                String hash = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(ImageTable.TABLE_IMAGES,
                                    values,
                                    appendSelection(ImageTable.COLUMN_HASH + "=?", selection),
                                    appendSelectionArgs(hash, selectionArgs));
                break;
            case RIDDLE_ALL:
                rowsUpdated = sqlDB.update(RiddleTable.TABLE_RIDDLES,
                        values, selection, selectionArgs);
                break;
            case RIDDLE_SOLVED:
                rowsUpdated = sqlDB.update(RiddleTable.TABLE_RIDDLES, values,
                        appendSelection(RiddleTable.SELECTION_SOLVED, selection), selectionArgs);
                break;
            case RIDDLE_UNSOLVED:
                rowsUpdated = sqlDB.update(RiddleTable.TABLE_RIDDLES, values,
                        appendSelection(RiddleTable.SELECTION_UNSOLVED, selection), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " +
                        uri);
        }
        getContext().getContentResolver().notifyChange(uri,
                null);
        return rowsUpdated;
    }

    //appends the selection to the own selection statement, if there is any selection
    private String appendSelection(String own, String selection) {
        if (TextUtils.isEmpty(selection)) {
            return own;
        } else {
            return own + " and " + selection;
        }
    }

    //appends the selectionArgs to the own selection argument, if there are any selectionArgs
    private String[] appendSelectionArgs(String ownArg, String[] selectionArgs) {
        String[] args = new String[selectionArgs == null ? 1 : selectionArgs.length + 1];
        args[0] = ownArg;
        if (selectionArgs != null) {
            System.arraycopy(selectionArgs, 0, args, 1, selectionArgs.length);
        }
        return args;
    }
}
