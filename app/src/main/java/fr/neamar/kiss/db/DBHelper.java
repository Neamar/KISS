package fr.neamar.kiss.db;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.neamar.kiss.utils.Log;

public class DBHelper {
    private static final String TAG = DBHelper.class.getSimpleName();
    // Match history entries whose query extends the current one, but also entries whose query is a prefix of it:
    // an item picked for "m" must still be boosted when typing "ma", otherwise results swap places as the user types.
    private static final String QUERY_FILTER = " (query LIKE ?1 || '%' OR (query != '' AND ?1 LIKE query || '%')) ";
    private static volatile SQLiteDatabase database = null;

    private DBHelper() {
    }

    private static SQLiteDatabase getDatabase(Context context) {
        if (database == null) {
            synchronized (DBHelper.class) {
                if (database == null) {
                    database = new DB(context).getReadableDatabase();
                }
            }
        }
        return database;
    }

    private static List<ValuedHistoryRecord> readCursor(@NonNull Cursor cursor, boolean withRelevance) {
        cursor.moveToFirst();

        int count = cursor.getCount();
        List<ValuedHistoryRecord> records = new ArrayList<>(count);
        while (!cursor.isAfterLast()) {
            ValuedHistoryRecord entry = new ValuedHistoryRecord();

            entry.record = cursor.getString(0);
            entry.value = cursor.getInt(1);
            entry.relevance = withRelevance ? count - cursor.getPosition() : 0;

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
    public static void insertHistory(@NonNull Context context, @Nullable String query, @NonNull String record) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("query", query);
        values.put("record", record);
        values.put("timeStamp", System.currentTimeMillis());
        db.insert("history", null, values);

        if (Math.random() <= 0.005) {
            // Roughly every 200 inserts, clean up the history of items older than 3 months
            long twoMonthsAgo = 7776000000L; // 1000 * 60 * 60 * 24 * 30 * 3;
            db.delete("history", "timeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() - twoMonthsAgo)});
            // And vacuum the DB for speed
            db.execSQL("VACUUM");
        }
    }

    public static void removeFromHistory(@NonNull Context context, @NonNull String record) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "record = ?", new String[]{record});
    }

