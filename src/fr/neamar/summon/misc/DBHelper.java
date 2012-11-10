package fr.neamar.summon.misc;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBHelper {

	private static SQLiteDatabase getDatabase(Context context) {
		DB db = new DB(context);
		return db.getReadableDatabase();
	}

	public static void insertHistory(Context context, String query,
			String record) {
		SQLiteDatabase db = getDatabase(context);

		ContentValues values = new ContentValues();
		values.put("query", query);
		values.put("record", record);
		db.insert("history", null, values);
		db.close();
	}

	public static ArrayList<String> getHistory(Context context, int limit) {
		SQLiteDatabase db = getDatabase(context);

		Cursor cursor = db.query("history", new String[] { "record" }, null, null,
				null, null, "_id DESC");

		ArrayList<String> records = new ArrayList<String>();
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast() && records.size() < limit) {
			records.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();
		db.close();
		
		return records;
	}
}
