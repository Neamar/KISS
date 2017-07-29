package fr.neamar.kiss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.pojo.ShortcutsPojo;

public class DBHelper {
    private DBHelper() {
    }

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

    public static void clearHistory(Context context) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "", null);
        db.close();
    }

    private static Cursor getSmartHistoryCursor(SQLiteDatabase db, int limit) {
        //Since smart history sql uses a group by we don't use the whole history but a limit of recent apps
        int historyWindowSize =  limit *30;

        //order history based on frequency * recency
        //frequency = #launches_for_app / #all_launches
        //recency = 1 / position_of_app_in_normal_history
        String sql ="SELECT record, count(*) FROM " +
                " (" +
                "   SELECT * FROM history ORDER BY _id DESC " +
                "   LIMIT " + historyWindowSize +"" +
                " ) small_history " +
                " GROUP BY record " +
                " ORDER BY " +
                "   count(*) * 1.0 / (select count(*) from history LIMIT " + historyWindowSize +") / ((SELECT _id FROM history ORDER BY _id DESC LIMIT 1) - max(_id) + 0.001) " +
                " DESC " +
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryCursor(SQLiteDatabase db, int limit) {
        return db.query(true, "history", new String[]{"record", "1"}, null, null,
                null, null, "_id DESC", Integer.toString(limit));
    }
    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getHistory(Context context, int limit, boolean smartHistory) {
        ArrayList<ValuedHistoryRecord> records;

        SQLiteDatabase db = getDatabase(context);

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = (smartHistory)?getSmartHistoryCursor(db, limit):getHistoryCursor(db, limit);
        //db.query(true, "history", new String[]{"record", "1"}, null, null,
        //        null, null, "_id DESC", Integer.toString(limit));

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

    /**
     * Insert new tags for given id
     *
     * @param context android context
     * @param tag   tag to insert
     * @param record  record to insert
     */
    public static void insertTagsForId(Context context, String tag, String record) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("tag", tag);
        values.put("record", record);
        db.insert("tags", null, values);
        db.close();
    }


    /* Delete
     * Insert new item into history
     *
     * @param context android context
     * @param tag   query to insert
     * @param record  record to insert
     */
    public static void deleteTagsForId(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);

        db.delete("tags", "record = ?", new String[] {record});
        db.close();
    }

    public static Map<String, String> loadTags(Context context) {
        Map<String, String> records = new HashMap<>();
        SQLiteDatabase db = getDatabase(context);

        Cursor cursor = db.query("tags", new String[]{"record", "tag"}, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String id = cursor.getString(0);
            String tags = cursor.getString(1);
            records.put(id, tags);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        return records;

    }
}
