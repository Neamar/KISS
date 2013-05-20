package fr.neamar.summon.db;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;

import java.util.ArrayList;

import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.ContactHolder;

public class DBHelper {

	public static final Object sDataLock = new Object();

	private static SQLiteDatabase getReadDatabase(Context context) {
		DB db = new DB(context);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            db.setWriteAheadLoggingEnabled(true);
        }
		return db.getReadableDatabase();
	}

    private static SQLiteDatabase getWriteDatabase(Context context) {
        DB db = new DB(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            db.setWriteAheadLoggingEnabled(true);
        }
        return db.getWritableDatabase();
    }

	private static ArrayList<ValuedHistoryRecord> readCursor(Cursor cursor) {
		ArrayList<ValuedHistoryRecord> records = new ArrayList<ValuedHistoryRecord>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			ValuedHistoryRecord entry = new ValuedHistoryRecord();

			entry.record = cursor.getString(0);
			entry.value = cursor.getInt(1);

			records.add(entry);
			cursor.moveToNext();
		}
		cursor.close();

		return records;
	}

	/**
	 * Insert new item into history
	 * 
	 * @param context
	 * @param query
	 * @param record
	 */
	public static void insertHistory(Context context, String query, String record) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getWriteDatabase(context);

			ContentValues values = new ContentValues();
			values.put("query", query);
			values.put("record", record);
			db.insert("history", null, values);
			db.close();
		}
		new BackupManager(context).dataChanged();
	}

	public static void removeFromHistory(Context context, String record) {
		synchronized (DBHelper.sDataLock) {
			SQLiteDatabase db = getWriteDatabase(context);
			db.delete("history", "record = ?", new String[] { record });
			db.close();
		}
		new BackupManager(context).dataChanged();
	}

	/**
	 * Retrieve previous query history
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public static ArrayList<ValuedHistoryRecord> getHistory(Context context, int limit) {
		ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getReadDatabase(context);

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = db.query(true, "history", new String[] { "record", "1" }, null, null,
                null, null, "_id DESC", Integer.toString(limit));

        records = readCursor(cursor);
        db.close();
		return records;
	}

	/**
	 * Retrieve previously selected items for the query
	 * 
	 * @param context
	 * @param query
	 * @return
	 */
	public static ArrayList<ValuedHistoryRecord> getPreviousResultsForQuery(Context context,
			String query) {
		ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getReadDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[] { "record", "COUNT(*) AS count" },
                "query = ?", new String[] { query }, "record", null, "COUNT(*) DESC", "5");
        records = readCursor(cursor);
        db.close();
		return records;
	}

	/**
	 * Retrieve most used records. Warning : filter through applications only
	 * 
	 * @param context
	 * @param limit
	 * @return
	 */
	public static ArrayList<ValuedHistoryRecord> getFavorites(Context context, int limit) {
		ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getReadDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[] { "record", "COUNT(*) AS count" },
                null, null, "record", null, "COUNT(*) DESC", Integer.toString(limit));

        records = readCursor(cursor);
        db.close();
		return records;
	}

    public static void clearHolders(Context context){
        synchronized (DBHelper.sDataLock) {
            SQLiteDatabase db = getWriteDatabase(context);
            db.delete("apps", null, null);
            db.delete("contacts", null, null);
            db.close();
        }
    }

    public static void saveAppHolders(Context context, ArrayList<AppHolder> apps) {
        synchronized (DBHelper.sDataLock) {
            SQLiteDatabase db = getWriteDatabase(context);
            for (AppHolder app : apps){
                ContentValues values = new ContentValues();
                values.put("name", app.name);
                values.put("package", app.packageName);
                values.put("activity", app.activityName);
                db.insert("apps", null, values);
            }
            db.close();
        }
    }

    public static ArrayList<AppHolder> getAppHolders(Context context, String holderScheme){
        ArrayList<AppHolder> apps = new ArrayList<AppHolder>();
        SQLiteDatabase db = getReadDatabase(context);

        Cursor cursor = db.query("apps", null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            AppHolder app = new AppHolder();
            app.packageName = cursor.getString(1);
            app.activityName = cursor.getString(2);
            app.id = holderScheme + app.packageName + "/"
                    + app.activityName;
            app.name = cursor.getString(3);

            //Ugly hack to remove accented characters.
            //Note Java 5 provides a Normalizer method, unavailable for Android :\
            app.nameLowerCased = app.name.toLowerCase().replaceAll("[èéêë]", "e")
                    .replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
                    .replaceAll("[àâ]", "a").replaceAll("ô", "o").replaceAll("[ÈÉÊË]", "E");
            apps.add(app);
            cursor.moveToNext();
        }
        db.close();
        return apps;
    }

    public static void saveContactHolders(Context context, ArrayList<ContactHolder> contacts) {
        synchronized (DBHelper.sDataLock) {
            SQLiteDatabase db = getWriteDatabase(context);
            for (ContactHolder contact : contacts) {
                ContentValues values = new ContentValues();
                values.put("name", contact.name);
                values.put("lookup_key", contact.lookupKey);
                values.put("phone", contact.phone);
                values.put("mail", contact.mail);
                if(contact.icon != null){
                    values.put("icon", contact.icon.getPath());
                }else {
                    values.putNull("icon");
                }
                values.put("primary_number", contact.primary);
                values.put("times_contacted", contact.timesContacted);
                values.put("starred", contact.starred);
                values.put("home_number", contact.homeNumber);
                values.put("name", contact.name);
                db.insert("contacts", null, values);
            }
            db.close();
        }
    }

    public static ArrayList<ContactHolder> getContactsHolders(Context context, String holderScheme) {
        ArrayList<ContactHolder> contacts = new ArrayList<ContactHolder>();
        SQLiteDatabase db = getReadDatabase(context);

        Cursor cursor = db.query("contacts", null, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ContactHolder contact = new ContactHolder();
            contact.lookupKey = cursor.getString(1);
            contact.phone = cursor.getString(2);
            contact.mail = cursor.getString(3);
            if(!cursor.isNull(4)){
                contact.icon = new Uri.Builder().appendPath(cursor.getString(3)).build();
            }else {
                contact.icon = null;
            }
            contact.primary = cursor.getInt(5) == 1;
            contact.timesContacted = cursor.getInt(6);
            contact.starred = cursor.getInt(7) == 1;
            contact.homeNumber = cursor.getInt(8) == 1;
            if(!cursor.isNull(9)){
                contact.name = cursor.getString(9);
            }
            contact.id = holderScheme + contact.lookupKey + contact.phone;
            if (contact.name != null) {
                contact.nameLowerCased = contact.name.toLowerCase().replaceAll("[èéêë]", "e")
                        .replaceAll("[ûù]", "u").replaceAll("[ïî]", "i")
                        .replaceAll("[àâ]", "a").replaceAll("ô", "o").replaceAll("[ÈÉÊË]", "E");
            }
            contacts.add(contact);
            cursor.moveToNext();
        }
        db.close();
        return contacts;
    }


}
