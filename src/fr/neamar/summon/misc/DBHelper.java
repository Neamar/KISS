package fr.neamar.summon.misc;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper {

	private static SQLiteDatabase getDatabase(Context context)
	{
		DB db = new DB(context);
		return db.getReadableDatabase();
	}

	public static void insertHistory(Context context, String query, String record)
	{
		SQLiteDatabase db = getDatabase(context);
		
		ContentValues values = new ContentValues();
		values.put("query", query);
		values.put("record", record);
		db.insert("history", null, values);
		db.close();
	}
}
