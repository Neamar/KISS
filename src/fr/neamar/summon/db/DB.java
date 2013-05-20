package fr.neamar.summon.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

	final static int DB_VERSION = 2;
	final static String DB_NAME = "summon.s3db";
	Context context;

	public DB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

   	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT NOT NULL, record TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, package TEXT NOT NULL, activity TEXT NOT NULL, name TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS contacts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, lookup_key TEXT NOT NULL, phone TEXT NOT NULL, " +
                "mail TEXT NOT NULL, icon TEXT NOT NULL, primary_number BOOLEAN NOT NULL, times_contacted INTEGER NOT NULL, starred BOOLEAN NOT NULL, " +
                "home_number BOOLEAN NOT NULL, name TEXT);");
	}

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //context.deleteDatabase(DB_NAME);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT NOT NULL, record TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, package TEXT NOT NULL, activity TEXT NOT NULL, name TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS contacts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, lookup_key TEXT NOT NULL, phone TEXT NOT NULL, " +
                "mail TEXT NOT NULL, icon TEXT NOT NULL, primary_number BOOLEAN NOT NULL, times_contacted INTEGER NOT NULL, starred BOOLEAN NOT NULL, " +
                "home_number BOOLEAN NOT NULL, name TEXT);");
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //context.deleteDatabase(DB_NAME);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT NOT NULL, record TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, package TEXT NOT NULL, activity TEXT NOT NULL, name TEXT NOT NULL);");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS contacts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, lookup_key TEXT NOT NULL, phone TEXT NOT NULL, " +
                "mail TEXT NOT NULL, icon TEXT NOT NULL, primary_number BOOLEAN NOT NULL, times_contacted INTEGER NOT NULL, starred BOOLEAN NOT NULL, " +
                "home_number BOOLEAN NOT NULL, name TEXT);");
    }
}