    public static void clearHistory(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "", null);
    }

    private static Cursor getHistoryByFrecency(@NonNull SQLiteDatabase db, int limit, @Nullable String query) {
        // Since smart history sql uses a group by we don't use the whole history but a limit of recent apps
        int historyWindowSize = limit * 30;

        if (query == null) {
            // order history based on frequency * recency
            // frequency = #launches_for_app / #all_launches
            // recency = 1 / position_of_app_in_normal_history
            String sql = "SELECT record, count(*) FROM " +
                    " (" +
                    "   SELECT * FROM history ORDER BY _id DESC " +
                    "   LIMIT " + historyWindowSize +
                    " ) small_history " +
                    " GROUP BY record " +
                    " ORDER BY " +
                    "   count(*) * 1.0 / (select count(*) from history LIMIT " + historyWindowSize + ") / ((SELECT _id FROM history ORDER BY _id DESC LIMIT 1) - max(_id) + 0.001) " +
                    " DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, null);
        } else {
            // order history based on frequency * recency
            // frequency = #launches_for_app / #all_launches
            // recency = 1 / position_of_app_in_normal_history
            String sql = "SELECT record, count(*) FROM " +
                    " (" +
                    "   SELECT * FROM history ORDER BY _id DESC " +
                    "   LIMIT " + historyWindowSize +
                    " ) small_history " +
                    " WHERE " + QUERY_FILTER +
                    " GROUP BY record " +
                    " ORDER BY " +
                    "   count(*) * 1.0 / (select count(*) from history LIMIT " + historyWindowSize + ") / ((SELECT _id FROM history ORDER BY _id DESC LIMIT 1) - max(_id) + 0.001) " +
                    " DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, new String[]{query});
        }
    }

    private static Cursor getHistoryByFrequency(@NonNull SQLiteDatabase db, int limit, @Nullable String query) {
        if (query == null) {
            // order history based on frequency
            String sql = "SELECT record, count(*) AS value FROM history" +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, null);
        } else {
            // order history based on frequency
            String sql = "SELECT record, count(*) AS value FROM history" +
                    " WHERE " + QUERY_FILTER +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, new String[]{query});
        }
    }

    private static Cursor getHistoryByRecency(@NonNull SQLiteDatabase db, int limit, @Nullable String query) {
        if (query == null) {
            return db.query(true, "history", new String[]{"record", "MAX(_id) AS value"}, null, null,
                    "record", null, "value DESC", Integer.toString(limit));
        } else {
            return db.query(true, "history", new String[]{"record", "MAX(_id) AS value"}, QUERY_FILTER, new String[]{query},
                    "record", null, "value DESC", Integer.toString(limit));
        }
    }

    /**
     * Get the most used history items adaptively based on a set period of time
     *
     * @param db    The SQL db
     * @param limit Maximum result size
     * @return Cursor
     */
    private static Cursor getHistoryByAdaptive(@NonNull SQLiteDatabase db, int limit, @Nullable String query) {
        // how many hours back we want to test frequency against
        int hours = 36;
        if (query == null) {
            // order history based on frequency
            String sql = "SELECT record, count(*) AS value FROM history " +
                    " WHERE timeStamp >= 0 " +
                    " AND timeStamp >" + (System.currentTimeMillis() - (hours * 3600000L)) +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, null);
        } else {
            // order history based on frequency
            String sql = "SELECT record, count(*) AS value FROM history " +
                    " WHERE timeStamp >= 0 " +
                    " AND timeStamp >" + (System.currentTimeMillis() - (hours * 3600000L)) +
                    " AND " + QUERY_FILTER +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, new String[]{query});
        }
    }

    /**
     * Get the history items used closest to this time of day, for each day old an item is it has
     * one less hour of time weight. So we limit the number of days of history to 24 days in the
     * WHERE clause, this should also help with speed on large histories.
     * <p>
     * This is done by taking the max of a triangle waveform whose period is 24 hours, amplitude
     * is half the milliseconds in a day and begins at currentTimeMillis() - timestamp, then offset
     * by the time difference / 48 to diminish older history items by an hour for every day old. 48
     * is used because the triangle wave is half amplitude (1 / 2) * (1 / 24) = 1 / 48.
     *
     * @param db    The SQL db
     * @param limit Maximum result size
     * @return Cursor
     */
    private static Cursor getHistoryByTime(@NonNull SQLiteDatabase db, int limit, @Nullable String query) {
        final long now = System.currentTimeMillis();
        final long MS_24_DAYS_AGO = now - 2073600000L;
        if (query == null) {
            String sql = "SELECT record, MAX(ABS((" + now + " - timestamp) % 86400000 - 43200000) - (" + now + " - timestamp) / 48 ) AS value" +
                    " FROM history" +
                    " WHERE timestamp > " + MS_24_DAYS_AGO +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, null);
        } else {
            String sql = "SELECT record, MAX(ABS((" + now + " - timestamp) % 86400000 - 43200000) - (" + now + " - timestamp) / 48 ) AS value" +
                    " FROM history" +
                    " WHERE timestamp > " + MS_24_DAYS_AGO +
                    " AND " + QUERY_FILTER +
                    " GROUP BY record " +
                    " ORDER BY value DESC " +
                    " LIMIT " + limit;
            return db.rawQuery(sql, new String[]{query});
        }
    }

    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @return records with number of use
     */
    public static List<ValuedHistoryRecord> getHistory(@NonNull Context context, int limit, HistoryMode historyMode) {
        return getHistory(context, limit, historyMode, null);
    }

    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @param query   search query
     * @return records with number of use
     */
    public static List<ValuedHistoryRecord> getHistory(@NonNull Context context, int limit, HistoryMode historyMode, @Nullable String query) {
        List<ValuedHistoryRecord> records;

        SQLiteDatabase db = getDatabase(context);

        Cursor cursor;
        switch (historyMode) {
            case FRECENCY:
                cursor = getHistoryByFrecency(db, limit, query);
                break;
            case FREQUENCY:
                cursor = getHistoryByFrequency(db, limit, query);
                break;
            case ADAPTIVE:
                cursor = getHistoryByAdaptive(db, limit, query);
                break;
            case TIME:
                cursor = getHistoryByTime(db, limit, query);
                break;
            case ALPHABETICALLY:
            case RECENCY:
                cursor = getHistoryByRecency(db, limit, query);
                break;
            default:
                cursor = getHistoryByRecency(db, limit, query);
                Log.e(TAG, "Fallback to 'recency' for unknown history mode " + historyMode);
                break;
        }

        records = readCursor(cursor, historyMode != HistoryMode.ALPHABETICALLY);
        cursor.close();

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
        try (Cursor cursor = db.query(false, "history", new String[]{"COUNT(*)"}, null, null,
                null, null, null, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    /**
     * Insert or update a shortcut into DB.
     *
     * @param context
     * @param shortcut
     * @return true, if shortcut has changed
     */
    public static boolean insertShortcut(Context context, ShortcutRecord shortcut) {
        SQLiteDatabase db = getDatabase(context);
        // check if any field has changed
        try (Cursor cursor = db.query("shortcuts", new String[]{"name", "package", "intent_uri"},
                "name = ? and package = ? AND intent_uri = ?", new String[]{shortcut.name, shortcut.packageName, shortcut.intentUri}, null, null, null, null)) {
            if (cursor.getCount() > 0) {
                return false;
            }
        }

        ContentValues values = new ContentValues();
        values.put("name", shortcut.name);
        values.put("package", shortcut.packageName);
        values.put("icon", (String) null); // Legacy field (for shortcuts before Oreo), not used anymore
        values.put("icon_blob", (String) null); // Another legacy field (icon is dynamically retrieved)
        values.put("intent_uri", shortcut.intentUri);

        // do not add duplicate shortcuts
        int rowsAffected = db.update("shortcuts", values, "package = ? AND intent_uri = ?", new String[]{shortcut.packageName, shortcut.intentUri});
        if (rowsAffected == 0) {
            db.insert("shortcuts", null, values);
        }
        return true;
    }

    /**
     * Remove a shortcut from DB.
     *
     * @param context
     * @param packageName
     * @param intentUri
     * @return true, if shortcut was removed
     */
    public static boolean removeShortcut(Context context, String packageName, String intentUri) {
        SQLiteDatabase db = getDatabase(context);
        int rowsAffected = db.delete("shortcuts", "package = ? AND intent_uri = ?", new String[]{packageName, intentUri});
        return rowsAffected > 0;
    }

    public static void addCustomAppName(Context context, String componentName, String newName) {
        ContentValues values = new ContentValues();
        values.put("component_name", componentName);
        values.put("name", newName);
        // flag read by older versions of KISS, should the user downgrade
        values.put("custom_flags", 2);
        getDatabase(context).insertWithOnConflict("custom_apps", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void removeCustomAppName(Context context, String componentName) {
        getDatabase(context).delete("custom_apps", "component_name = ?", new String[]{componentName});
    }

    public static void moveCustomApp(Context context, String oldComponentName, String newComponentName) {
        ContentValues values = new ContentValues();
        values.put("component_name", newComponentName);
        getDatabase(context).update("custom_apps", values, "component_name = ?", new String[]{oldComponentName});
    }

    /**
     * @return custom name by component name
     */
    public static Map<String, String> getCustomAppNames(Context context) {
        Map<String, String> names = new HashMap<>();
        try (Cursor cursor = getDatabase(context).query("custom_apps", new String[]{"component_name", "name"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                names.put(cursor.getString(0), cursor.getString(1));
            }
        }
        return names;
    }

    /**
     * Retrieve a list of all shortcuts for current package name, without icons.
     */
    public static List<ShortcutRecord> getShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"_id", "name", "package", "intent_uri"},
                "package = ?", new String[]{packageName}, null, null, null);
        cursor.moveToFirst();

        List<ShortcutRecord> records = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            ShortcutRecord entry = new ShortcutRecord();

            entry.dbId = cursor.getInt(0);
            entry.name = cursor.getString(1);
            entry.packageName = cursor.getString(2);
            entry.intentUri = cursor.getString(3);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Retrieve a list of all shortcuts, without icons.
     */
    public static List<ShortcutRecord> getShortcuts(Context context) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"_id", "name", "package", "intent_uri"},
                null, null, null, null, null);
        cursor.moveToFirst();

        List<ShortcutRecord> records = new ArrayList<>(cursor.getCount());
        while (!cursor.isAfterLast()) {
            ShortcutRecord entry = new ShortcutRecord();

            entry.dbId = cursor.getInt(0);
            entry.name = cursor.getString(1);
            entry.packageName = cursor.getString(2);
            entry.intentUri = cursor.getString(3);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Remove shortcuts for a given package name
     */
    public static void removeShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // remove shortcuts
        db.delete("shortcuts", "package LIKE ?", new String[]{"%" + packageName + "%"});
    }

    public static void removeAllShortcuts(Context context) {
        SQLiteDatabase db = getDatabase(context);
        // delete whole table
        db.delete("shortcuts", null, null);
    }

    /**
     * Insert new tags for given id
     *
     * @param context android context
     * @param tag     tag to insert
     * @param record  record to insert
     */
    public static void insertTagsForId(Context context, String tag, String record) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("tag", tag);
        values.put("record", record);
        db.insert("tags", null, values);
    }


    /**
     * Delete
     *
     * @param context android context
     * @param record  record to delete
     */
    public static void deleteTagsForId(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);

        db.delete("tags", "record = ?", new String[]{record});
    }

    public static void moveTags(Context context, String oldRecord, String newRecord) {
        ContentValues values = new ContentValues();
        values.put("record", newRecord);
        getDatabase(context).update("tags", values, "record = ?", new String[]{oldRecord});
    }

    /**
     * Delete all tags
     *
     * @param context android context
     */
    public static void deleteTags(Context context) {
        SQLiteDatabase db = getDatabase(context);

        db.execSQL("DELETE FROM tags;");
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
        return records;
    }

    public static void initDatabase(Context context) {
        getDatabase(context);
    }

    public static Map<String, ComponentName> getCustomComponents(@NonNull Context context) {
        Map<String, ComponentName> components = new HashMap<>();
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("custom_components", new String[]{"id", "package", "class"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String pck = cursor.getString(1);
                String cls = cursor.getString(2);
                ComponentName componentName = new ComponentName(pck, cls);
                components.put(id, componentName);
            }
        }

        return components;
    }

    public static void setCustomComponent(@NonNull Context context, @NonNull String id, @Nullable ComponentName componentName) {
        SQLiteDatabase db = getDatabase(context);

        if (componentName == null) {
            db.delete("custom_components", "id = ?", new String[]{id});
        } else {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("package", componentName.getPackageName());
            values.put("class", componentName.getClassName());

            // do not add duplicate shortcuts
            int rowsAffected = db.update("custom_components", values, "id = ?", new String[]{id});
            if (rowsAffected == 0) {
                db.insert("custom_components", null, values);
            }
        }
    }

    public static void removeAllCustomComponents(Context context) {
        SQLiteDatabase db = getDatabase(context);
        // delete whole table
        db.delete("custom_components", null, null);

    }
}
