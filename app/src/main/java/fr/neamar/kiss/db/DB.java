package fr.neamar.kiss.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DB extends SQLiteOpenHelper {

    private final static String DB_NAME = "kiss.s3db";
    private final static int DB_VERSION = 6;

    DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT, record TEXT NOT NULL)");
        database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
        createTags(database);
        addTimeStamps(database);
    }

    private void createTags(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE tags ( _id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL, record TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_tags_record ON tags(record);");
    }

    private void addTimeStamps(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE history ADD COLUMN timeStamp INTEGER DEFAULT 0  NOT NULL");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d("onUpgrade", "Updating database from version " + oldVersion + " to version " + newVersion);
        // See
        // http://www.drdobbs.com/database/using-sqlite-on-android/232900584
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                case 2:
                case 3:
                    database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                            + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
                    // fall through
                case 4:
                    createTags(database);
                    // fall through
                case 5:
                    addTimeStamps(database);
                    // fall through
                default:
                    break;
            }
        }
    }
}
