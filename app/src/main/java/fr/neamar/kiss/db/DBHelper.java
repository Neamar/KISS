package fr.neamar.kiss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class DBHelper {
    private static SQLiteDatabase getDatabase(Context context) {
        DB db = new DB(context);
        return db.getReadableDatabase();
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
     * @param context
     * @param limit
     * @return
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
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
                "query LIKE ?", new String[]{query + "%"}, "record", null, "COUNT(*) DESC", "10");
        records = readCursor(cursor);
        db.close();
        return records;
    }

    /**
     * Retrieve most used records. Warning : filter through applications only
     *
     * @param context
     * @param limit   number of item to return
     * @return
     */
    public static ArrayList<ValuedHistoryRecord> getFavorites(Context context, int limit) {
        ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
                null, null, "record", null, "COUNT(*) DESC", Integer.toString(limit));

        records = readCursor(cursor);
        db.close();
        return records;
    }
}
