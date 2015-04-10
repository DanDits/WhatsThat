package dan.dit.whatsthat.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper that can be used to get a writable database object for the imageData database.
 * The database holds a table for the images {@ImageTable} and a table for already solved or
 * current riddles {@RiddleTable}.
 * Created by daniel on 24.03.15.
 */
public class ImageSQLiteHelper extends SQLiteOpenHelper{
    //Initial version number = 1
    private static final int DATABASE_VERSION=1;

    //Database name will not change
    private static final String DATABASE_NAME="imageData";

    /**
     * Creates an instance of the SQLiteOpenHelper required to use the imageData database.
     * @param context Context object.
     */
    protected ImageSQLiteHelper(Context context) {
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

