package fr.neamar.kiss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import fr.neamar.kiss.pojo.ShortcutsPojo;

import java.util.ArrayList;

public class DBHelper {
    private static SQLiteDatabase getDatabase(Context context) {
        DB db = new DB(context);
        return db.getReadableDatabase();
    }

    private static ArrayList<ValuedHistoryRecord> readCursor(Cursor cursor) {
        ArrayList<ValuedHistoryRecord> records = new ArrayList<>();

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
     * @param context android context
     * @param query   query to insert
     * @param record  record to insert
     */
    public static void insertHistory(Context context, String query, String record) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("query", query);
        values.put("record", record);
        db.insert("history", null, values);
        db.close();
    }

    public static void removeFromHistory(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "record = ?", new String[]{record});
        db.close();
    }

    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getHistory(Context context, int limit) {
        ArrayList<ValuedHistoryRecord> records;

        SQLiteDatabase db = getDatabase(context);

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = db.query(true, "history", new String[]{"record", "1"}, null, null,
                null, null, "_id DESC", Integer.toString(limit));

        records = readCursor(cursor);
        cursor.close();
        db.close();
        return records;
    }


    /**
     * Retrieve history size
     *
     * @param context android context
     * @return total number of use for the application
     */
    public static int getHistoryLength(Context context) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = db.query(false, "history", new String[]{"COUNT(*)"}, null, null,
                null, null, null, null);

        cursor.moveToFirst();
        int historyLength = cursor.getInt(0);
        cursor.close();
        db.close();
        return historyLength;
    }

    /**
     * Retrieve previously selected items for the query
     *
     * @param context android context
     * @param query   query to run
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getPreviousResultsForQuery(Context context,
                                                                            String query) {
        ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
                "query LIKE ?", new String[]{query + "%"}, "record", null, "COUNT(*) DESC", "10");
        records = readCursor(cursor);
        cursor.close();
        db.close();
        return records;
    }

    public static void insertShortcut(Context context, ShortcutRecord shortcut) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("name", shortcut.name);
        values.put("package", shortcut.packageName);
        values.put("icon", shortcut.iconResource);
        values.put("intent_uri", shortcut.intentUri);
        values.put("icon_blob", shortcut.icon_blob);
        
        db.insert("shortcuts", null, values);
        db.close();
    }
    
    public static void removeShortcut(Context context, String name) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("shortcuts", "name = ?", new String[]{name});
        db.close();
    }

    
    public static ArrayList<ShortcutRecord> getShortcuts(Context context) {
        ArrayList<ShortcutRecord> records = new ArrayList<>();
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"name", "package", "icon", "intent_uri", "icon_blob"},
                null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ShortcutRecord entry = new ShortcutRecord();

            entry.name = cursor.getString(0);
            entry.packageName = cursor.getString(1);
            entry.iconResource = cursor.getString(2);
            entry.intentUri = cursor.getString(3);
            entry.icon_blob = cursor.getBlob(4);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();
        
        db.close();
        return records;
    }
    
    public static void removeShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"name", "package", "icon", "intent_uri", "icon_blob"},
                "intent_uri LIKE ?", new String[]{"%"+packageName+"%"}, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) { // remove from history
            db.delete("history", "record = ?", new String[]{ShortcutsPojo.SCHEME + cursor.getString(0).toLowerCase()});
            cursor.moveToNext();
        }
        cursor.close();
        
        //remove shortcuts
        db.delete("shortcuts", "intent_uri LIKE ?", new String[]{"%" + packageName + "%"});
        
        db.close();        
    }
    
}
